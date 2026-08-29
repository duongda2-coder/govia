package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup1Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskScoreRankRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQuantitativeRowResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQuantitativeValue;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaQuantitativeValueRepository;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Ket qua cham diem rui ro dinh luong" theo chi nhanh/nam (sheet CT_Diem_DL, tinh dong tu du lieu
 * da nhap o "Ho so rui ro dinh luong" - xem RiskCriteriaQuantitativeValueService). Man hinh CHI XEM,
 * khong co them/sua/xoa truc tiep (giong RiskAssessmentOtherRankingService).
 *
 * <p>Voi moi chi tieu (RiskCriteriaQuantitative) co gia tri HSRR (N) cho 1 chi nhanh/nam:
 * <ol>
 *   <li>Diem quy doi: tra N vao 5 nguong score20/40/60/80/100 cua chinh chi tieu do, theo
 *   {@code criteriaType} (1 = thuan/tang dan, 2 = nghich/giam dan, 3 = bang chinh xac) - xem
 *   {@link #convertScore}.</li>
 *   <li>Dong gop = diem quy doi x {@code criteria.weight} (ti trong chi tieu, cot TS_NV) x
 *   {@code group1.weight} (ti trong nhom, qua {@code criteria.group1Id}).</li>
 * </ol>
 * Tong diem = tong dong gop moi chi tieu. Xep hang = tra RiskScoreRank theo Tong diem + nam (cung
 * pattern voi RiskAssessmentOtherRankingService.resolveRankLabel).
 */
@Service
public class RiskBranchScoreQuantitativeService {

    private final RiskCriteriaQuantitativeValueRepository valueRepository;
    private final RiskCriteriaQuantitativeRepository criteriaRepository;
    private final RiskGroup1Repository group1Repository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final RiskScoreRankRepository rankRepository;

    public RiskBranchScoreQuantitativeService(RiskCriteriaQuantitativeValueRepository valueRepository,
                                               RiskCriteriaQuantitativeRepository criteriaRepository,
                                               RiskGroup1Repository group1Repository,
                                               AuditObjectUnitRepository auditObjectUnitRepository,
                                               RiskScoreRankRepository rankRepository) {
        this.valueRepository = valueRepository;
        this.criteriaRepository = criteriaRepository;
        this.group1Repository = group1Repository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.rankRepository = rankRepository;
    }

    @Transactional(readOnly = true)
    public List<RiskBranchScoreQuantitativeRowResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskCriteriaQuantitative> criteria = criteriaById(tenantId);
        Map<UUID, RiskGroup1> groups = group1ById(tenantId);
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);
        List<RiskScoreRank> ranks = rankRepository.findByTenantIdOrderByFromYearAscScoreFromAsc(tenantId);

        Map<String, List<RiskCriteriaQuantitativeValue>> byBranch = new LinkedHashMap<>();
        for (RiskCriteriaQuantitativeValue v : valueRepository.findByTenantIdAndYearOrderByBranchCodeAsc(tenantId, year)) {
            byBranch.computeIfAbsent(v.getBranchCode(), k -> new ArrayList<>()).add(v);
        }

        List<RiskBranchScoreQuantitativeRowResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<RiskCriteriaQuantitativeValue>> entry : byBranch.entrySet()) {
            String branchCode = entry.getKey();
            Map<String, BigDecimal> scores = new HashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (RiskCriteriaQuantitativeValue v : entry.getValue()) {
                RiskCriteriaQuantitative criterion = criteria.get(v.getCriteriaId());
                if (criterion == null || v.getValue() == null || criterion.getWeight() == null) {
                    continue;
                }
                BigDecimal converted = convertScore(criterion, v.getValue());
                if (converted == null) {
                    continue;
                }
                RiskGroup1 group = groups.get(criterion.getGroup1Id());
                BigDecimal groupWeight = group != null && group.getWeight() != null ? group.getWeight() : BigDecimal.ZERO;
                BigDecimal contribution = converted.multiply(criterion.getWeight()).multiply(groupWeight);
                scores.put(criterion.getCode(), contribution);
                total = total.add(contribution);
            }
            AuditObjectUnit unit = units.get(branchCode);
            total = total.setScale(2, RoundingMode.HALF_UP);
            String rankLabel = resolveRankLabel(ranks, total, year);
            result.add(new RiskBranchScoreQuantitativeRowResponse(branchCode, unit != null ? unit.getName() : null,
                    year, total, rankLabel, scores));
        }
        result.sort((a, b) -> b.totalScore().compareTo(a.totalScore()));
        return result;
    }

    /** Diem quy doi theo criteriaType - xem Javadoc lop nay cho cong thuc day du (vi du bang so
     * trich tu tai lieu goc sheet CT_Diem_DL, muc "9 (cach tinh diem cho tung chi tieu)"). */
    private BigDecimal convertScore(RiskCriteriaQuantitative c, BigDecimal n) {
        Integer type = c.getCriteriaType();
        if (type == null) {
            return null;
        }
        return switch (type) {
            case 1 -> thresholdMatch(n, c, true);
            case 2 -> thresholdMatch(n, c, false);
            case 3 -> exactMatch(n, c);
            default -> null;
        };
    }

    /** ascending=true (loai 1, "thuan"): lay bac cao nhat ma N >= nguong.
     * ascending=false (loai 2, "nghich"): lay bac cao nhat ma N < nguong. */
    private BigDecimal thresholdMatch(BigDecimal n, RiskCriteriaQuantitative c, boolean ascending) {
        BigDecimal[] scores = { BigDecimal.valueOf(100), BigDecimal.valueOf(80), BigDecimal.valueOf(60), BigDecimal.valueOf(40), BigDecimal.valueOf(20) };
        BigDecimal[] thresholds = { c.getScore100(), c.getScore80(), c.getScore60(), c.getScore40(), c.getScore20() };
        for (int i = 0; i < scores.length; i++) {
            if (thresholds[i] == null) {
                continue;
            }
            boolean matches = ascending ? n.compareTo(thresholds[i]) >= 0 : n.compareTo(thresholds[i]) < 0;
            if (matches) {
                return scores[i];
            }
        }
        return BigDecimal.ZERO;
    }

    /** loai 3 ("bang"): N phai khop chinh xac 1 trong 5 nguong. */
    private BigDecimal exactMatch(BigDecimal n, RiskCriteriaQuantitative c) {
        BigDecimal[] scores = { BigDecimal.valueOf(100), BigDecimal.valueOf(80), BigDecimal.valueOf(60), BigDecimal.valueOf(40), BigDecimal.valueOf(20) };
        BigDecimal[] thresholds = { c.getScore100(), c.getScore80(), c.getScore60(), c.getScore40(), c.getScore20() };
        for (int i = 0; i < scores.length; i++) {
            if (thresholds[i] != null && n.compareTo(thresholds[i]) == 0) {
                return scores[i];
            }
        }
        return BigDecimal.ZERO;
    }

    /** Xet khoang diem [scoreFrom, scoreTo] con hieu luc trong nam (fromYear..toYear) - lay dong dau tien khop. */
    private String resolveRankLabel(List<RiskScoreRank> ranks, BigDecimal score, Integer year) {
        return ranks.stream()
                .filter(RiskScoreRank::isActive)
                .filter(r -> year >= r.getFromYear() && year <= r.getToYear())
                .filter(r -> score.compareTo(r.getScoreFrom()) >= 0 && score.compareTo(r.getScoreTo()) <= 0)
                .map(RiskScoreRank::getRankLabel)
                .findFirst()
                .orElse(null);
    }

    private Map<UUID, RiskCriteriaQuantitative> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaQuantitative> map = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> map.put(c.getId(), c));
        return map;
    }

    private Map<UUID, RiskGroup1> group1ById(UUID tenantId) {
        Map<UUID, RiskGroup1> map = new HashMap<>();
        group1Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }
}
