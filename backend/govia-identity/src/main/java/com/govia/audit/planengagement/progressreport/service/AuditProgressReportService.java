package com.govia.audit.planengagement.progressreport.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.approval.AuditWorkApprovalChainResolver;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportApproveRequest;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportResponse;
import com.govia.audit.planengagement.progressreport.entity.AuditProgressReport;
import com.govia.audit.planengagement.progressreport.repository.AuditProgressReportRepository;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.workflow.dto.CompleteTaskRequest;
import com.govia.identity.workflow.service.WorkflowTaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Báo cáo tiến độ" (Khối B) - 1 chuc nang trong man hinh "Quan ly cong viec THKT". Khong co API
 * tao thu cong: {@link #recordUpload} duoc goi TU DONG boi AuditTtssService moi lan "Upload file
 * TTSS" xong.
 */
@Service
public class AuditProgressReportService {

    private static final String PROCESS_KEY = "audit_progress_report_approval";
    private static final String ATTACHMENT_ENTITY_NAME = "AUDIT_PROGRESS_REPORT";

    private final AuditProgressReportRepository reportRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditWorkItemRepository workItemRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final AttachmentService attachmentService;
    private final AuditWorkApprovalChainResolver approvalChainResolver;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WorkflowTaskService workflowTaskService;
    private final AuditLogService auditLogService;

    public AuditProgressReportService(AuditProgressReportRepository reportRepository, AuditEngagementRepository engagementRepository,
                                       AuditEngagementGroupRepository groupRepository, AuditEngagementGroupMemberRepository memberRepository,
                                       AuditEngagementAssignmentRepository assignmentRepository, AuditWorkItemRepository workItemRepository,
                                       AuditMasterDataItemRepository masterDataItemRepository, EmployeeRepository employeeRepository,
                                       AttachmentService attachmentService, AuditWorkApprovalChainResolver approvalChainResolver,
                                       RuntimeService runtimeService, TaskService taskService, WorkflowTaskService workflowTaskService,
                                       AuditLogService auditLogService) {
        this.reportRepository = reportRepository;
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.attachmentService = attachmentService;
        this.approvalChainResolver = approvalChainResolver;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.workflowTaskService = workflowTaskService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditProgressReportResponse> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        List<AuditProgressReport> reports = reportRepository.findByTenantIdAndEngagementIdOrderByReportDateDesc(tenantId, engagementId);
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        Map<UUID, Employee> employees = employeeRepository.findAllById(
                        reports.stream().map(AuditProgressReport::getReportedEmployeeId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));
        return reports.stream().map(r -> toResponse(r, segments, employees)).toList();
    }

    /**
     * Duoc AuditTtssService goi ngay sau khi parse xong 1 lan "Upload file TTSS": tinh lai cac chi
     * so tu chinh cac dong TTSS vua upload (khong tinh lai toan bo lich su), luu 1 report MOI
     * (reportRound tu tang theo engagement+mang NV+nguoi bao cao), va dinh kem file goc qua
     * Attachment chung.
     */
    @Transactional
    public AuditProgressReportResponse recordUpload(UUID engagementId, UUID reportedEmployeeId, UUID businessSegmentId,
                                                      List<AuditTtssRecord> uploadedRecords, String note, String uploaderUsername,
                                                      MultipartFile originalFile) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);

        Set<String> findingCodes = uploadedRecords.stream().map(AuditTtssRecord::getFindingCode)
                .filter(code -> code != null && !code.isBlank()).collect(Collectors.toSet());
        long totalFindings = uploadedRecords.stream().filter(r -> r.getFindingCode() != null && !r.getFindingCode().isBlank()).count();
        long totalMaterialFindings = uploadedRecords.stream()
                .filter(r -> r.isMaterial() && r.getFindingCode() != null && !r.getFindingCode().isBlank()).count();
        Set<String> materialFindingCodes = uploadedRecords.stream()
                .filter(r -> r.isMaterial() && r.getFindingCode() != null && !r.getFindingCode().isBlank())
                .map(AuditTtssRecord::getFindingCode).collect(Collectors.toSet());

        int[] sampleCounts = countAssignedSamples(tenantId, engagementId, reportedEmployeeId, businessSegmentId);

        AuditProgressReport report = new AuditProgressReport();
        report.setTenantId(tenantId);
        report.setEngagementId(engagementId);
        report.setBusinessSegmentId(businessSegmentId);
        report.setReportedEmployeeId(reportedEmployeeId);
        report.setTotalFindings((int) totalFindings);
        report.setTotalTtss(findingCodes.size());
        report.setTotalMaterialFindings((int) totalMaterialFindings);
        report.setTotalMaterialTtss(materialFindingCodes.size());
        report.setTotalSamples(sampleCounts[0]);
        report.setCompletedSamples(sampleCounts[1]);
        report.setReportDate(LocalDate.now());
        report.setReportRound((int) reportRepository.countByTenantIdAndEngagementIdAndBusinessSegmentIdAndReportedEmployeeId(
                tenantId, engagementId, businessSegmentId, reportedEmployeeId) + 1);
        report.setReportedByUsername(uploaderUsername);
        report.setNote(note);
        report = reportRepository.save(report);

        if (originalFile != null && !originalFile.isEmpty()) {
            attachmentService.upload(ATTACHMENT_ENTITY_NAME, report.getId(), originalFile);
        }

        auditLogService.record("AuditProgressReport", report.getId(), AuditAction.CREATE,
                "Sinh bao cao tien do lan " + report.getReportRound() + " tu upload TTSS cho CKT " + engagement.getCode());
        return toResponse(report, segmentsById(tenantId), employeeMap(reportedEmployeeId));
    }

    /** Dem "tong so luong mau"/"so luong mau hoan thanh" - proxy qua AuditEngagementAssignment theo
     * (nguoi bao cao, mang nghiep vu cua cong viec), KHONG dem mau that o 16 bang chon mau. */
    private int[] countAssignedSamples(UUID tenantId, UUID engagementId, UUID employeeId, UUID businessSegmentId) {
        if (employeeId == null) {
            return new int[] {0, 0};
        }
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<UUID> memberIds = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds).stream()
                .filter(m -> m.getEmployeeId().equals(employeeId))
                .map(AuditEngagementGroupMember::getId).toList();
        if (memberIds.isEmpty()) {
            return new int[] {0, 0};
        }
        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId, memberIds);
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));

        int total = 0;
        int completed = 0;
        for (AuditEngagementAssignment assignment : assignments) {
            AuditWorkItem workItem = workItems.get(assignment.getWorkItemId());
            if (workItem == null || (businessSegmentId != null && !businessSegmentId.equals(workItem.getBusinessSegmentId()))) {
                continue;
            }
            total++;
            if (assignment.getStatus() == AssignmentStatus.DONE) {
                completed++;
            }
        }
        return new int[] {total, completed};
    }

    @Transactional
    public List<UUID> approve(UUID engagementId, AuditProgressReportApproveRequest request, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireTeamLead(tenantId, engagement, principal.employeeCode());

        List<UUID> approvedIds = new ArrayList<>();
        for (UUID reportId : request.reportIds()) {
            AuditProgressReport report = reportRepository.findById(reportId)
                    .filter(r -> r.getTenantId().equals(tenantId) && r.getEngagementId().equals(engagementId))
                    .orElseThrow(() -> new BusinessException("AUDIT_PROGRESS_REPORT_NOT_FOUND", "Khong tim thay bao cao tien do", HttpStatus.NOT_FOUND));
            if (report.getApprovalStatus() == AssignmentApprovalStatus.APPROVED) {
                continue;
            }

            List<UUID> approverChain = approvalChainResolver.resolveChain(engagement);
            Map<String, Object> variables = new HashMap<>();
            variables.put("progressReportId", report.getId().toString());
            variables.put("approved", true);
            variables.put("approverChain", approverChain.stream().map(UUID::toString).toList());

            var processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(PROCESS_KEY)
                    .tenantId(tenantId.toString())
                    .businessKey(report.getId().toString())
                    .variables(variables)
                    .start();

            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskTenantId(tenantId.toString())
                    .singleResult();
            if (task == null) {
                reportRepository.findById(reportId)
                        .filter(r -> r.getApprovalStatus() == AssignmentApprovalStatus.APPROVED)
                        .ifPresent(r -> approvedIds.add(reportId));
                continue;
            }

            report.setProcessInstanceId(processInstance.getId());
            report.setApprovalStatus(AssignmentApprovalStatus.PENDING);
            reportRepository.save(report);

            workflowTaskService.complete(task.getId(),
                    new CompleteTaskRequest(Map.of("approved", true, "approverUsername", principal.username())), principal);
            approvedIds.add(reportId);
        }

        auditLogService.record("AuditProgressReport", engagementId, AuditAction.APPROVE,
                "Truong doan phe duyet " + approvedIds.size() + " bao cao tien do cua CKT " + engagement.getCode());
        return approvedIds;
    }

    private void requireTeamLead(UUID tenantId, AuditEngagement engagement, String actorEmployeeCode) {
        if (actorEmployeeCode == null) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phe duyet", HttpStatus.FORBIDDEN);
        }
        Employee lead = employeeRepository.findById(engagement.getTeamLeadEmployeeId())
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay truong doan", HttpStatus.NOT_FOUND));
        if (!actorEmployeeCode.equalsIgnoreCase(lead.getEmployeeCode())) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phe duyet", HttpStatus.FORBIDDEN);
        }
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

    private Map<UUID, Employee> employeeMap(UUID employeeId) {
        if (employeeId == null) {
            return Map.of();
        }
        return employeeRepository.findById(employeeId).map(e -> Map.of(e.getId(), e)).orElse(Map.of());
    }

    private AuditProgressReportResponse toResponse(AuditProgressReport report, Map<UUID, AuditMasterDataItem> segments, Map<UUID, Employee> employees) {
        AuditMasterDataItem segment = segments.get(report.getBusinessSegmentId());
        Employee employee = employees.get(report.getReportedEmployeeId());
        return new AuditProgressReportResponse(report.getId(), report.getEngagementId(), report.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), report.getReportedEmployeeId(),
                employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(),
                report.getTotalFindings(), report.getTotalTtss(), report.getTotalMaterialFindings(), report.getTotalMaterialTtss(),
                report.getTotalSamples(), report.getCompletedSamples(), report.getReportDate(), report.getReportRound(),
                report.getReportedByUsername(), report.getNote(), report.getApprovalStatus(), report.getApprovedBy(), report.getApprovedAt());
    }
}
