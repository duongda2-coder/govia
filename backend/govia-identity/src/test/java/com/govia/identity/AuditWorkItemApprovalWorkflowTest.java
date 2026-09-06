package com.govia.identity;

import com.govia.audit.planengagement.dto.AuditWorkAssignmentApproveRequest;
import com.govia.audit.planengagement.dto.AuditWorkAssignmentStatusUpdateRequest;
import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditWorkAssignmentService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.entity.UserStatus;
import com.govia.identity.notification.NotificationOutboxRepository;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiem chung quy trinh Flowable "audit_workitem_approval" (xem audit-workitem-approval.bpmn20.xml)
 * gan vao "Quan ly cong viec" CBKT/THKT (AuditWorkAssignmentService): can bo thuc hien cap nhat
 * trang thai DONE, truong doan phe duyet hang loat -> approvalStatus phai chuyen APPROVED va co
 * 1 dong moi trong notification_outbox (gui mail dang tat, xem LoggingWorkflowNotificationService).
 * Test o tang service (khong qua MockMvc/JWT that) - dung TenantContext + CurrentUserPrincipal thu
 * cong, giong AuditEmployeeCapabilityServiceTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditWorkItemApprovalWorkflowTest {

    @Autowired
    private AuditWorkAssignmentService workAssignmentService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired
    private AuditEngagementRepository engagementRepository;
    @Autowired
    private AuditEngagementGroupRepository groupRepository;
    @Autowired
    private AuditEngagementGroupMemberRepository memberRepository;
    @Autowired
    private AuditEngagementAssignmentRepository assignmentRepository;
    @Autowired
    private AuditWorkItemRepository workItemRepository;
    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void teamLeadApprove_flipsAssignmentToApproved_andQueuesNotification() {
        EmployeeResponse teamLead = createEmployee("NV-TL-01");
        UUID teamLeadAccountId = createUserAccount(teamLead.id(), "tl01");
        EmployeeResponse worker = createEmployee("NV-WK-01");

        AuditEngagement engagement = createEngagement("CKT-TEST-01", teamLead.id());
        AuditEngagementGroupMember member = createGroupWithMember(engagement, teamLead.id(), worker.id());
        AuditWorkItem workItem = createWorkItem("CNA0101", AuditWorkPhase.CBKT);
        AuditEngagementAssignment assignment = createAssignment(member.getId(), workItem.getId());

        workAssignmentService.updateStatus(engagement.getId(), assignment.getId(),
                new AuditWorkAssignmentStatusUpdateRequest(AssignmentStatus.DONE, "Da lam xong"), worker.employeeCode());

        List<UUID> approvedIds = workAssignmentService.approve(engagement.getId(),
                new AuditWorkAssignmentApproveRequest(List.of(assignment.getId())), principalFor(teamLeadAccountId, "tl01", teamLead.employeeCode()));

        assertThat(approvedIds).containsExactly(assignment.getId());

        AuditEngagementAssignment reloaded = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(reloaded.getApprovalStatus()).isEqualTo(AssignmentApprovalStatus.APPROVED);
        assertThat(reloaded.getApprovedBy()).isEqualTo("tl01");
        assertThat(reloaded.getApprovedAt()).isNotNull();
        assertThat(reloaded.getProcessInstanceId()).isNotNull();

        assertThat(notificationOutboxRepository.findAll())
                .anySatisfy(n -> assertThat(n.getRecipientUserId()).isEqualTo(teamLeadAccountId.toString()));
    }

    @Test
    void approve_rejectedWhenAssignmentNotDone() {
        EmployeeResponse teamLead = createEmployee("NV-TL-02");
        UUID teamLeadAccountId = createUserAccount(teamLead.id(), "tl02");
        EmployeeResponse worker = createEmployee("NV-WK-02");

        AuditEngagement engagement = createEngagement("CKT-TEST-02", teamLead.id());
        AuditEngagementGroupMember member = createGroupWithMember(engagement, teamLead.id(), worker.id());
        AuditWorkItem workItem = createWorkItem("CNA0102", AuditWorkPhase.CBKT);
        AuditEngagementAssignment assignment = createAssignment(member.getId(), workItem.getId());
        // Khong goi updateStatus - assignment con nguyen NOT_STARTED.

        CurrentUserPrincipal teamLeadPrincipal = principalFor(teamLeadAccountId, "tl02", teamLead.employeeCode());
        AuditWorkAssignmentApproveRequest request = new AuditWorkAssignmentApproveRequest(List.of(assignment.getId()));

        assertThatThrownBy(() -> workAssignmentService.approve(engagement.getId(), request, teamLeadPrincipal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da hoan thanh");
    }

    @Test
    void approve_forbiddenWhenActorIsNotTeamLead() {
        EmployeeResponse teamLead = createEmployee("NV-TL-03");
        createUserAccount(teamLead.id(), "tl03");
        EmployeeResponse worker = createEmployee("NV-WK-03");
        UUID workerAccountId = createUserAccount(worker.id(), "wk03");

        AuditEngagement engagement = createEngagement("CKT-TEST-03", teamLead.id());
        AuditEngagementGroupMember member = createGroupWithMember(engagement, teamLead.id(), worker.id());
        AuditWorkItem workItem = createWorkItem("CNA0103", AuditWorkPhase.CBKT);
        AuditEngagementAssignment assignment = createAssignment(member.getId(), workItem.getId());

        workAssignmentService.updateStatus(engagement.getId(), assignment.getId(),
                new AuditWorkAssignmentStatusUpdateRequest(AssignmentStatus.DONE, null), worker.employeeCode());

        CurrentUserPrincipal workerPrincipal = principalFor(workerAccountId, "wk03", worker.employeeCode());
        AuditWorkAssignmentApproveRequest request = new AuditWorkAssignmentApproveRequest(List.of(assignment.getId()));

        assertThatThrownBy(() -> workAssignmentService.approve(engagement.getId(), request, workerPrincipal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("truong doan");
    }

    /** Phan quyen theo dong (xem AuditWorkAssignmentService.resolveVisibleEmployeeIds()): thanh vien
     * chi thay cong viec cua chinh minh, truong nhom thay ca nhom minh phu trach (KHONG lan sang
     * nhom khac), truong doan thay tat ca - cung quy tac voi AuditTtssService. */
    @Test
    void list_scopesByGroupLeadershipInEngagement() {
        EmployeeResponse teamLead = createEmployee("NV-WM-TL");
        EmployeeResponse groupLeadA = createEmployee("NV-WM-GLA");
        EmployeeResponse memberA = createEmployee("NV-WM-MA");
        EmployeeResponse groupLeadB = createEmployee("NV-WM-GLB");
        EmployeeResponse memberB = createEmployee("NV-WM-MB");

        AuditEngagement engagement = createEngagement("CKT-WM-VIS-01", teamLead.id());

        AuditEngagementGroup groupA = new AuditEngagementGroup();
        groupA.setTenantId(tenantId);
        groupA.setAuditEngagementId(engagement.getId());
        groupA.setGroupCode(AuditEngagementGroupCode.TINDUNG);
        groupA.setLeaderEmployeeId(groupLeadA.id());
        groupA = groupRepository.save(groupA);
        AuditEngagementGroupMember memberAInGroup = new AuditEngagementGroupMember();
        memberAInGroup.setTenantId(tenantId);
        memberAInGroup.setGroupId(groupA.getId());
        memberAInGroup.setEmployeeId(memberA.id());
        memberAInGroup = memberRepository.save(memberAInGroup);

        AuditEngagementGroup groupB = new AuditEngagementGroup();
        groupB.setTenantId(tenantId);
        groupB.setAuditEngagementId(engagement.getId());
        groupB.setGroupCode(AuditEngagementGroupCode.NTINDUNG);
        groupB.setLeaderEmployeeId(groupLeadB.id());
        groupB = groupRepository.save(groupB);
        AuditEngagementGroupMember memberBInGroup = new AuditEngagementGroupMember();
        memberBInGroup.setTenantId(tenantId);
        memberBInGroup.setGroupId(groupB.getId());
        memberBInGroup.setEmployeeId(memberB.id());
        memberBInGroup = memberRepository.save(memberBInGroup);

        AuditWorkItem workItemA = createWorkItem("WM-VIS-A", AuditWorkPhase.THKT);
        createAssignment(memberAInGroup.getId(), workItemA.getId());
        AuditWorkItem workItemB = createWorkItem("WM-VIS-B", AuditWorkPhase.THKT);
        createAssignment(memberBInGroup.getId(), workItemB.getId());

        // Thanh vien A: chi thay cong viec cua chinh minh.
        List<AuditWorkManagementItemResponse> memberAView = workAssignmentService.list(engagement.getId(), AuditWorkPhase.THKT, null,
                principalFor(UUID.randomUUID(), "wmma", memberA.employeeCode()));
        assertThat(memberAView).extracting(AuditWorkManagementItemResponse::workItemCode).containsExactly("WM-VIS-A");

        // Truong nhom A: thay ca nhom A, KHONG thay nhom B.
        List<AuditWorkManagementItemResponse> groupLeadAView = workAssignmentService.list(engagement.getId(), AuditWorkPhase.THKT, null,
                principalFor(UUID.randomUUID(), "wmgla", groupLeadA.employeeCode()));
        assertThat(groupLeadAView).extracting(AuditWorkManagementItemResponse::workItemCode).containsExactly("WM-VIS-A");

        // Truong doan: thay tat ca.
        List<AuditWorkManagementItemResponse> teamLeadView = workAssignmentService.list(engagement.getId(), AuditWorkPhase.THKT, null,
                principalFor(UUID.randomUUID(), "wmtl", teamLead.employeeCode()));
        assertThat(teamLeadView).extracting(AuditWorkManagementItemResponse::workItemCode)
                .containsExactlyInAnyOrder("WM-VIS-A", "WM-VIS-B");
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }

    private UUID createUserAccount(UUID employeeId, String username) {
        UserAccount account = new UserAccount();
        account.setTenantId(tenantId);
        account.setEmployeeId(employeeId);
        account.setUsername(username);
        account.setPasswordHash("{noop}not-used-in-this-test");
        account.setStatus(UserStatus.ACTIVE);
        return userAccountRepository.save(account).getId();
    }

    private AuditEngagement createEngagement(String code, UUID teamLeadEmployeeId) {
        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode(code.substring(code.length() - 8)); // risk_score_audit_object_unit.code la VARCHAR(10)
        unit.setName("Chi nhanh test " + code);
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);

        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode(code);
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(9);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setTeamLeadEmployeeId(teamLeadEmployeeId);
        engagement.setDecisionNumber("QD-" + code);
        return engagementRepository.save(engagement);
    }

    private AuditEngagementGroupMember createGroupWithMember(AuditEngagement engagement, UUID leaderEmployeeId, UUID workerEmployeeId) {
        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagement.getId());
        group.setGroupCode(AuditEngagementGroupCode.TINDUNG);
        group.setLeaderEmployeeId(leaderEmployeeId);
        group = groupRepository.save(group);

        AuditEngagementGroupMember member = new AuditEngagementGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(group.getId());
        member.setEmployeeId(workerEmployeeId);
        return memberRepository.save(member);
    }

    private AuditWorkItem createWorkItem(String code, AuditWorkPhase phase) {
        AuditWorkItem workItem = new AuditWorkItem();
        workItem.setTenantId(tenantId);
        workItem.setPhase(phase);
        workItem.setCode(code);
        workItem.setName("Cong viec " + code);
        workItem.setActive(true);
        return workItemRepository.save(workItem);
    }

    private AuditEngagementAssignment createAssignment(UUID groupMemberId, UUID workItemId) {
        AuditEngagementAssignment assignment = new AuditEngagementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setGroupMemberId(groupMemberId);
        assignment.setWorkItemId(workItemId);
        return assignmentRepository.save(assignment);
    }

    private CurrentUserPrincipal principalFor(UUID userId, String username, String employeeCode) {
        return new CurrentUserPrincipal(userId, username, tenantId, employeeCode, List.of(), List.of(), UUID.randomUUID().toString());
    }
}
