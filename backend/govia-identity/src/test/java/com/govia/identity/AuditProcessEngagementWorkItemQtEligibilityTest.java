package com.govia.identity;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.processengagement.entity.AuditProcessEngagement;
import com.govia.audit.planengagement.processengagement.repository.AuditProcessEngagementRepository;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditEngagementTeamService;
import com.govia.audit.planengagement.service.AuditWorkAssignmentService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import com.govia.audit.workitemqt.repository.AuditWorkItemQtRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
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

/**
 * Kiem chung fix: "Quan ly cong viec" (CBKT/THKT) cua 1 CKT quy trinh (AuditEngagement co
 * processEngagementId) phai lay cong viec du dieu kien tu Danh muc "Bang ma cong viec quy trinh"
 * (AuditWorkItemQt), KHONG phai Danh muc "Cong viec kiem toan" (AuditWorkItem) nhu CKT chi nhanh -
 * VA phai dung "Ma bo cong viec" (workSetCode) da khai bao luc tao CKT quy trinh cha, xem
 * AuditEngagementTeamService.eligibleWorkItems()/AuditWorkAssignmentService.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditProcessEngagementWorkItemQtEligibilityTest {

    @Autowired
    private AuditEngagementTeamService teamService;
    @Autowired
    private AuditWorkAssignmentService workAssignmentService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired
    private AuditEngagementRepository engagementRepository;
    @Autowired
    private AuditEngagementGroupRepository groupRepository;
    @Autowired
    private AuditEngagementAssignmentRepository assignmentRepository;
    @Autowired
    private AuditWorkItemRepository workItemRepository;
    @Autowired
    private AuditWorkItemQtRepository workItemQtRepository;
    @Autowired
    private AuditProcessEngagementRepository processEngagementRepository;
    @Autowired
    private AuditMasterDataItemRepository masterDataItemRepository;

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
    void addMember_onProcessEngagement_autoAssignsFromWorkItemQtMatchingWorkSetCode_notBaseWorkItem() {
        AuditMasterDataItem segment = createSegment("AM-QT-01");
        EmployeeResponse teamLead = createEmployee("NV-QTE-TL");
        EmployeeResponse worker = createEmployee("NV-QTE-WK");

        AuditProcessEngagement processEngagement = createProcessEngagement(segment.getId(), teamLead.id(), "WSET-01");
        AuditEngagement childEngagement = createChildEngagement("QTAM2601_01", teamLead.id(), processEngagement.getId());

        // Cong viec dung nghiep vu + dung "bo cong viec" -> PHAI duoc tu dong phan cong.
        AuditWorkItemQt matching = createWorkItemQt("CVQT01", AuditWorkPhase.CBKT, segment.getId(), "WSET-01");
        // Cong viec dung nghiep vu nhung KHAC "bo cong viec" -> KHONG duoc phan cong.
        createWorkItemQt("CVQT02", AuditWorkPhase.CBKT, segment.getId(), "WSET-02");
        // Cong viec cung ma/nghiep vu nhung nam trong Danh muc Cong viec kiem toan THUONG -> KHONG duoc phan cong.
        createBaseWorkItem("CVQT01", AuditWorkPhase.CBKT, segment.getId());

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(childEngagement.getId());
        group.setGroupCode(segment.getCode());
        group.setLeaderEmployeeId(teamLead.id());
        group = groupRepository.save(group);

        AuditEngagementGroupMemberResponse member = teamService.addMember(childEngagement.getId(), group.getId(),
                new AuditEngagementGroupMemberRequest(worker.id(), segment.getId(), null, null));

        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(tenantId, member.id());
        assertThat(assignments).hasSize(1);
        AuditEngagementAssignment assignment = assignments.get(0);
        assertThat(assignment.getWorkItemQtId()).isEqualTo(matching.getId());
        assertThat(assignment.getWorkItemId()).isNull();

        // "Quan ly cong viec CBKT" phai hien thi dung ten/ma tu AuditWorkItemQt.
        List<AuditWorkManagementItemResponse> managementRows = workAssignmentService.list(childEngagement.getId(), AuditWorkPhase.CBKT, null,
                principalFor(teamLead));
        assertThat(managementRows).extracting(AuditWorkManagementItemResponse::workItemCode).containsExactly("CVQT01");
        assertThat(managementRows).extracting(AuditWorkManagementItemResponse::workItemName).containsExactly("Cong viec quy trinh CVQT01");
    }

    private AuditMasterDataItem createSegment(String code) {
        AuditMasterDataItem segment = new AuditMasterDataItem();
        segment.setTenantId(tenantId);
        segment.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        segment.setCode(code);
        segment.setName("Nghiep vu " + code);
        segment.setActive(true);
        return masterDataItemRepository.save(segment);
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }

    private AuditProcessEngagement createProcessEngagement(UUID businessSegmentId, UUID teamLeadEmployeeId, String workSetCode) {
        AuditProcessEngagement processEngagement = new AuditProcessEngagement();
        processEngagement.setTenantId(tenantId);
        processEngagement.setCode("QTAM2601");
        processEngagement.setObjectType("QT");
        processEngagement.setBusinessSegmentId(businessSegmentId);
        processEngagement.setYear(2026);
        processEngagement.setExpectedMonth(9);
        processEngagement.setDecisionDate(LocalDate.now());
        processEngagement.setTeamLeadEmployeeId(teamLeadEmployeeId);
        processEngagement.setDecisionNumber("QD-QTAM2601");
        processEngagement.setWorkSetCode(workSetCode);
        return processEngagementRepository.save(processEngagement);
    }

    private AuditEngagement createChildEngagement(String code, UUID teamLeadEmployeeId, UUID processEngagementId) {
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
        engagement.setProcessEngagementId(processEngagementId);
        return engagementRepository.save(engagement);
    }

    private AuditWorkItemQt createWorkItemQt(String code, AuditWorkPhase phase, UUID businessSegmentId, String workSetCode) {
        AuditWorkItemQt workItem = new AuditWorkItemQt();
        workItem.setTenantId(tenantId);
        workItem.setPhase(phase);
        workItem.setBusinessSegmentId(businessSegmentId);
        workItem.setCode(code);
        workItem.setName("Cong viec quy trinh " + code);
        workItem.setWorkSetCode(workSetCode);
        workItem.setActive(true);
        return workItemQtRepository.save(workItem);
    }

    private AuditWorkItem createBaseWorkItem(String code, AuditWorkPhase phase, UUID businessSegmentId) {
        AuditWorkItem workItem = new AuditWorkItem();
        workItem.setTenantId(tenantId);
        workItem.setPhase(phase);
        workItem.setBusinessSegmentId(businessSegmentId);
        workItem.setCode(code);
        workItem.setName("Cong viec thuong " + code);
        workItem.setActive(true);
        return workItemRepository.save(workItem);
    }

    private CurrentUserPrincipal principalFor(EmployeeResponse employee) {
        return new CurrentUserPrincipal(UUID.randomUUID(), "principal-" + employee.employeeCode(), tenantId, employee.employeeCode(), List.of(), List.of(),
                UUID.randomUUID().toString());
    }
}
