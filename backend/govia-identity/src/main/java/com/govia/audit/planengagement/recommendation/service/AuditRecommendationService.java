package com.govia.audit.planengagement.recommendation.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Lưu mã kiến nghị" (Khối C, chức năng "3. Thêm kiến nghị") - catalog rieng cho tung cuoc kiem
 * toan. Luon co san dong mac dinh KNKT000/CE/"Kiến nghị chung" (dung dac ta) - tu seed neu catalog
 * cua engagement do dang rong.
 */
@Service
public class AuditRecommendationService {

    private static final String DEFAULT_CODE = "KNKT000";
    private static final String DEFAULT_SEGMENT_CODE = "CE";
    private static final String DEFAULT_CONTENT = "Kiến nghị chung";

    private final AuditRecommendationRepository repository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;

    public AuditRecommendationService(AuditRecommendationRepository repository, AuditEngagementRepository engagementRepository,
                                       AuditMasterDataItemRepository masterDataItemRepository, AuditLogService auditLogService) {
        this.repository = repository;
        this.engagementRepository = engagementRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public List<AuditRecommendationResponse> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);
        return toResponses(tenantId, repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId));
    }

    @Transactional
    public AuditRecommendationResponse create(UUID engagementId, AuditRecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);

        String nextCode = nextCode(tenantId, engagementId);
        AuditRecommendation recommendation = new AuditRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setEngagementId(engagementId);
        recommendation.setCode(nextCode);
        recommendation.setBusinessSegmentId(request.businessSegmentId());
        recommendation.setContent(request.content());
        recommendation = repository.save(recommendation);

        auditLogService.record("AuditRecommendation", recommendation.getId(), AuditAction.CREATE,
                "Them kien nghi " + nextCode + " cho CKT");
        return toResponse(recommendation, segmentsById(tenantId));
    }

    private String nextCode(UUID tenantId, UUID engagementId) {
        int nextSeq = repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId).size();
        return String.format("KNKT%03d", nextSeq);
    }

    private void ensureDefaultSeeded(UUID tenantId, UUID engagementId) {
        if (!repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId).isEmpty()) {
            return;
        }
        AuditMasterDataItem defaultSegment = masterDataItemRepository
                .findByTenantIdAndCategoryAndCode(tenantId, BUSINESS_SEGMENT, DEFAULT_SEGMENT_CODE).orElse(null);

        AuditRecommendation defaultRecommendation = new AuditRecommendation();
        defaultRecommendation.setTenantId(tenantId);
        defaultRecommendation.setEngagementId(engagementId);
        defaultRecommendation.setCode(DEFAULT_CODE);
        defaultRecommendation.setBusinessSegmentId(defaultSegment == null ? null : defaultSegment.getId());
        defaultRecommendation.setContent(DEFAULT_CONTENT);
        repository.save(defaultRecommendation);
    }

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> segmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private List<AuditRecommendationResponse> toResponses(UUID tenantId, List<AuditRecommendation> recommendations) {
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        return recommendations.stream().map(r -> toResponse(r, segments)).toList();
    }

    private AuditRecommendationResponse toResponse(AuditRecommendation recommendation, Map<UUID, AuditMasterDataItem> segments) {
        AuditMasterDataItem segment = segments.get(recommendation.getBusinessSegmentId());
        return new AuditRecommendationResponse(recommendation.getId(), recommendation.getEngagementId(), recommendation.getCode(),
                recommendation.getBusinessSegmentId(), segment == null ? null : segment.getCode(), recommendation.getContent());
    }
}
