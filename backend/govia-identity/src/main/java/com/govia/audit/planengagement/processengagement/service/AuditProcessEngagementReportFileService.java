package com.govia.audit.planengagement.processengagement.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.dto.AuditWorkReportFileResponse;
import com.govia.audit.planengagement.processengagement.entity.AuditProcessEngagement;
import com.govia.audit.planengagement.processengagement.repository.AuditProcessEngagementRepository;
import com.govia.core.attachment.Attachment;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/**
 * "2. File báo cáo khác" cua man hinh "QL CKT quy trinh" (Tao CKT (4).xlsx) - dung file dinh kem o
 * CAP CKT quy trinh (khong gan voi tung CKT con), cho ca doan quy trinh dung chung. Clone cua
 * {@link com.govia.audit.planengagement.service.AuditWorkReportFileService} nhung don gian hon:
 * "Loai bao cao" tinh theo mang nghiep vu CO DINH cua chinh CKT quy trinh (thay vi theo tung
 * nguoi upload nhu ben CKT chi nhanh, vi CKT quy trinh khong co khai niem nhom/thanh vien rieng).
 */
@Service
public class AuditProcessEngagementReportFileService {

    private static final String ENTITY_NAME = "AUDIT_PROCESS_ENGAGEMENT_REPORT_FILE";

    private final AttachmentService attachmentService;
    private final AuditProcessEngagementRepository processEngagementRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    public AuditProcessEngagementReportFileService(AttachmentService attachmentService, AuditProcessEngagementRepository processEngagementRepository,
                                                     AuditMasterDataItemRepository masterDataItemRepository, EmployeeRepository employeeRepository,
                                                     UserAccountRepository userAccountRepository) {
        this.attachmentService = attachmentService;
        this.processEngagementRepository = processEngagementRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public AuditWorkReportFileResponse upload(UUID processEngagementId, MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessEngagement engagement = getOwnedOrThrow(tenantId, processEngagementId);
        Attachment attachment = attachmentService.upload(ENTITY_NAME, processEngagementId, file);
        return toResponse(attachment, tenantId, engagement);
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditWorkReportFileResponse> list(UUID processEngagementId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessEngagement engagement = getOwnedOrThrow(tenantId, processEngagementId);
        return attachmentService.listByEntity(ENTITY_NAME, processEngagementId).stream()
                .map(a -> toResponse(a, tenantId, engagement))
                .toList();
    }

    /** Chi nguoi da upload moi duoc xoa file cua chinh minh - dung nguyen tac voi AuditWorkReportFileService. */
    @Transactional
    public void delete(UUID processEngagementId, UUID attachmentId, String actorUsername) {
        Attachment attachment = attachmentService.getMetadata(attachmentId);
        if (attachment == null || !ENTITY_NAME.equals(attachment.getEntityName()) || !processEngagementId.equals(attachment.getEntityId())) {
            throw new BusinessException("AUDIT_PROCESS_ENGAGEMENT_REPORT_FILE_NOT_FOUND", "Khong tim thay file", HttpStatus.NOT_FOUND);
        }
        if (!attachment.getCreatedBy().equalsIgnoreCase(actorUsername)) {
            throw new BusinessException("AUDIT_PROCESS_ENGAGEMENT_REPORT_FILE_NOT_OWNER",
                    "Chi nguoi da upload moi duoc xoa file nay", HttpStatus.FORBIDDEN);
        }
        attachmentService.delete(attachmentId);
    }

    private AuditWorkReportFileResponse toResponse(Attachment attachment, UUID tenantId, AuditProcessEngagement engagement) {
        AuditMasterDataItem segment = masterDataItemRepository.findById(engagement.getBusinessSegmentId()).orElse(null);
        String segmentCode = segment == null ? null : segment.getCode();
        String reportType = resolveReportType(segmentCode);
        Optional<UserAccount> account = attachment.getCreatedBy() == null ? Optional.empty()
                : userAccountRepository.findByTenantIdAndUsername(tenantId, attachment.getCreatedBy());
        String uploaderName = account.flatMap(a -> a.getEmployeeId() == null ? Optional.empty() : employeeRepository.findById(a.getEmployeeId()))
                .map(Employee::getFullName).orElse(null);

        return new AuditWorkReportFileResponse(attachment.getId(), segmentCode, attachment.getCreatedAt(),
                attachment.getCreatedBy(), uploaderName, reportType, attachment.getFileName());
    }

    private String resolveReportType(String segmentCode) {
        if ("LN".equalsIgnoreCase(segmentCode)) {
            return "TINDUNG";
        }
        if ("CE".equalsIgnoreCase(segmentCode)) {
            return "DIEUHANH";
        }
        return "NTINDUNG";
    }

    private AuditProcessEngagement getOwnedOrThrow(UUID tenantId, UUID id) {
        return processEngagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan quy trinh", HttpStatus.NOT_FOUND));
    }
}
