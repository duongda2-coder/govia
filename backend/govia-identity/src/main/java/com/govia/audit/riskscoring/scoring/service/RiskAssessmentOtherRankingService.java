package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskScoreRankRepository;
import com.govia.audit.riskscoring.masterdata.service.AuditObjectResolverService;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherRankingResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherHeader;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherLine;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOther;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOtherScale;
import com.govia.audit.riskscoring.scoring.entity.RiskTypeHO;
import com.govia.audit.riskscoring.scoring.repository.RiskAssessmentOtherHeaderRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskAssessmentOtherLineRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherScaleRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskTypeHORepository;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Bang xep hang cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_BXHRR_KHAC) -
 * man hinh CHI XEM (khong cho them/sua/xoa truc tiep), tinh dong tu du lieu da cham o ZTC_CDRR_KHAC
 * (xem RiskAssessmentOtherHeader/Line) theo 1 nam do NSD chon:
 * - Diem rui ro: neu doi tuong la 1 don vi co unitType=HO (Hoi so that su, khac voi GSCC/Chi nhanh
 *   cung category HO) thi Cong thuc 1 = Tong(diem chi tieu * ti trong chi tieu * ti trong nhom rui
 *   ro HO ung voi chi tieu do); nguoc lai (moi category khac, hoac GSCC/CN) dung Cong thuc 2 =
 *   Tong(diem chi tieu * ti trong chi tieu) - khong nhan ti trong nhom.
 * - Xep loai: doi chieu Diem rui ro voi khoang [scoreFrom, scoreTo] con hieu luc trong nam do cua
 *   danh muc Thang diem xep loai rui ro (xem RiskScoreRank/ScoreRankService, sheet "QL thang diem",
 *   tcode ztc_rank).
 * - Chi hien thi doi tuong kiem toan da co it nhat 1 chi tieu duoc cham diem (line.scaleId != null)
 *   trong nam do - header vua tao (chua cham diem chi tieu nao) khong duoc liet ke.
 */
@Service
public class RiskAssessmentOtherRankingService {

    private final RiskAssessmentOtherHeaderRepository headerRepository;
    private final RiskAssessmentOtherLineRepository lineRepository;
    private final RiskCriteriaOtherRepository criteriaOtherRepository;
    private final RiskCriteriaOtherScaleRepository scaleRepository;
    private final RiskTypeHORepository riskTypeHoRepository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditObjectResolverService objectResolver;
    private final RiskScoreRankRepository rankRepository;

    public RiskAssessmentOtherRankingService(RiskAssessmentOtherHeaderRepository headerRepository,
                                              RiskAssessmentOtherLineRepository lineRepository,
                                              RiskCriteriaOtherRepository criteriaOtherRepository,
                                              RiskCriteriaOtherScaleRepository scaleRepository,
                                              RiskTypeHORepository riskTypeHoRepository,
                                              AuditObjectCategoryRepository auditObjectCategoryRepository,
                                              AuditObjectUnitRepository auditObjectUnitRepository,
                                              AuditObjectResolverService objectResolver,
                                              RiskScoreRankRepository rankRepository) {
        this.headerRepository = headerRepository;
        this.lineRepository = lineRepository;
        this.criteriaOtherRepository = criteriaOtherRepository;
        this.scaleRepository = scaleRepository;
        this.riskTypeHoRepository = riskTypeHoRepository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.objectResolver = objectResolver;
        this.rankRepository = rankRepository;
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentOtherRankingResponse> listByYear(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        Map<UUID, RiskCriteriaOther> criteria = criteriaById(tenantId);
        Map<UUID, RiskCriteriaOtherScale> scales = scalesById(tenantId);
        Map<UUID, RiskTypeHO> riskTypes = riskTypesById(tenantId);
        List<RiskScoreRank> ranks = rankRepository.findByTenantIdOrderByFromYearAscScoreFromAsc(tenantId);

        List<RiskAssessmentOtherRankingResponse> result = new ArrayList<>();
        for (RiskAssessmentOtherHeader header : headerRepository.findByTenantIdOrderByYearDescAuditObjectCodeAsc(tenantId)) {
            if (!header.getYear().equals(year)) {
                continue;
            }
            List<RiskAssessmentOtherLine> lines = lineRepository.findByHeaderIdOrderByCriteriaOtherIdAsc(header.getId());
            boolean hasAnyScore = lines.stream().anyMatch(l -> l.getScaleId() != null);
            if (!hasAnyScore) {
                continue;
            }

            AuditObjectCategory category = categories.get(header.getAuditObjectCategoryId());
            String categoryCode = category != null ? category.getCode() : null;
            String objectName = objectResolver.resolveName(tenantId, category, header.getAuditObjectCode());

            BigDecimal score = computeScore(tenantId, header, lines, categoryCode, criteria, scales, riskTypes);
            String rankLabel = resolveRankLabel(ranks, score, year);

            result.add(new RiskAssessmentOtherRankingResponse(header.getId(), header.getYear(),
                    categoryCode, category != null ? category.getName() : null,
                    header.getAuditObjectCode(), objectName, score, rankLabel));
        }
        result.sort((a, b) -> b.riskScore().compareTo(a.riskScore()));
        return result;
    }

    private BigDecimal computeScore(UUID tenantId, RiskAssessmentOtherHeader header, List<RiskAssessmentOtherLine> lines, String categoryCode,
                                     Map<UUID, RiskCriteriaOther> criteria, Map<UUID, RiskCriteriaOtherScale> scales,
                                     Map<UUID, RiskTypeHO> riskTypes) {
        boolean useGroupWeight = "HO".equals(categoryCode)
                && auditObjectUnitRepository.findByTenantIdAndCode(tenantId, header.getAuditObjectCode())
                        .map(u -> "HO".equals(u.getUnitType()))
                        .orElse(false);

        BigDecimal total = BigDecimal.ZERO;
        for (RiskAssessmentOtherLine line : lines) {
            if (line.getScaleId() == null) {
                continue;
            }
            RiskCriteriaOtherScale scale = scales.get(line.getScaleId());
            RiskCriteriaOther criterion = criteria.get(line.getCriteriaOtherId());
            if (scale == null || criterion == null || criterion.getWeight() == null) {
                continue;
            }
            BigDecimal contribution = BigDecimal.valueOf(scale.getScaleScore()).multiply(criterion.getWeight());
            if (useGroupWeight) {
                RiskTypeHO riskType = riskTypes.get(criterion.getRiskTypeHoId());
                if (riskType != null && riskType.getWeight() != null) {
                    contribution = contribution.multiply(riskType.getWeight());
                }
            }
            total = total.add(contribution);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
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

    private Map<UUID, AuditObjectCategory> categoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<UUID, RiskCriteriaOther> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaOther> map = new HashMap<>();
        for (RiskCriteriaOther c : criteriaOtherRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<UUID, RiskCriteriaOtherScale> scalesById(UUID tenantId) {
        Map<UUID, RiskCriteriaOtherScale> map = new HashMap<>();
        for (RiskCriteriaOtherScale s : scaleRepository.findByTenantIdOrderByCriteriaOtherIdAscScaleScoreAsc(tenantId)) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private Map<UUID, RiskTypeHO> riskTypesById(UUID tenantId) {
        Map<UUID, RiskTypeHO> map = new HashMap<>();
        for (RiskTypeHO rt : riskTypeHoRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(rt.getId(), rt);
        }
        return map;
    }
}
