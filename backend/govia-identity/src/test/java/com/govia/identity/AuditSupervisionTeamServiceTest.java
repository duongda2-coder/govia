package com.govia.identity;

import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.service.AuditEmployeeCapabilityService;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionCandidateResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionEvaluationResponse;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionEvaluationRequest;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionTeamRequest;
import com.govia.audit.planengagement.supervisionteam.service.AuditSupervisionTeamService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.entity.UserAccount;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kich ban test cho sheet "To giam sat" (Tao CKT (4).xlsx): chi nguoi tao CKT moi chon duoc to
 * giam sat, thanh vien to giam sat chi tu ghi duoc dong danh gia cua chinh minh (evaluatedAt tu
 * dong cap nhat), va viec duoc chon to giam sat cho phep XEM (khong sua) CKT do o man hinh
 * "Quản lý đợt kiểm toán" du khong phai truong doan/truong nhom/thanh vien.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditSupervisionTeamServiceTest {

    @Autowired
    private AuditSupervisionTeamService supervisionTeamService;
    @Autowired
    private AuditEmployeeCapabilityService capabilityService;
    @Autowired
    private AuditEngagementMonitoringService monitoringService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired
    private AuditEngagementRepository engagementRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("creator-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void saveTeam_onlyCreatorMayChooseMembersAndOnlyCapableEmployeesAreEligible() {
        EmployeeResponse teamLead = createEmployee("NV-SUP-TL-01");
        EmployeeResponse supervisor = createEmployee("NV-SUP-SV-01");
        EmployeeResponse notCapable = createEmployee("NV-SUP-NC-01");
        markSupervisionCapable(supervisor.id(), true);
        markSupervisionCapable(notCapable.id(), false);
        AuditEngagement engagement = createEngagement("CKT-SUP-01", teamLead.id());

        List<AuditSupervisionCandidateResponse> candidates = supervisionTeamService.listCandidates(engagement.getId());
        assertThat(candidates).extracting(AuditSupervisionCandidateResponse::employeeId).contains(supervisor.id());
        assertThat(candidates).extracting(AuditSupervisionCandidateResponse::employeeId).doesNotContain(notCapable.id());
        assertThat(candidates).allMatch(c -> !c.selected());

        CurrentUserPrincipal notCreator = new CurrentUserPrincipal(UUID.randomUUID(), "someone-else", tenantId, teamLead.employeeCode(),
                List.of(), List.of(), UUID.randomUUID().toString());
        assertThatThrownBy(() -> supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(supervisor.id())), notCreator))
                .isInstanceOf(BusinessException.class);

        CurrentUserPrincipal creator = new CurrentUserPrincipal(UUID.randomUUID(), "creator-user", tenantId, teamLead.employeeCode(),
                List.of(), List.of(), UUID.randomUUID().toString());
        assertThatThrownBy(() -> supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(notCapable.id())), creator))
                .isInstanceOf(BusinessException.class);

        supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(supervisor.id())), creator);
        List<AuditSupervisionCandidateResponse> afterSave = supervisionTeamService.listCandidates(engagement.getId());
        assertThat(afterSave.stream().filter(c -> c.employeeId().equals(supervisor.id())).findFirst().orElseThrow().selected()).isTrue();
    }

    @Test
    void myEvaluation_onlyOwnRowIsWritableAndEvaluatedAtAlwaysRefreshes() throws InterruptedException {
        EmployeeResponse teamLead = createEmployee("NV-SUP-TL-02");
        EmployeeResponse supervisor = createEmployee("NV-SUP-SV-02");
        EmployeeResponse outsider = createEmployee("NV-SUP-OUT-02");
        markSupervisionCapable(supervisor.id(), true);
        AuditEngagement engagement = createEngagement("CKT-SUP-02", teamLead.id());

        CurrentUserPrincipal creator = principalFor("creator-user", teamLead.employeeCode());
        supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(supervisor.id())), creator);

        CurrentUserPrincipal supervisorPrincipal = principalFor("sv02", supervisor.employeeCode());
        CurrentUserPrincipal outsiderPrincipal = principalFor("out02", outsider.employeeCode());

        assertThatThrownBy(() -> supervisionTeamService.saveMyEvaluation(engagement.getId(),
                new SaveSupervisionEvaluationRequest(true, true, true, "OK"), outsiderPrincipal))
                .isInstanceOf(BusinessException.class);

        AuditSupervisionEvaluationResponse blank = supervisionTeamService.getMyEvaluation(engagement.getId(), supervisorPrincipal);
        assertThat(blank.progress()).isFalse();
        assertThat(blank.evaluatedAt()).isNull();

        AuditSupervisionEvaluationResponse saved = supervisionTeamService.saveMyEvaluation(engagement.getId(),
                new SaveSupervisionEvaluationRequest(true, true, false, "Dang lam"), supervisorPrincipal);
        assertThat(saved.progress()).isTrue();
        assertThat(saved.qualityAssured()).isFalse();
        assertThat(saved.note()).isEqualTo("Dang lam");
        Instant firstEvaluatedAt = saved.evaluatedAt();
        assertThat(firstEvaluatedAt).isNotNull();

        Thread.sleep(5);
        AuditSupervisionEvaluationResponse resaved = supervisionTeamService.saveMyEvaluation(engagement.getId(),
                new SaveSupervisionEvaluationRequest(true, true, true, "Xong"), supervisorPrincipal);
        assertThat(resaved.qualityAssured()).isTrue();
        assertThat(resaved.evaluatedAt()).isAfter(firstEvaluatedAt);

        List<AuditSupervisionEvaluationResponse> all = supervisionTeamService.listAllEvaluations(engagement.getId(), supervisorPrincipal);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).note()).isEqualTo("Xong");
    }

    @Test
    void listAllEvaluations_includesBlankPlaceholderForMemberWhoHasNotEvaluatedYet() {
        EmployeeResponse teamLead = createEmployee("NV-SUP-TL-03");
        EmployeeResponse svEvaluated = createEmployee("NV-SUP-SV-03A");
        EmployeeResponse svNotYet = createEmployee("NV-SUP-SV-03B");
        markSupervisionCapable(svEvaluated.id(), true);
        markSupervisionCapable(svNotYet.id(), true);
        AuditEngagement engagement = createEngagement("CKT-SUP-03", teamLead.id());

        CurrentUserPrincipal creator = principalFor("creator-user", teamLead.employeeCode());
        supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(svEvaluated.id(), svNotYet.id())), creator);
        CurrentUserPrincipal evaluatedPrincipal = principalFor("sv03a", svEvaluated.employeeCode());
        supervisionTeamService.saveMyEvaluation(engagement.getId(), new SaveSupervisionEvaluationRequest(true, false, false, null), evaluatedPrincipal);

        List<AuditSupervisionEvaluationResponse> all = supervisionTeamService.listAllEvaluations(engagement.getId(), evaluatedPrincipal);
        assertThat(all).hasSize(2);
        AuditSupervisionEvaluationResponse notYetRow = all.stream().filter(r -> r.employeeId().equals(svNotYet.id())).findFirst().orElseThrow();
        assertThat(notYetRow.evaluatedAt()).isNull();
        assertThat(notYetRow.progress()).isFalse();
    }

    @Test
    void supervisionMembership_grantsReadOnlyVisibilityOnEngagementMonitoringWithoutBeingTeamLeadOrGroupMember() {
        EmployeeResponse teamLead = createEmployee("NV-SUP-TL-04");
        EmployeeResponse supervisor = createEmployee("NV-SUP-SV-04");
        markSupervisionCapable(supervisor.id(), true);
        AuditEngagement engagement = createEngagement("CKT-SUP-04", teamLead.id());

        CurrentUserPrincipal creator = principalFor("creator-user", teamLead.employeeCode());
        CurrentUserPrincipal supervisorPrincipal = principalFor("sv04", supervisor.employeeCode());

        List<AuditEngagementMonitoringResponse> before = monitoringService.list(supervisorPrincipal);
        assertThat(before).noneMatch(r -> r.engagement().id().equals(engagement.getId()));

        supervisionTeamService.saveTeam(engagement.getId(), new SaveSupervisionTeamRequest(List.of(supervisor.id())), creator);

        List<AuditEngagementMonitoringResponse> after = monitoringService.list(supervisorPrincipal);
        assertThat(after).anyMatch(r -> r.engagement().id().equals(engagement.getId()));
        assertThat(monitoringService.teamDetail(engagement.getId(), supervisorPrincipal)).isNotNull();

        List<AuditEngagementMonitoringResponse> myEngagements = supervisionTeamService.listMyEngagements(supervisorPrincipal);
        assertThat(myEngagements).anyMatch(r -> r.engagement().id().equals(engagement.getId()));
    }

    private CurrentUserPrincipal principalFor(String username, String employeeCode) {
        return new CurrentUserPrincipal(UUID.randomUUID(), username, tenantId, employeeCode, List.of(), List.of(), UUID.randomUUID().toString());
    }

    private void markSupervisionCapable(UUID employeeId, boolean capable) {
        capabilityService.bulkUpdate(List.of(new AuditEmployeeCapabilityItemRequest(employeeId,
                false, false, false, false, false, false, false, false, false, false, false, false, capable, false)));
    }

    private AuditEngagement createEngagement(String code, UUID teamLeadEmployeeId) {
        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode(code.substring(code.length() - 8));
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

    private EmployeeResponse createEmployee(String code) {
        EmployeeResponse employee = employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
        UserAccount account = new UserAccount();
        account.setTenantId(tenantId);
        account.setEmployeeId(employee.id());
        account.setUsername("u-" + code.toLowerCase());
        account.setPasswordHash("x");
        userAccountRepository.save(account);
        return employee;
    }
}
