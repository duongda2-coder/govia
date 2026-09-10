package com.govia.identity;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportApproveRequest;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportResponse;
import com.govia.audit.planengagement.progressreport.repository.AuditProgressReportRepository;
import com.govia.audit.planengagement.progressreport.service.AuditProgressReportService;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.dto.AuditTtssApproveRecommendationsRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssLinkRecommendationRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
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
 * Kiem chung Khoi B (Bao cao tien do) va Khoi C (TTSS + Kien nghi): dung lai đúng dây phê duyệt
 * động (AuditWorkApprovalChainResolver) va notification hook cua Khoi A, chi khac entity duoc duyet
 * - xem AuditProgressReportService.approve/AuditTtssService.approveRecommendations. Test o tang
 * service (khong qua MockMvc/JWT that), giong AuditWorkItemApprovalWorkflowTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditTtssAndProgressReportWorkflowTest {

    @Autowired
    private AuditTtssService ttssService;
    @Autowired
    private AuditRecommendationService recommendationService;
    @Autowired
    private AuditProgressReportService progressReportService;
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
    private AuditTtssRecordRepository ttssRecordRepository;
    @Autowired
    private AuditProgressReportRepository progressReportRepository;
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
    void recordUpload_computesAggregatesAndTeamLeadApproveFlipsToApproved() {
        EmployeeResponse teamLead = createEmployee("NV-B-TL-01");
        UUID teamLeadAccountId = createUserAccount(teamLead.id(), "btl01");
        EmployeeResponse worker = createEmployee("NV-B-WK-01");
        AuditEngagement engagement = createEngagement("CKT-B-01", teamLead.id());
        createGroupWithMember(engagement, teamLead.id(), worker.id());

        AuditTtssRecord r1 = newTtssRecord(engagement.getId(), "TT001", true);
        AuditTtssRecord r2 = newTtssRecord(engagement.getId(), "TT001", true); // ma trung -> khong tang so luong TTSS/trong yeu phan biet
        AuditTtssRecord r3 = newTtssRecord(engagement.getId(), "TT002", false);

        AuditProgressReportResponse report = progressReportService.recordUpload(engagement.getId(), worker.id(), null,
                List.of(r1, r2, r3), "Bao cao dot 1", "worker01", null);

        assertThat(report.totalFindings()).isEqualTo(3); // 3 dong co ma TTSS
        assertThat(report.totalTtss()).isEqualTo(2); // TT001 + TT002, bo trung
        assertThat(report.totalMaterialFindings()).isEqualTo(2); // r1, r2 trong yeu
        assertThat(report.totalMaterialTtss()).isEqualTo(1); // chi TT001 trong yeu, bo trung
        assertThat(report.reportRound()).isEqualTo(1);
        assertThat(report.approvalStatus()).isNull();

        List<UUID> approvedIds = progressReportService.approve(engagement.getId(),
                new AuditProgressReportApproveRequest(List.of(report.id())),
                principalFor(teamLeadAccountId, "btl01", teamLead.employeeCode()));

        assertThat(approvedIds).containsExactly(report.id());
        assertThat(progressReportRepository.findById(report.id()).orElseThrow().getApprovalStatus())
                .isEqualTo(AssignmentApprovalStatus.APPROVED);
        assertThat(notificationOutboxRepository.findAll())
                .anySatisfy(n -> assertThat(n.getRecipientUserId()).isEqualTo(teamLeadAccountId.toString()));

        // Upload lan 2 CUNG NGAY cho cung nguoi/mang NV van chi tinh la Lan bao cao 1.
        AuditProgressReportResponse secondReportSameDay = progressReportService.recordUpload(engagement.getId(), worker.id(), null,
                List.of(r3), "Bao cao dot 2 cung ngay", "worker01", null);
        assertThat(secondReportSameDay.reportRound()).isEqualTo(1);

        // Gia lap bao cao dau tien la CUA NGAY HOM QUA -> upload hom nay phai sang Lan bao cao 2.
        var firstReportEntity = progressReportRepository.findById(report.id()).orElseThrow();
        firstReportEntity.setReportDate(LocalDate.now().minusDays(1));
        progressReportRepository.save(firstReportEntity);
        var secondReportSameDayEntity = progressReportRepository.findById(secondReportSameDay.id()).orElseThrow();
        secondReportSameDayEntity.setReportDate(LocalDate.now().minusDays(1));
        progressReportRepository.save(secondReportSameDayEntity);

        AuditProgressReportResponse nextDayReport = progressReportService.recordUpload(engagement.getId(), worker.id(), null,
                List.of(r3), "Bao cao ngay hom sau", "worker01", null);
        assertThat(nextDayReport.reportRound()).isEqualTo(2);
    }

    @Test
    void linkThenApproveRecommendation_flipsTtssRecordToApproved() {
        EmployeeResponse teamLead = createEmployee("NV-C-TL-01");
        UUID teamLeadAccountId = createUserAccount(teamLead.id(), "ctl01");
        AuditEngagement engagement = createEngagement("CKT-C-01", teamLead.id());

        AuditRecommendationResponse defaultRecommendation = recommendationService.list(engagement.getId()).get(0);
        assertThat(defaultRecommendation.code()).isEqualTo("KNKT000");
        AuditRecommendationResponse created = recommendationService.create(engagement.getId(),
                new AuditRecommendationRequest(null, "Thieu chu ky phe duyet"));
        assertThat(created.code()).isEqualTo("KNKT001");

        AuditTtssRecord record = ttssRecordRepository.save(newTtssRecord(engagement.getId(), "TT100", true));

        ttssService.linkRecommendation(engagement.getId(), new AuditTtssLinkRecommendationRequest(List.of(record.getId()), created.id()));
        assertThat(ttssRecordRepository.findById(record.getId()).orElseThrow().getTeamRecommendationId()).isEqualTo(created.id());

        List<UUID> approvedIds = ttssService.approveRecommendations(engagement.getId(),
                new AuditTtssApproveRecommendationsRequest(List.of(record.getId())),
                principalFor(teamLeadAccountId, "ctl01", teamLead.employeeCode()));

        assertThat(approvedIds).containsExactly(record.getId());
        AuditTtssRecord reloaded = ttssRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(reloaded.getRecommendationApprovalStatus()).isEqualTo(AssignmentApprovalStatus.APPROVED);
        assertThat(reloaded.getRecommendationApprovedBy()).isEqualTo("ctl01");
        assertThat(notificationOutboxRepository.findAll())
                .anySatisfy(n -> assertThat(n.getRecipientUserId()).isEqualTo(teamLeadAccountId.toString()));
    }

    @Test
    void deleteRecommendation_blockedForDefaultAndInUseOtherwiseRemoved() {
        EmployeeResponse teamLead = createEmployee("NV-C-TL-03");
        AuditEngagement engagement = createEngagement("CKT-C-03", teamLead.id());

        AuditRecommendationResponse defaultRecommendation = recommendationService.list(engagement.getId()).get(0);
        assertThatThrownBy(() -> recommendationService.delete(engagement.getId(), defaultRecommendation.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mac dinh");

        AuditRecommendationResponse inUse = recommendationService.create(engagement.getId(), new AuditRecommendationRequest(null, "Dang duoc gan"));
        AuditTtssRecord record = ttssRecordRepository.save(newTtssRecord(engagement.getId(), "TT300", false));
        ttssService.linkRecommendation(engagement.getId(), new AuditTtssLinkRecommendationRequest(List.of(record.getId()), inUse.id()));
        assertThatThrownBy(() -> recommendationService.delete(engagement.getId(), inUse.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dang duoc gan");

        AuditRecommendationResponse unused = recommendationService.create(engagement.getId(), new AuditRecommendationRequest(null, "Chua dung"));
        recommendationService.delete(engagement.getId(), unused.id());
        assertThat(recommendationService.list(engagement.getId())).extracting(AuditRecommendationResponse::id).doesNotContain(unused.id());
    }

    /** Ma khong con do nguoi dung tu nhap - server tu sinh tiep theo sau ma lon nhat da co (ke ca
     * KNKT000 mac dinh), luon tang dan va khong bao gio trung (xem generateNextCode()). */
    @Test
    void createRecommendation_autoGeneratesSequentialCode() {
        EmployeeResponse teamLead = createEmployee("NV-C-TL-04");
        AuditEngagement engagement = createEngagement("CKT-C-04", teamLead.id());

        AuditRecommendationResponse first = recommendationService.create(engagement.getId(), new AuditRecommendationRequest(null, "Ban dau"));
        assertThat(first.code()).isEqualTo("KNKT001");

        AuditRecommendationResponse second = recommendationService.create(engagement.getId(), new AuditRecommendationRequest(null, "Tiep theo"));
        assertThat(second.code()).isEqualTo("KNKT002");
    }

    @Test
    void approveRecommendation_rejectedWhenNotLinked() {
        EmployeeResponse teamLead = createEmployee("NV-C-TL-02");
        UUID teamLeadAccountId = createUserAccount(teamLead.id(), "ctl02");
        AuditEngagement engagement = createEngagement("CKT-C-02", teamLead.id());
        AuditTtssRecord record = ttssRecordRepository.save(newTtssRecord(engagement.getId(), "TT200", false));

        CurrentUserPrincipal teamLeadPrincipal = principalFor(teamLeadAccountId, "ctl02", teamLead.employeeCode());
        AuditTtssApproveRecommendationsRequest request = new AuditTtssApproveRecommendationsRequest(List.of(record.getId()));

        assertThatThrownBy(() -> ttssService.approveRecommendations(engagement.getId(), request, teamLeadPrincipal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("gan kien nghi");
    }

    /** Phan quyen theo dong (recordUsername): thanh vien chi thay dong cua chinh minh, truong nhom
     * thay dong cua CA nhom minh phu trach (nhung KHONG thay nhom khac), truong doan thay tat ca -
     * ap dung dong nhat cho list()/delete()/approveRecommendations() (xem AuditTtssService.
     * resolveVisibility()). */
    @Test
    void ttssVisibility_scopesListAndDeleteByGroupLeadershipInEngagement() {
        EmployeeResponse teamLead = createEmployee("NV-VIS-TL");
        EmployeeResponse groupLeadA = createEmployee("NV-VIS-GLA");
        EmployeeResponse memberA = createEmployee("NV-VIS-MA");
        EmployeeResponse groupLeadB = createEmployee("NV-VIS-GLB");
        EmployeeResponse memberB = createEmployee("NV-VIS-MB");

        UUID glaAccountId = createUserAccount(groupLeadA.id(), "visgla");
        UUID maAccountId = createUserAccount(memberA.id(), "visma");
        UUID mbAccountId = createUserAccount(memberB.id(), "vismb");

        AuditEngagement engagement = createEngagement("CKT-VIS-01", teamLead.id());

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
        memberRepository.save(memberAInGroup);

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
        memberRepository.save(memberBInGroup);

        AuditTtssRecord recordA = newTtssRecord(engagement.getId(), "TT-VIS-A", false);
        recordA.setRecordUsername("visma");
        recordA = ttssRecordRepository.save(recordA);

        AuditTtssRecord recordGroupLead = newTtssRecord(engagement.getId(), "TT-VIS-GLA", false);
        recordGroupLead.setRecordUsername("visgla");
        recordGroupLead = ttssRecordRepository.save(recordGroupLead);

        AuditTtssRecord recordB = newTtssRecord(engagement.getId(), "TT-VIS-B", false);
        recordB.setRecordUsername("vismb");
        recordB = ttssRecordRepository.save(recordB);

        // Thanh vien A: chi thay dong cua chinh minh.
        List<AuditTtssRecordResponse> memberAView = ttssService.list(engagement.getId(), principalFor(maAccountId, "visma", memberA.employeeCode()));
        assertThat(memberAView).extracting(AuditTtssRecordResponse::findingCode).containsExactly("TT-VIS-A");

        // Truong nhom A: thay dong cua chinh minh + thanh vien trong nhom A, KHONG thay nhom B.
        CurrentUserPrincipal groupLeadAPrincipal = principalFor(glaAccountId, "visgla", groupLeadA.employeeCode());
        List<AuditTtssRecordResponse> groupLeadAView = ttssService.list(engagement.getId(), groupLeadAPrincipal);
        assertThat(groupLeadAView).extracting(AuditTtssRecordResponse::findingCode)
                .containsExactlyInAnyOrder("TT-VIS-A", "TT-VIS-GLA");

        // Truong doan: thay tat ca nghiep vu.
        List<AuditTtssRecordResponse> teamLeadView = ttssService.list(engagement.getId(),
                principalFor(UUID.randomUUID(), "vistl", teamLead.employeeCode()));
        assertThat(teamLeadView).extracting(AuditTtssRecordResponse::findingCode)
                .containsExactlyInAnyOrder("TT-VIS-A", "TT-VIS-GLA", "TT-VIS-B");

        // Truong nhom A KHONG the xoa dong cua thanh vien nhom B (ngoai pham vi phu trach).
        UUID recordBId = recordB.getId();
        assertThatThrownBy(() -> ttssService.delete(engagement.getId(), recordBId, groupLeadAPrincipal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khong co quyen");

        // Nhung XOA duoc dong cua thanh vien trong nhom minh.
        UUID recordAId = recordA.getId();
        ttssService.delete(engagement.getId(), recordAId, groupLeadAPrincipal);
        assertThat(ttssRecordRepository.findById(recordAId)).isEmpty();
    }

    private AuditTtssRecord newTtssRecord(UUID engagementId, String findingCode, boolean material) {
        AuditTtssRecord record = new AuditTtssRecord();
        record.setTenantId(tenantId);
        record.setEngagementId(engagementId);
        record.setFindingCode(findingCode);
        record.setMaterial(material);
        return record;
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

    private CurrentUserPrincipal principalFor(UUID userId, String username, String employeeCode) {
        return new CurrentUserPrincipal(userId, username, tenantId, employeeCode, List.of(), List.of(), UUID.randomUUID().toString());
    }
}
