package com.govia.audit.planengagement.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.approval.AuditWorkApprovalChainResolver;
import com.govia.audit.planengagement.dto.AuditWorkAssignmentApproveRequest;
import com.govia.audit.planengagement.dto.AuditWorkAssignmentStatusUpdateRequest;
import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.workflow.dto.CompleteTaskRequest;
import com.govia.identity.workflow.service.WorkflowTaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Quản lý công việc" (CBKT/THKT) - sheet cung ten trong Tạo CKT (1).xlsx: liet ke cong viec da
 * duoc phan cong (AuditEngagementAssignment) theo giai doan, cho user tu cap nhat trang thai, va
 * cho truong doan phe duyet HANG LOAT qua quy trinh Flowable "audit_workitem_approval" (dây phê
 * duyệt động do {@link AuditWorkApprovalChainResolver} tra cuu - mac dinh 1 buoc = truong doan).
 */
@Service
public class AuditWorkAssignmentService {

    private static final String PROCESS_KEY = "audit_workitem_approval";

    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditWorkItemRepository workItemRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditWorkApprovalChainResolver approvalChainResolver;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WorkflowTaskService workflowTaskService;
    private final AuditLogService auditLogService;

    public AuditWorkAssignmentService(AuditEngagementRepository engagementRepository, AuditEngagementGroupRepository groupRepository,
                                       AuditEngagementGroupMemberRepository memberRepository, AuditEngagementAssignmentRepository assignmentRepository,
                                       AuditWorkItemRepository workItemRepository, AuditMasterDataItemRepository masterDataItemRepository,
                                       EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                                       AuditWorkApprovalChainResolver approvalChainResolver, RuntimeService runtimeService,
                                       TaskService taskService, WorkflowTaskService workflowTaskService, AuditLogService auditLogService) {
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.approvalChainResolver = approvalChainResolver;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.workflowTaskService = workflowTaskService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditWorkManagementItemResponse> list(UUID engagementId, AuditWorkPhase phase, UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);

        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
        if (employeeId != null) {
            members = members.stream().filter(m -> employeeId.equals(m.getEmployeeId())).toList();
        }
        List<UUID> memberIds = members.stream().map(AuditEngagementGroupMember::getId).toList();
        Map<UUID, AuditEngagementGroupMember> membersById = members.stream().collect(Collectors.toMap(AuditEngagementGroupMember::getId, m -> m));

        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId, memberIds);
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));
        Map<UUID, AuditMasterDataItem> segments = masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
        Map<UUID, Employee> employees = employeeRepository.findAllById(members.stream().map(AuditEngagementGroupMember::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, String> usernames = userAccountRepository.findByEmployeeIdIn(new HashSet<>(employees.keySet())).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));

        return assignments.stream()
                .filter(a -> {
                    AuditWorkItem workItem = workItems.get(a.getWorkItemId());
                    return workItem != null && workItem.getPhase() == phase;
                })
                .map(a -> toResponse(a, engagement, membersById.get(a.getGroupMemberId()), workItems, segments, employees, usernames))
                .toList();
    }

    @Transactional
    public AuditWorkManagementItemResponse updateStatus(UUID engagementId, UUID assignmentId, AuditWorkAssignmentStatusUpdateRequest request,
                                                          String actorEmployeeCode) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementAssignment assignment = getAssignmentOrThrow(tenantId, assignmentId);
        AuditEngagementGroupMember member = memberRepository.findById(assignment.getGroupMemberId())
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_MEMBER_NOT_FOUND", "Khong tim thay thanh vien", HttpStatus.NOT_FOUND));
        requireOwnerOrTeamLead(tenantId, engagement, member, actorEmployeeCode);

        assignment.setStatus(request.status());
        assignment.setNote(request.note());
        // Cap nhat lai trang thai cong viec thi coi nhu chua duoc duyet lai (tru khi van la DONE va
        // da APPROVED tu truoc - giu nguyen, tranh mat lich su phe duyet chi vi sua ghi chu).
        if (request.status() != AssignmentStatus.DONE) {
            assignment.setApprovalStatus(null);
            assignment.setApprovedBy(null);
            assignment.setApprovedAt(null);
        }
        assignment = assignmentRepository.save(assignment);

        auditLogService.record("AuditEngagementAssignment", assignmentId, AuditAction.UPDATE,
                "Cap nhat trang thai cong viec: " + request.status());

        AuditEngagementGroup group = groupRepository.findById(member.getGroupId()).orElse(null);
        return toResponse(assignment, engagement, member,
                workItemRepository.findAllById(List.of(assignment.getWorkItemId())).stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w)),
                masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                        .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i)),
                employeeRepository.findById(member.getEmployeeId()).map(e -> Map.of(e.getId(), e)).orElse(Map.of()),
                userAccountRepository.findByEmployeeId(member.getEmployeeId()).map(a -> Map.of(member.getEmployeeId(), a.getUsername())).orElse(Map.of()));
    }

    /**
     * "Phê duyệt" hàng loạt: chỉ trưởng đoàn được gọi, chỉ chấp nhận các dòng đã DONE. Với mỗi
     * dòng: dùng {@link AuditWorkApprovalChainResolver} lấy dây duyệt (mặc định 1 bước = chính
     * trưởng đoàn đang gọi), start quy trình Flowable rồi hoàn tất NGAY task vừa tạo - vẫn đi qua
     * Flowable thật (có lịch sử, có chỗ cho email, sẵn sàng cho dây > 1 bước sau này mà không cần
     * đổi API).
     */
    @Transactional
    public List<UUID> approve(UUID engagementId, AuditWorkAssignmentApproveRequest request, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireTeamLead(tenantId, engagement, principal.employeeCode());

        List<UUID> approvedIds = new ArrayList<>();
        for (UUID assignmentId : request.assignmentIds()) {
            AuditEngagementAssignment assignment = getAssignmentOrThrow(tenantId, assignmentId);
            if (assignment.getStatus() != AssignmentStatus.DONE) {
                throw new BusinessException("AUDIT_WORK_ASSIGNMENT_NOT_DONE",
                        "Chi duoc phe duyet cong viec da hoan thanh", HttpStatus.BAD_REQUEST);
            }
            if (assignment.getApprovalStatus() == AssignmentApprovalStatus.APPROVED) {
                continue;
            }

            List<UUID> approverChain = approvalChainResolver.resolveChain(engagement);
            Map<String, Object> variables = new HashMap<>();
            variables.put("assignmentId", assignment.getId().toString());
            variables.put("approved", true);
            variables.put("approverChain", approverChain.stream().map(UUID::toString).toList());

            var processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(PROCESS_KEY)
                    .tenantId(tenantId.toString())
                    .businessKey(assignment.getId().toString())
                    .variables(variables)
                    .start();

            // Neu approverChain rong (hiem gap - xem AuditWorkApprovalChainResolver), quy trinh da
            // TU KET THUC ngay trong start() o tren (Multi-Instance 0 vong lap) va listener
            // endApproved da luu APPROVED roi - KHONG ghi de lai o day, chi can doc lai de biet.
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskTenantId(tenantId.toString())
                    .singleResult();
            if (task == null) {
                assignmentRepository.findById(assignmentId)
                        .filter(a -> a.getApprovalStatus() == AssignmentApprovalStatus.APPROVED)
                        .ifPresent(a -> approvedIds.add(assignmentId));
                continue;
            }

            assignment.setProcessInstanceId(processInstance.getId());
            assignment.setApprovalStatus(AssignmentApprovalStatus.PENDING);
            assignmentRepository.save(assignment);

            workflowTaskService.complete(task.getId(),
                    new CompleteTaskRequest(Map.of("approved", true, "approverUsername", principal.username())), principal);
            approvedIds.add(assignmentId);
        }

        auditLogService.record("AuditEngagementAssignment", engagementId, AuditAction.APPROVE,
                "Truong doan phe duyet " + approvedIds.size() + " cong viec cua CKT " + engagement.getCode());
        return approvedIds;
    }

    private void requireOwnerOrTeamLead(UUID tenantId, AuditEngagement engagement, AuditEngagementGroupMember member, String actorEmployeeCode) {
        if (actorEmployeeCode == null) {
            throw new BusinessException("AUDIT_WORK_ASSIGNMENT_FORBIDDEN", "Khong xac dinh duoc nguoi dung", HttpStatus.FORBIDDEN);
        }
        Employee owner = employeeRepository.findById(member.getEmployeeId())
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay can bo thuc hien", HttpStatus.NOT_FOUND));
        if (actorEmployeeCode.equalsIgnoreCase(owner.getEmployeeCode())) {
            return;
        }
        Employee lead = employeeRepository.findById(engagement.getTeamLeadEmployeeId())
                .filter(e -> e.getTenantId().equals(tenantId)).orElse(null);
        if (lead != null && actorEmployeeCode.equalsIgnoreCase(lead.getEmployeeCode())) {
            return;
        }
        throw new BusinessException("AUDIT_WORK_ASSIGNMENT_FORBIDDEN",
                "Chi can bo thuc hien hoac truong doan moi duoc cap nhat cong viec nay", HttpStatus.FORBIDDEN);
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

    private AuditEngagementAssignment getAssignmentOrThrow(UUID tenantId, UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_ASSIGNMENT_NOT_FOUND", "Khong tim thay phan cong", HttpStatus.NOT_FOUND));
    }

    private AuditWorkManagementItemResponse toResponse(AuditEngagementAssignment assignment, AuditEngagement engagement, AuditEngagementGroupMember member,
                                                          Map<UUID, AuditWorkItem> workItems, Map<UUID, AuditMasterDataItem> segments,
                                                          Map<UUID, Employee> employees, Map<UUID, String> usernames) {
        AuditWorkItem workItem = workItems.get(assignment.getWorkItemId());
        AuditMasterDataItem segment = workItem == null ? null : segments.get(workItem.getBusinessSegmentId());
        Employee employee = member == null ? null : employees.get(member.getEmployeeId());
        return new AuditWorkManagementItemResponse(
                assignment.getId(), engagement.getId(), engagement.getCode(), engagement.getName(),
                segment == null ? null : segment.getCode(),
                assignment.getWorkItemId(), workItem == null ? null : workItem.getPhase(),
                workItem == null ? null : workItem.getCode(), workItem == null ? null : workItem.getName(),
                member == null ? null : member.getEmployeeId(), employee == null ? null : employee.getEmployeeCode(),
                employee == null ? null : employee.getFullName(), member == null ? null : usernames.get(member.getEmployeeId()),
                assignment.getStatus(), assignment.getNote(), assignment.getApprovalStatus(),
                assignment.getApprovedBy(), assignment.getApprovedAt());
    }
}
