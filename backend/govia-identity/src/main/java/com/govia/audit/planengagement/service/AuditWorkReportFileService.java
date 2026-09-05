package com.govia.audit.planengagement.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.dto.AuditWorkReportFileResponse;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "1. File báo cáo khác" (man hinh "Quản lý công việc"): KHONG tao bang moi - dung lai
 * {@link AttachmentService} dung chung (entityName="AUDIT_WORK_REPORT_FILE", entityId=
 * engagementId), chi bo them cot dan xuat "Loại báo cáo" theo mang nghiep vu cua nguoi upload
 * (LN -> TINDUNG, CE -> DIEUHANH, con lai -> NTINDUNG - dung dac ta).
 */
@Service
public class AuditWorkReportFileService {

    private static final String ENTITY_NAME = "AUDIT_WORK_REPORT_FILE";

    private final AttachmentService attachmentService;
    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    public AuditWorkReportFileService(AttachmentService attachmentService, AuditEngagementRepository engagementRepository,
                                       AuditEngagementGroupRepository groupRepository, AuditEngagementGroupMemberRepository memberRepository,
                                       AuditMasterDataItemRepository masterDataItemRepository, EmployeeRepository employeeRepository,
                                       UserAccountRepository userAccountRepository) {
        this.attachmentService = attachmentService;
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public AuditWorkReportFileResponse upload(UUID engagementId, MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        Attachment attachment = attachmentService.upload(ENTITY_NAME, engagementId, file);
        return toResponse(attachment, tenantId, engagementId);
    }

    @Transactional(readOnly = true)
    public List<AuditWorkReportFileResponse> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        return attachmentService.listByEntity(ENTITY_NAME, engagementId).stream()
                .map(a -> toResponse(a, tenantId, engagementId))
                .toList();
    }

    /** Chi nguoi da upload moi duoc xoa file cua chinh minh (dung dac ta "chỉ user upload mới
     * được quyền xoá file của chính mình"). */
    @Transactional
    public void delete(UUID engagementId, UUID attachmentId, String actorUsername) {
        Attachment attachment = attachmentService.getMetadata(attachmentId);
        if (attachment == null || !ENTITY_NAME.equals(attachment.getEntityName()) || !engagementId.equals(attachment.getEntityId())) {
            throw new BusinessException("AUDIT_WORK_REPORT_FILE_NOT_FOUND", "Khong tim thay file", HttpStatus.NOT_FOUND);
        }
        if (!attachment.getCreatedBy().equalsIgnoreCase(actorUsername)) {
            throw new BusinessException("AUDIT_WORK_REPORT_FILE_NOT_OWNER",
                    "Chi nguoi da upload moi duoc xoa file nay", HttpStatus.FORBIDDEN);
        }
        attachmentService.delete(attachmentId);
    }

    private AuditWorkReportFileResponse toResponse(Attachment attachment, UUID tenantId, UUID engagementId) {
        String segmentCode = resolveUploaderBusinessSegmentCode(tenantId, engagementId, attachment.getCreatedBy());
        String reportType = resolveReportType(segmentCode);
        Optional<UserAccount> account = attachment.getCreatedBy() == null ? Optional.empty()
                : userAccountRepository.findByTenantIdAndUsername(tenantId, attachment.getCreatedBy());
        String uploaderName = account.flatMap(a -> a.getEmployeeId() == null ? Optional.empty() : employeeRepository.findById(a.getEmployeeId()))
                .map(Employee::getFullName).orElse(null);

        return new AuditWorkReportFileResponse(attachment.getId(), segmentCode, attachment.getCreatedAt(),
                attachment.getCreatedBy(), uploaderName, reportType, attachment.getFileName());
    }

    private String resolveUploaderBusinessSegmentCode(UUID tenantId, UUID engagementId, String uploaderUsername) {
        if (uploaderUsername == null) {
            return null;
        }
        UserAccount account = userAccountRepository.findByTenantIdAndUsername(tenantId, uploaderUsername).orElse(null);
        if (account == null || account.getEmployeeId() == null) {
            return null;
        }
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        AuditEngagementGroupMember member = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds).stream()
                .filter(m -> m.getEmployeeId().equals(account.getEmployeeId()))
                .findFirst().orElse(null);
        if (member == null || member.getBusinessSegment1Id() == null) {
            return null;
        }
        Map<UUID, AuditMasterDataItem> segments = masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
        AuditMasterDataItem segment = segments.get(member.getBusinessSegment1Id());
        return segment == null ? null : segment.getCode();
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

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }
}
