package com.govia.audit.planengagement.service;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.dto.AssignWorkItemsRequest;
import com.govia.audit.planengagement.dto.AuditEngagementAssignmentResponse;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.dto.AuditEngagementGroupRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupResponse;
import com.govia.audit.planengagement.dto.EligibleWorkItemResponse;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/** "Danh sach nhom" + "Danh sach thanh vien" + "Phan cong nghiep vu cho thanh vien" - sheet
 * "quan ly DKT" cua Tao CKT.xlsx. */
@Service
public class AuditEngagementTeamService {

    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditWorkItemRepository workItemRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditEmployeeCapabilityRepository employeeCapabilityRepository;
    private final AuditLogService auditLogService;

    public AuditEngagementTeamService(AuditEngagementRepository engagementRepository, AuditEngagementGroupRepository groupRepository,
                                       AuditEngagementGroupMemberRepository memberRepository, AuditEngagementAssignmentRepository assignmentRepository,
                                       AuditWorkItemRepository workItemRepository, AuditMasterDataItemRepository masterDataItemRepository,
                                       EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                                       AuditEmployeeCapabilityRepository employeeCapabilityRepository, AuditLogService auditLogService) {
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeCapabilityRepository = employeeCapabilityRepository;
        this.auditLogService = auditLogService;
    }

    // ===================== Nhom =====================

