package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreExpertRankRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreExpertRankResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskBranchScoreExpertRank;
import com.govia.audit.riskscoring.scoring.repository.RiskBranchScoreExpertRankRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Cap nhat bang xep hang rui ro chi nhanh theo y kien chuyen gia" (sheet ZTC_DGRR_cg) - 2 thao tac
 * chinh dung theo tai lieu goc: syncFromSource ("nut cap nhat du lieu tu nguon" - keo du lieu moi
 * nhat tu ket qua cham diem tong hop, KHONG ghi de cac truong chuyen gia da nhap) va list ("nut xem
 * lai ket qua"). update() dung khi chuyen gia nhap/sua xep hang lai + ly do + ket qua xep loai.
 */
@Service
public class RiskBranchScoreExpertRankService {

    private final RiskBranchScoreExpertRankRepository repository;
    private final RiskBranchScoreCombinedService combinedService;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditLogService auditLogService;

    public RiskBranchScoreExpertRankService(RiskBranchScoreExpertRankRepository repository,
                                             RiskBranchScoreCombinedService combinedService,
                                             AuditObjectUnitRepository auditObjectUnitRepository,
                                             AuditLogService auditLogService) {
        this.repository = repository;
        this.combinedService = combinedService;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.auditLogService = auditLogService;
    }

    /** "Nut xem lai ket qua": danh sach da luu cho 1 nam, KHONG tinh toan lai gi them. */
    @Transactional(readOnly = true)
    public List<RiskBranchScoreExpertRankResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);
        return repository.findByTenantIdAndYearOrderByTotalScoreDesc(tenantId, year).stream()
                .map(item -> toResponse(item, units))
                .toList();
    }

    /**
     * "Nut cap nhat du lieu tu nguon": doi voi moi chi nhanh co ket qua o "Ket qua cham diem tong
     * hop" nam nay, tao dong moi (neu chua co) hoac chi lam moi total_score/base_rank_label (neu da
     * co) - KHONG dung tay vao cac truong chuyen gia da nhap (re_rank_label, reason, assessed_date,
     * expert_name, final_rank_label) de tranh mat du lieu ra soat da lam.
     */
    @Transactional
    public List<RiskBranchScoreExpertRankResponse> syncFromSource(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        for (RiskBranchScoreCombinedRowResponse combined : combinedService.list(year)) {
            RiskBranchScoreExpertRank item = repository
                    .findByTenantIdAndBranchCodeAndYear(tenantId, combined.branchCode(), year)
                    .orElseGet(() -> {
                        RiskBranchScoreExpertRank created = new RiskBranchScoreExpertRank();
                        created.setTenantId(tenantId);
                        created.setBranchCode(combined.branchCode());
                        created.setYear(year);
                        return created;
                    });
            item.setTotalScore(combined.totalScore());
            item.setBaseRankLabel(combined.rankLabel());
            repository.save(item);
        }

        auditLogService.record("RiskBranchScoreExpertRank", null, AuditAction.UPDATE,
                "Cap nhat du lieu tu nguon bang xep hang rui ro chi nhanh theo YKCG nam " + year);
        return list(year);
    }

    @Transactional
    public RiskBranchScoreExpertRankResponse update(UUID id, RiskBranchScoreExpertRankRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskBranchScoreExpertRank item = getOwnedOrThrow(tenantId, id);
        item.setReRankLabel(request.reRankLabel());
        item.setReason(request.reason());
        item.setAssessedDate(request.assessedDate());
        item.setExpertName(request.expertName());
        item.setFinalRankLabel(request.finalRankLabel());
        item = repository.save(item);

        auditLogService.record("RiskBranchScoreExpertRank", item.getId(), AuditAction.UPDATE,
                "Cap nhat xep hang rui ro chi nhanh theo YKCG: " + item.getBranchCode() + "/" + item.getYear());
        return toResponse(item, unitsByCode(tenantId));
    }

    private RiskBranchScoreExpertRank getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_BRANCH_SCORE_EXPERT_RANK_NOT_FOUND",
                        "Khong tim thay dong xep hang rui ro chi nhanh theo YKCG", HttpStatus.NOT_FOUND));
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }

    private RiskBranchScoreExpertRankResponse toResponse(RiskBranchScoreExpertRank item, Map<String, AuditObjectUnit> units) {
        AuditObjectUnit unit = units.get(item.getBranchCode());
        return new RiskBranchScoreExpertRankResponse(item.getId(), item.getYear(),
                item.getBranchCode(), unit != null ? unit.getName() : null,
                item.getTotalScore(), item.getBaseRankLabel(), item.getReRankLabel(), item.getReason(),
                item.getAssessedDate(), item.getExpertName(), item.getFinalRankLabel(), item.getUpdatedBy());
    }
}
