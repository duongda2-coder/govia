package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import com.govia.audit.riskscoring.masterdata.entity.RiskWeightByBusiness;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup1Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup2Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskScoreRankRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskWeightByBusinessRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQualitativeRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQuantitativeRowResponse;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Ket qua cham diem tong hop dinh tinh dinh luong theo tung chi nhanh" (sheet CT_Diem_All) - gop
 * ket qua cua {@link RiskBranchScoreQuantitativeService} (CT_Diem_DL) va
 * {@link RiskBranchScoreQualitativeService} (CT_Diem_DT) theo nghiep vu, khong tinh lai tu dau.
 *
 * <p>Moi nhom chi tieu dinh luong (RiskGroup1) va nhom cap 2 dinh tinh (RiskGroup2, qua group1 cha
 * cua no) duoc gan 1 "ma nghiep vu" (khop domain cua {@link RiskWeightByBusiness#getBusinessCode()}):
 * uu tien {@code RiskGroup1.businessLineCode} neu co nhap, khong thi dung tam {@code group1.code}
 * (xem Javadoc field businessLineCode). Diem dinh luong/dinh tinh cua 1 chi nhanh duoc cong don ve
 * theo tung ma nghiep vu nay (tu {@code scoresByCriteriaCode} cua DL va {@code scoresByGroup2Code}
 * cua DT).
 *
 * <p>Voi moi nghiep vu: neu co dong RiskWeightByBusiness hieu luc trong nam (fromYear..toYear) thi
 * quy doi = DT_tong x qualitativeWeight + DL_tong x quantitativeWeight; neu khong co (nghiep vu chi
 * co du lieu 1 ben, vi du Tien te kho quy/Tai chinh ke toan/Hoat dong the/IPCAS chi co DT, Ket qua
 * thuc hien KHKD chi co DL) thi lay thang tong cua ben co du lieu, khong nhan trong so.
 */
@Service
public class RiskBranchScoreCombinedService {

    private final RiskBranchScoreQuantitativeService quantitativeService;
    private final RiskBranchScoreQualitativeService qualitativeService;
    private final RiskCriteriaQuantitativeRepository criteriaQuantitativeRepository;
    private final RiskGroup1Repository group1Repository;
    private final RiskGroup2Repository group2Repository;
    private final RiskWeightByBusinessRepository weightByBusinessRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final RiskScoreRankRepository rankRepository;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;

    public RiskBranchScoreCombinedService(RiskBranchScoreQuantitativeService quantitativeService,
                                           RiskBranchScoreQualitativeService qualitativeService,
                                           RiskCriteriaQuantitativeRepository criteriaQuantitativeRepository,
                                           RiskGroup1Repository group1Repository,
                                           RiskGroup2Repository group2Repository,
                                           RiskWeightByBusinessRepository weightByBusinessRepository,
                                           AuditObjectUnitRepository auditObjectUnitRepository,
                                           RiskScoreRankRepository rankRepository,
                                           ExcelExportService excelExportService,
                                           WordExportService wordExportService) {
        this.quantitativeService = quantitativeService;
        this.qualitativeService = qualitativeService;
        this.criteriaQuantitativeRepository = criteriaQuantitativeRepository;
        this.group1Repository = group1Repository;
        this.group2Repository = group2Repository;
        this.weightByBusinessRepository = weightByBusinessRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.rankRepository = rankRepository;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
    }

    @Transactional(readOnly = true)
    public List<RiskBranchScoreCombinedRowResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<RiskBranchScoreQuantitativeRowResponse> dlRows = quantitativeService.list(year);
        List<RiskBranchScoreQualitativeRowResponse> dtRows = qualitativeService.list(year);

        Map<UUID, RiskGroup1> groups1 = group1ById(tenantId);
        Map<UUID, RiskGroup2> groups2 = group2ById(tenantId);

        Map<String, String> dlCriteriaCodeToBusinessLine = new HashMap<>();
        for (RiskCriteriaQuantitative c : criteriaQuantitativeRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            RiskGroup1 g = groups1.get(c.getGroup1Id());
            if (g != null) {
                dlCriteriaCodeToBusinessLine.put(c.getCode(), businessLineOf(g));
            }
        }
        Map<String, String> dtGroup2CodeToBusinessLine = new HashMap<>();
        for (RiskGroup2 g2 : groups2.values()) {
            RiskGroup1 g1 = groups1.get(g2.getGroup1Id());
            if (g1 != null) {
                dtGroup2CodeToBusinessLine.put(g2.getCode(), businessLineOf(g1));
            }
        }

        Map<String, RiskWeightByBusiness> weightsByBusinessLine = weightsEffectiveInYear(tenantId, year);
        List<RiskScoreRank> ranks = rankRepository.findByTenantIdOrderByFromYearAscScoreFromAsc(tenantId);
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);

        Map<String, RiskBranchScoreQuantitativeRowResponse> dlByBranch = new HashMap<>();
        dlRows.forEach(r -> dlByBranch.put(r.branchCode(), r));
        Map<String, RiskBranchScoreQualitativeRowResponse> dtByBranch = new HashMap<>();
        dtRows.forEach(r -> dtByBranch.put(r.branchCode(), r));

        Set<String> branchCodes = new HashSet<>();
        branchCodes.addAll(dlByBranch.keySet());
        branchCodes.addAll(dtByBranch.keySet());

        List<RiskBranchScoreCombinedRowResponse> result = new ArrayList<>();
        for (String branchCode : branchCodes.stream().sorted().toList()) {
            Map<String, BigDecimal> dlByBusinessLine = sumByKey(
                    dlByBranch.get(branchCode) != null ? dlByBranch.get(branchCode).scoresByCriteriaCode() : Map.of(),
                    dlCriteriaCodeToBusinessLine);
            Map<String, BigDecimal> dtByBusinessLine = sumByKey(
                    dtByBranch.get(branchCode) != null ? dtByBranch.get(branchCode).scoresByGroup2Code() : Map.of(),
                    dtGroup2CodeToBusinessLine);

            Set<String> businessLines = new HashSet<>();
            businessLines.addAll(dlByBusinessLine.keySet());
            businessLines.addAll(dtByBusinessLine.keySet());

            Map<String, BigDecimal> scoresByBusinessLine = new HashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (String businessLine : businessLines) {
                BigDecimal dl = dlByBusinessLine.getOrDefault(businessLine, BigDecimal.ZERO);
                BigDecimal dt = dtByBusinessLine.getOrDefault(businessLine, BigDecimal.ZERO);
                RiskWeightByBusiness weight = weightsByBusinessLine.get(businessLine);
                BigDecimal score;
                if (weight != null) {
                    BigDecimal qualitativeWeight = weight.getQualitativeWeight() != null ? weight.getQualitativeWeight() : BigDecimal.ZERO;
                    BigDecimal quantitativeWeight = weight.getQuantitativeWeight() != null ? weight.getQuantitativeWeight() : BigDecimal.ZERO;
                    score = dt.multiply(qualitativeWeight).add(dl.multiply(quantitativeWeight));
                } else {
                    // Khong co dong ty trong cho nghiep vu nay - lay thang tong cua ben co du lieu
                    // (nghiep vu chi co 1 ben), khong nhan trong so.
                    score = dl.add(dt);
                }
                scoresByBusinessLine.put(businessLine, score);
                total = total.add(score);
            }

            AuditObjectUnit unit = units.get(branchCode);
            total = total.setScale(2, RoundingMode.HALF_UP);
            String rankLabel = resolveRankLabel(ranks, total, year);
            result.add(new RiskBranchScoreCombinedRowResponse(branchCode, unit != null ? unit.getName() : null,
                    year, total, rankLabel, scoresByBusinessLine));
        }
        result.sort((a, b) -> b.totalScore().compareTo(a.totalScore()));
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Integer year) {
        List<String> businessLines = allBusinessLineCodes(TenantContext.getTenantId());
        return excelExportService.export("risk_score_branch_score_combined", exportColumns(businessLines), exportRows(year, businessLines));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(Integer year) {
        List<String> businessLines = allBusinessLineCodes(TenantContext.getTenantId());
        return wordExportService.export("Kết quả chấm điểm tổng hợp", exportColumns(businessLines), exportRows(year, businessLines));
    }

    private List<ExportColumn> exportColumns(List<String> businessLines) {
        List<ExportColumn> columns = new ArrayList<>(List.of(
                new ExportColumn("year", "Năm"),
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("branchName", "Tên chi nhánh"),
                new ExportColumn("totalScore", "Tổng điểm"),
                new ExportColumn("rankLabel", "Xếp hạng")));
        for (String businessLine : businessLines) {
            columns.add(new ExportColumn(businessLine, businessLine));
        }
        return columns;
    }

    private List<Map<String, Object>> exportRows(Integer year, List<String> businessLines) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RiskBranchScoreCombinedRowResponse row : list(year)) {
            Map<String, Object> map = new HashMap<>();
            map.put("year", row.year());
            map.put("branchCode", row.branchCode());
            map.put("branchName", row.branchName());
            map.put("totalScore", row.totalScore());
            map.put("rankLabel", row.rankLabel());
            for (String businessLine : businessLines) {
                map.put(businessLine, row.scoresByBusinessLineCode().get(businessLine));
            }
            rows.add(map);
        }
        return rows;
    }

    private List<String> allBusinessLineCodes(UUID tenantId) {
        Set<String> codes = new HashSet<>();
        for (RiskGroup1 g : group1Repository.findByTenantIdOrderByCodeAsc(tenantId)) {
            codes.add(businessLineOf(g));
        }
        for (RiskWeightByBusiness w : weightByBusinessRepository.findByTenantIdOrderByBusinessCodeAscFromYearAsc(tenantId)) {
            codes.add(w.getBusinessCode());
        }
        return codes.stream().sorted().toList();
    }

    /** Ma nghiep vu cua 1 nhom cap 1: uu tien businessLineCode neu da nhap, khong thi dung tam ma
     * cua chinh group1 do (phu hop khi group1 da anh xa 1:1 voi 1 nghiep vu, vi du cac group1 dinh
     * tinh LN/CE/DP/MF/GA/TF/CD/IP/XD trong sheet CT_Diem_DT). */
    private String businessLineOf(RiskGroup1 group1) {
        return group1.getBusinessLineCode() != null && !group1.getBusinessLineCode().isBlank()
                ? group1.getBusinessLineCode()
                : group1.getCode();
    }

    private Map<String, BigDecimal> sumByKey(Map<String, BigDecimal> scoresByCode, Map<String, String> codeToBusinessLine) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : scoresByCode.entrySet()) {
            String businessLine = codeToBusinessLine.get(entry.getKey());
            if (businessLine == null || entry.getValue() == null) {
                continue;
            }
            result.merge(businessLine, entry.getValue(), BigDecimal::add);
        }
        return result;
    }

    private Map<String, RiskWeightByBusiness> weightsEffectiveInYear(UUID tenantId, Integer year) {
        Map<String, RiskWeightByBusiness> map = new HashMap<>();
        for (RiskWeightByBusiness w : weightByBusinessRepository.findByTenantIdOrderByBusinessCodeAscFromYearAsc(tenantId)) {
            if (!w.isActive()) {
                continue;
            }
            boolean afterFrom = w.getFromYear() == null || year >= w.getFromYear();
            boolean beforeTo = w.getToYear() == null || year <= w.getToYear();
            if (afterFrom && beforeTo) {
                map.putIfAbsent(w.getBusinessCode(), w);
            }
        }
        return map;
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

    private Map<UUID, RiskGroup1> group1ById(UUID tenantId) {
        Map<UUID, RiskGroup1> map = new HashMap<>();
        group1Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<UUID, RiskGroup2> group2ById(UUID tenantId) {
        Map<UUID, RiskGroup2> map = new HashMap<>();
        group2Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }
}
