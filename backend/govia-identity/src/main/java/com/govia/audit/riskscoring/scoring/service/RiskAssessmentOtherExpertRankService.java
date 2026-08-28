package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProcess;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProject;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectSubsidiary;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProcessRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProjectRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectSubsidiaryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherExpertRankRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherExpertRankResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherRankingResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherExpertRank;
import com.govia.audit.riskscoring.scoring.repository.RiskAssessmentOtherExpertRankRepository;
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
 * "Cap nhat bang xep hang rui ro theo y kien chuyen gia cua DTKT khac" (sheet ZTC_XHRR_KHAC_CG) -
 * 2 thao tac chinh dung theo tai lieu goc: syncFromSource ("nut cap nhat du lieu tu nguon" - keo du
 * lieu moi nhat tu ZTC_BXHRR_KHAC, KHONG ghi de cac truong chuyen gia da nhap) va list ("nut xem lai
 * ket qua"). update() dung khi chuyen gia nhap/sua xep hang lai + ly do + ket qua xep loai.
 */
@Service
public class RiskAssessmentOtherExpertRankService {

    private final RiskAssessmentOtherExpertRankRepository repository;
    private final RiskAssessmentOtherRankingService rankingService;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditObjectSubsidiaryRepository auditObjectSubsidiaryRepository;
    private final AuditObjectProjectRepository auditObjectProjectRepository;
    private final AuditObjectProcessRepository auditObjectProcessRepository;
    private final AuditLogService auditLogService;

    public RiskAssessmentOtherExpertRankService(RiskAssessmentOtherExpertRankRepository repository,
                                                 RiskAssessmentOtherRankingService rankingService,
                                                 AuditObjectCategoryRepository auditObjectCategoryRepository,
                                                 AuditObjectUnitRepository auditObjectUnitRepository,
                                                 AuditObjectSubsidiaryRepository auditObjectSubsidiaryRepository,
                                                 AuditObjectProjectRepository auditObjectProjectRepository,
                                                 AuditObjectProcessRepository auditObjectProcessRepository,
                                                 AuditLogService auditLogService) {
        this.repository = repository;
        this.rankingService = rankingService;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.auditObjectSubsidiaryRepository = auditObjectSubsidiaryRepository;
        this.auditObjectProjectRepository = auditObjectProjectRepository;
        this.auditObjectProcessRepository = auditObjectProcessRepository;
        this.auditLogService = auditLogService;
    }

    /** "Nut xem lai ket qua": danh sach da luu cho 1 nam, KHONG tinh toan lai gi them. */
    @Transactional(readOnly = true)
    public List<RiskAssessmentOtherExpertRankResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        return repository.findByTenantIdAndYearOrderByRiskScoreDesc(tenantId, year).stream()
                .map(item -> toResponse(item, categories))
                .toList();
    }

    /**
     * "Nut cap nhat du lieu tu nguon": doi voi moi doi tuong co ket qua o ZTC_BXHRR_KHAC nam nay,
     * tao dong moi (neu chua co) hoac chi lam moi risk_score/base_rank_label (neu da co) - KHONG
     * dung tay vao cac truong chuyen gia da nhap (re_rank_label, reason, assessed_date, expert_name,
     * final_rank_label) de tranh mat du lieu ra soat da lam.
     */
    @Transactional
    public List<RiskAssessmentOtherExpertRankResponse> syncFromSource(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        Map<String, UUID> categoryIdsByCode = new HashMap<>();
        categories.values().forEach(c -> categoryIdsByCode.put(c.getCode(), c.getId()));

        for (RiskAssessmentOtherRankingResponse ranked : rankingService.listByYear(year)) {
            UUID categoryId = categoryIdsByCode.get(ranked.auditObjectCategoryCode());
            if (categoryId == null) {
                continue;
            }
            RiskAssessmentOtherExpertRank item = repository
                    .findByTenantIdAndAuditObjectCategoryIdAndAuditObjectCodeAndYear(tenantId, categoryId, ranked.auditObjectCode(), year)
                    .orElseGet(() -> {
                        RiskAssessmentOtherExpertRank created = new RiskAssessmentOtherExpertRank();
                        created.setTenantId(tenantId);
                        created.setAuditObjectCategoryId(categoryId);
                        created.setAuditObjectCode(ranked.auditObjectCode());
                        created.setYear(year);
                        return created;
                    });
            item.setRiskScore(ranked.riskScore());
            item.setBaseRankLabel(ranked.rankLabel());
            repository.save(item);
        }

        auditLogService.record("RiskAssessmentOtherExpertRank", null, AuditAction.UPDATE,
                "Cap nhat du lieu tu nguon bang xep hang theo YKCG nam " + year);
        return list(year);
    }

    @Transactional
    public RiskAssessmentOtherExpertRankResponse update(UUID id, RiskAssessmentOtherExpertRankRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskAssessmentOtherExpertRank item = getOwnedOrThrow(tenantId, id);
        item.setReRankLabel(request.reRankLabel());
        item.setReason(request.reason());
        item.setAssessedDate(request.assessedDate());
        item.setExpertName(request.expertName());
        item.setFinalRankLabel(request.finalRankLabel());
        item = repository.save(item);

        auditLogService.record("RiskAssessmentOtherExpertRank", item.getId(), AuditAction.UPDATE,
                "Cap nhat xep hang theo YKCG: " + item.getAuditObjectCode() + "/" + item.getYear());
        return toResponse(item, categoriesById(tenantId));
    }

    private RiskAssessmentOtherExpertRank getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_ASSESSMENT_OTHER_EXPERT_RANK_NOT_FOUND",
                        "Khong tim thay dong xep hang theo YKCG", HttpStatus.NOT_FOUND));
    }

    private String resolveAuditObjectName(UUID tenantId, String categoryCode, String auditObjectCode) {
        if (categoryCode == null) {
            return null;
        }
        return switch (categoryCode) {
            case "HO" -> auditObjectUnitRepository.findByTenantIdAndCode(tenantId, auditObjectCode).map(AuditObjectUnit::getName).orElse(null);
            case "CTC" -> auditObjectSubsidiaryRepository.findByTenantIdAndCode(tenantId, auditObjectCode).map(AuditObjectSubsidiary::getName).orElse(null);
            case "KTQT" -> auditObjectProcessRepository.findByTenantIdAndCode(tenantId, auditObjectCode).map(AuditObjectProcess::getName).orElse(null);
            default -> auditObjectProjectRepository.findByTenantIdAndCode(tenantId, auditObjectCode).map(AuditObjectProject::getName).orElse(null);
        };
    }

    private Map<UUID, AuditObjectCategory> categoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private RiskAssessmentOtherExpertRankResponse toResponse(RiskAssessmentOtherExpertRank item, Map<UUID, AuditObjectCategory> categories) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        String categoryCode = category != null ? category.getCode() : null;
        String objectName = resolveAuditObjectName(item.getTenantId(), categoryCode, item.getAuditObjectCode());
        return new RiskAssessmentOtherExpertRankResponse(item.getId(), item.getYear(),
                categoryCode, category != null ? category.getName() : null,
                item.getAuditObjectCode(), objectName,
                item.getRiskScore(), item.getBaseRankLabel(), item.getReRankLabel(), item.getReason(),
                item.getAssessedDate(), item.getExpertName(), item.getFinalRankLabel(), item.getUpdatedBy());
    }
}