    @Transactional(readOnly = true)
    public List<AuditEngagementGroupResponse> listGroups(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        Map<UUID, Employee> leaders = employeesById(groups.stream().map(AuditEngagementGroup::getLeaderEmployeeId).toList());
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groups.stream().map(AuditEngagementGroup::getId).toList());
        Map<UUID, Long> memberCountByGroup = members.stream().collect(Collectors.groupingBy(AuditEngagementGroupMember::getGroupId, Collectors.counting()));
        Map<UUID, Long> assignmentCountByGroup = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId, members.stream().map(AuditEngagementGroupMember::getId).toList())
                .stream().collect(Collectors.groupingBy(a -> memberGroupId(members, a.getGroupMemberId()), Collectors.counting()));
        return groups.stream().map(g -> toGroupResponse(g, engagement, leaders, memberCountByGroup, assignmentCountByGroup)).toList();
    }

    @Transactional
    public AuditEngagementGroupResponse addGroup(UUID engagementId, AuditEngagementGroupRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        if (groupRepository.findByTenantIdAndAuditEngagementIdAndGroupCode(tenantId, engagementId, request.groupCode()).isPresent()) {
            throw new BusinessException("AUDIT_ENGAGEMENT_GROUP_DUPLICATE", "Nhom nay da ton tai trong cuoc kiem toan");
        }
        Employee leader = getEmployeeOrThrow(tenantId, request.leaderEmployeeId());
        boolean truongNhomCapable = employeeCapabilityRepository.findByTenantIdAndEmployeeId(tenantId, leader.getId())
                .map(AuditEmployeeCapability::isTruongNhomCapable).orElse(false);
        if (!truongNhomCapable) {
            throw new BusinessException("EMPLOYEE_NOT_TEAM_LEAD_CAPABLE", "Nhan vien nay khong co kha nang dam nhiem truong nhom");
        }

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagementId);
        group.setGroupCode(request.groupCode());
        group.setLeaderEmployeeId(request.leaderEmployeeId());
        group = groupRepository.save(group);

        auditLogService.record("AuditEngagementGroup", group.getId(), AuditAction.CREATE, "Them nhom " + request.groupCode() + " cho CKT: " + engagement.getCode());
        return toGroupResponse(group, engagement, employeesById(List.of(leader.getId())), Map.of(), Map.of());
    }

    @Transactional
    public void deleteGroup(UUID engagementId, UUID groupId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        if (memberRepository.countByGroupId(groupId) > 0) {
            throw new BusinessException("AUDIT_ENGAGEMENT_GROUP_HAS_MEMBERS", "Chi duoc xoa nhom khi khong co thanh vien");
        }
        groupRepository.delete(group);
        auditLogService.record("AuditEngagementGroup", groupId, AuditAction.DELETE, "Xoa nhom " + group.getGroupCode());
    }

    // ===================== Thanh vien =====================

    @Transactional(readOnly = true)
    public List<AuditEngagementGroupMemberResponse> listMembers(UUID engagementId, UUID groupId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdOrderByCreatedAtAsc(tenantId, groupId);
        return toMemberResponses(members, engagement, group);
    }

    @Transactional(readOnly = true)
    public List<AuditEngagementGroupMemberResponse> listMembersByEngagement(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        Map<UUID, AuditEngagementGroup> groupsById = groups.stream().collect(Collectors.toMap(AuditEngagementGroup::getId, g -> g));
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groups.stream().map(AuditEngagementGroup::getId).toList());
        List<AuditEngagementGroupMemberResponse> result = new ArrayList<>();
        for (AuditEngagementGroupMember member : members) {
            result.addAll(toMemberResponses(List.of(member), engagement, groupsById.get(member.getGroupId())));
        }
        return result;
    }

    @Transactional
    public AuditEngagementGroupMemberResponse addMember(UUID engagementId, UUID groupId, AuditEngagementGroupMemberRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        getEmployeeOrThrow(tenantId, request.employeeId());
        if (memberRepository.findByTenantIdAndGroupIdAndEmployeeId(tenantId, groupId, request.employeeId()).isPresent()) {
            throw new BusinessException("AUDIT_ENGAGEMENT_MEMBER_DUPLICATE", "Nhan vien nay da co trong nhom");
        }

        AuditEngagementGroupMember member = new AuditEngagementGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(groupId);
        applyMemberRequest(member, request);
        member = memberRepository.save(member);

        int autoAssigned = autoAssignWorkItems(tenantId, member);

        auditLogService.record("AuditEngagementGroupMember", member.getId(), AuditAction.CREATE,
                "Them thanh vien cho nhom " + group.getGroupCode() + " CKT " + engagement.getCode() + " - tu dong phan cong " + autoAssigned + " cong viec");
        return toMemberResponses(List.of(member), engagement, group).get(0);
    }

    /** "Thay doi can bo" - doi nguoi/nghiep vu cua 1 vi tri thanh vien, tinh lai phan cong tu dong. */
    @Transactional
    public AuditEngagementGroupMemberResponse updateMember(UUID engagementId, UUID groupId, UUID memberId, AuditEngagementGroupMemberRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        AuditEngagementGroupMember member = getMemberOrThrow(tenantId, groupId, memberId);
        getEmployeeOrThrow(tenantId, request.employeeId());
        memberRepository.findByTenantIdAndGroupIdAndEmployeeId(tenantId, groupId, request.employeeId())
                .filter(existing -> !existing.getId().equals(memberId))
                .ifPresent(existing -> { throw new BusinessException("AUDIT_ENGAGEMENT_MEMBER_DUPLICATE", "Nhan vien nay da co trong nhom"); });

        assignmentRepository.deleteByGroupMemberId(memberId);
        applyMemberRequest(member, request);
        member = memberRepository.save(member);
        int autoAssigned = autoAssignWorkItems(tenantId, member);

        auditLogService.record("AuditEngagementGroupMember", member.getId(), AuditAction.UPDATE,
                "Thay doi can bo cho nhom " + group.getGroupCode() + " - tu dong phan cong lai " + autoAssigned + " cong viec");
        return toMemberResponses(List.of(member), engagement, group).get(0);
    }

    @Transactional
    public void deleteMember(UUID engagementId, UUID groupId, UUID memberId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        getGroupOrThrow(tenantId, engagementId, groupId);
        AuditEngagementGroupMember member = getMemberOrThrow(tenantId, groupId, memberId);
        assignmentRepository.deleteByGroupMemberId(memberId);
        memberRepository.delete(member);
        auditLogService.record("AuditEngagementGroupMember", memberId, AuditAction.DELETE, "Xoa thanh vien khoi nhom");
    }

    // ===================== Phan cong =====================

    @Transactional(readOnly = true)
    public List<EligibleWorkItemResponse> listEligibleWorkItems(UUID engagementId, UUID groupId, UUID memberId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        getGroupOrThrow(tenantId, engagementId, groupId);
        AuditEngagementGroupMember member = getMemberOrThrow(tenantId, groupId, memberId);
        List<AuditWorkItem> eligible = eligibleWorkItems(tenantId, member);
        Set<UUID> assignedIds = assignmentRepository.findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(tenantId, memberId)
                .stream().map(AuditEngagementAssignment::getWorkItemId).collect(Collectors.toSet());
        return eligible.stream().filter(w -> !assignedIds.contains(w.getId()))
                .map(w -> new EligibleWorkItemResponse(w.getId(), w.getPhase(), w.getCode(), w.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEngagementAssignmentResponse> listAssignments(UUID engagementId, UUID groupId, UUID memberId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        AuditEngagementGroupMember member = getMemberOrThrow(tenantId, groupId, memberId);
        return toAssignmentResponses(assignmentRepository.findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(tenantId, memberId), member, group, engagement);
    }

    /** Nut "Chon cong viec" - trung doan them thu cong tu tap "du dieu kien" (giong pool tu dong). */
    @Transactional
    public List<AuditEngagementAssignmentResponse> assignWorkItems(UUID engagementId, UUID groupId, UUID memberId, AssignWorkItemsRequest request, String actorEmployeeCode) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireTeamLead(tenantId, engagement, actorEmployeeCode);
        AuditEngagementGroup group = getGroupOrThrow(tenantId, engagementId, groupId);
        AuditEngagementGroupMember member = getMemberOrThrow(tenantId, groupId, memberId);

        Set<UUID> eligibleIds = eligibleWorkItems(tenantId, member).stream().map(AuditWorkItem::getId).collect(Collectors.toSet());
        for (UUID workItemId : request.workItemIds()) {
            if (!eligibleIds.contains(workItemId)) {
                throw new BusinessException("AUDIT_ENGAGEMENT_WORK_ITEM_NOT_ELIGIBLE", "Cong viec khong thuoc nghiep vu cua thanh vien nay");
            }
            if (!assignmentRepository.existsByGroupMemberIdAndWorkItemId(memberId, workItemId)) {
                AuditEngagementAssignment assignment = new AuditEngagementAssignment();
                assignment.setTenantId(tenantId);
                assignment.setGroupMemberId(memberId);
                assignment.setWorkItemId(workItemId);
                assignmentRepository.save(assignment);
            }
        }

        auditLogService.record("AuditEngagementAssignment", memberId, AuditAction.CREATE,
                "Chon them " + request.workItemIds().size() + " cong viec cho thanh vien");
        return toAssignmentResponses(assignmentRepository.findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(tenantId, memberId), member, group, engagement);
    }

    @Transactional
    public void deleteAssignment(UUID engagementId, UUID groupId, UUID memberId, UUID assignmentId, String actorEmployeeCode) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireTeamLead(tenantId, engagement, actorEmployeeCode);
        getGroupOrThrow(tenantId, engagementId, groupId);
        getMemberOrThrow(tenantId, groupId, memberId);
        AuditEngagementAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getTenantId().equals(tenantId) && a.getGroupMemberId().equals(memberId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_ASSIGNMENT_NOT_FOUND", "Khong tim thay phan cong", HttpStatus.NOT_FOUND));
        assignmentRepository.delete(assignment);
        auditLogService.record("AuditEngagementAssignment", assignmentId, AuditAction.DELETE, "Bo phan cong cong viec");
    }

    // ===================== Helpers =====================

    private int autoAssignWorkItems(UUID tenantId, AuditEngagementGroupMember member) {
        List<AuditWorkItem> eligible = eligibleWorkItems(tenantId, member);
        int count = 0;
        for (AuditWorkItem workItem : eligible) {
            if (!assignmentRepository.existsByGroupMemberIdAndWorkItemId(member.getId(), workItem.getId())) {
                AuditEngagementAssignment assignment = new AuditEngagementAssignment();
                assignment.setTenantId(tenantId);
                assignment.setGroupMemberId(member.getId());
                assignment.setWorkItemId(workItem.getId());
                assignmentRepository.save(assignment);
                count++;
            }
        }
        return count;
    }

    private List<AuditWorkItem> eligibleWorkItems(UUID tenantId, AuditEngagementGroupMember member) {
        // Luu y: List.of(...) nem NPE ngay khi co phan tu null (khong doi den filter) - phai dung
        // Stream.of(...) de cho phep null truoc khi loc, vi nghiep vu 2/3 thuong bo trong.
        List<UUID> segmentIds = java.util.stream.Stream.of(member.getBusinessSegment1Id(), member.getBusinessSegment2Id(), member.getBusinessSegment3Id())
                .filter(java.util.Objects::nonNull).toList();
        if (segmentIds.isEmpty()) {
            return List.of();
        }
        return workItemRepository.findByTenantIdAndActiveTrueAndBusinessSegmentIdIn(tenantId, segmentIds);
    }

    private void requireTeamLead(UUID tenantId, AuditEngagement engagement, String actorEmployeeCode) {
        if (actorEmployeeCode == null) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phan cong", HttpStatus.FORBIDDEN);
        }
        Employee lead = employeeRepository.findById(engagement.getTeamLeadEmployeeId())
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay truong doan", HttpStatus.NOT_FOUND));
        if (!actorEmployeeCode.equalsIgnoreCase(lead.getEmployeeCode())) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phan cong", HttpStatus.FORBIDDEN);
        }
    }

    private void applyMemberRequest(AuditEngagementGroupMember member, AuditEngagementGroupMemberRequest request) {
        member.setEmployeeId(request.employeeId());
        member.setBusinessSegment1Id(request.businessSegment1Id());
        member.setBusinessSegment2Id(request.businessSegment2Id());
        member.setBusinessSegment3Id(request.businessSegment3Id());
    }

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private AuditEngagementGroup getGroupOrThrow(UUID tenantId, UUID engagementId, UUID groupId) {
        return groupRepository.findById(groupId)
                .filter(g -> g.getTenantId().equals(tenantId) && g.getAuditEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_GROUP_NOT_FOUND", "Khong tim thay nhom", HttpStatus.NOT_FOUND));
    }

    private AuditEngagementGroupMember getMemberOrThrow(UUID tenantId, UUID groupId, UUID memberId) {
        return memberRepository.findById(memberId)
                .filter(m -> m.getTenantId().equals(tenantId) && m.getGroupId().equals(groupId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_MEMBER_NOT_FOUND", "Khong tim thay thanh vien", HttpStatus.NOT_FOUND));
    }

    private Employee getEmployeeOrThrow(UUID tenantId, UUID id) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
    }

    private UUID memberGroupId(List<AuditEngagementGroupMember> members, UUID memberId) {
        return members.stream().filter(m -> m.getId().equals(memberId)).map(AuditEngagementGroupMember::getGroupId).findFirst().orElse(null);
    }

    private Map<UUID, Employee> employeesById(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        return employeeRepository.findAllById(unique).stream().collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private AuditEngagementGroupResponse toGroupResponse(AuditEngagementGroup group, AuditEngagement engagement, Map<UUID, Employee> leaders,
                                                           Map<UUID, Long> memberCountByGroup, Map<UUID, Long> assignmentCountByGroup) {
        Employee leader = leaders.get(group.getLeaderEmployeeId());
        return new AuditEngagementGroupResponse(group.getId(), group.getAuditEngagementId(), engagement.getCode(), group.getGroupCode(),
                group.getGroupCode().name(), group.getLeaderEmployeeId(), leader == null ? null : leader.getEmployeeCode(),
                leader == null ? null : leader.getFullName(), memberCountByGroup.getOrDefault(group.getId(), 0L),
                assignmentCountByGroup.getOrDefault(group.getId(), 0L));
    }

    private List<AuditEngagementGroupMemberResponse> toMemberResponses(List<AuditEngagementGroupMember> members, AuditEngagement engagement, AuditEngagementGroup group) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, Employee> employees = employeesById(members.stream().map(AuditEngagementGroupMember::getEmployeeId).toList());
        Employee leader = employeeRepository.findById(group.getLeaderEmployeeId()).orElse(null);
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, String> usernames = userAccountRepository.findByEmployeeIdIn(employees.keySet()).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
        return members.stream().map(member -> {
            Employee employee = employees.get(member.getEmployeeId());
            return new AuditEngagementGroupMemberResponse(member.getId(), member.getGroupId(), group.getGroupCode(), group.getGroupCode().name(),
                    engagement.getId(), engagement.getCode(), member.getEmployeeId(), employee == null ? null : employee.getEmployeeCode(),
                    employee == null ? null : employee.getFullName(), employee == null || employee.getOrgUnit() == null ? null : employee.getOrgUnit().getName(),
                    usernames.get(member.getEmployeeId()), group.getLeaderEmployeeId(), leader == null ? null : leader.getFullName(),
                    member.getBusinessSegment1Id(), codeOf(segments.get(member.getBusinessSegment1Id())),
                    member.getBusinessSegment2Id(), codeOf(segments.get(member.getBusinessSegment2Id())),
                    member.getBusinessSegment3Id(), codeOf(segments.get(member.getBusinessSegment3Id())));
        }).toList();
    }

    private List<AuditEngagementAssignmentResponse> toAssignmentResponses(List<AuditEngagementAssignment> assignments, AuditEngagementGroupMember member,
                                                                            AuditEngagementGroup group, AuditEngagement engagement) {
        Employee employee = employeeRepository.findById(member.getEmployeeId()).orElse(null);
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(engagement.getTenantId());
        return assignments.stream().map(a -> {
            AuditWorkItem workItem = workItems.get(a.getWorkItemId());
            AuditMasterDataItem segment = workItem == null ? null : segments.get(workItem.getBusinessSegmentId());
            return new AuditEngagementAssignmentResponse(a.getId(), member.getId(), group.getId(), group.getGroupCode().name(),
                    member.getEmployeeId(), employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(),
                    a.getWorkItemId(), workItem == null ? null : workItem.getPhase(), segment == null ? null : segment.getCode(),
                    workItem == null ? null : workItem.getCode(), workItem == null ? null : workItem.getName());
        }).toList();
    }

    private String codeOf(AuditMasterDataItem item) {
        return item == null ? null : item.getCode();
    }
}
