package com.govia.audit.planengagement.processengagement.service;

import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditWorkAssignmentService;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.planengagement.processengagement.repository.AuditProcessEngagementRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cac man hinh doc-tong-hop cua "QL CKT quy trinh" (Tao CKT (4).xlsx) - "Chi tiết đoàn", "Quản lý
 * công việc", "Quản lý TTSS", "Quản lý KN": deu gom du lieu cua TAT CA CKT con (AuditEngagement.
 * processEngagementId = CKT quy trinh nay) roi tai su dung nguyen ham list/tong hop san co cua
 * tung module con, khong tu viet lai truy van/logic.
 */
@Service
public class AuditProcessEngagementAggregateService {

    private final AuditProcessEngagementRepository processEngagementRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementMonitoringService monitoringService;
    private final AuditWorkAssignmentService workAssignmentService;
    private final AuditTtssService ttssService;
    private final AuditRecommendationService recommendationService;

    public AuditProcessEngagementAggregateService(AuditProcessEngagementRepository processEngagementRepository,
                                                    AuditEngagementRepository engagementRepository,
                                                    AuditEngagementMonitoringService monitoringService,
                                                    AuditWorkAssignmentService workAssignmentService,
                                                    AuditTtssService ttssService, AuditRecommendationService recommendationService) {
        this.processEngagementRepository = processEngagementRepository;
        this.engagementRepository = engagementRepository;
        this.monitoringService = monitoringService;
        this.workAssignmentService = workAssignmentService;
        this.ttssService = ttssService;
        this.recommendationService = recommendationService;
    }

    /** "6. Chi tiết đoàn" - 1 dong / CKT con, kem so lieu tong hop (giong het man hinh "Quản lý đợt
     * kiểm toán" nhung chi loc trong pham vi CKT quy trinh nay). */
    @Transactional(readOnly = true)
    public List<AuditEngagementMonitoringResponse> children(UUID processEngagementId) {
        requireOwnedProcessEngagement(processEngagementId);
        return monitoringService.listByProcessEngagement(processEngagementId);
    }

    /** "3. Quản lý công việc" - gom cong viec da phan cong cua tat ca CKT con, ca CBKT/THKT/DCKT. */
    @Transactional(readOnly = true)
    public List<AuditWorkManagementItemResponse> workManagement(UUID processEngagementId, CurrentUserPrincipal principal) {
        requireOwnedProcessEngagement(processEngagementId);
        return workAssignmentService.listForEngagements(childIds(processEngagementId), principal);
    }

    /** "4. Quản lý TTSS" - gom TTSS cua tat ca CKT con. */
    @Transactional(readOnly = true)
    public List<AuditTtssRecordResponse> ttss(UUID processEngagementId) {
        requireOwnedProcessEngagement(processEngagementId);
        return ttssService.listByEngagementIds(childIds(processEngagementId));
    }

    /** "5. Quản lý kiến nghị" - gom kien nghi cua tat ca CKT con. */
    @Transactional(readOnly = true)
    public List<AuditRecommendationResponse> recommendations(UUID processEngagementId) {
        requireOwnedProcessEngagement(processEngagementId);
        return recommendationService.listByEngagementIds(childIds(processEngagementId));
    }

    private List<UUID> childIds(UUID processEngagementId) {
        UUID tenantId = TenantContext.getTenantId();
        return engagementRepository.findByTenantIdAndProcessEngagementIdOrderByCreatedAtAsc(tenantId, processEngagementId).stream()
                .map(AuditEngagement::getId).toList();
    }

    private void requireOwnedProcessEngagement(UUID processEngagementId) {
        UUID tenantId = TenantContext.getTenantId();
        processEngagementRepository.findById(processEngagementId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan quy trinh", HttpStatus.NOT_FOUND));
    }
}
