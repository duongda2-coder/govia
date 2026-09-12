package com.govia.identity;

import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementTeamMemberDetailResponse;
import com.govia.audit.planengagement.monitoring.dto.ProgressStat;
import com.govia.audit.planengagement.monitoring.dto.TeamMemberScoringRequest;
import com.govia.audit.planengagement.monitoring.dto.TeamRankingUpdateRequest;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
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
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiem chung man hinh "Quản lý đợt kiểm toán" (AuditEngagementMonitoringService): so lieu tong
 * hop dung cong thuc da chon (xem Javadoc cua AuditEngagementMonitoringResponse/
 * AuditEngagementTeamMemberDetailResponse) va phan quyen scope theo tham gia doan.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditEngagementMonitoringServiceTest {

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
    private AuditEngagementGroupRepository groupRepository;
    @Autowired
    private AuditEngagementGroupMemberRepository memberRepository;
    @Autowired
    private AuditWorkItemRepository workItemRepository;
    @Autowired
    private AuditEngagementAssignmentRepository assignmentRepository;
    @Autowired
    private AuditTtssRecordRepository ttssRecordRepository;
    @Autowired
    private AuditRecommendationRepository recommendationRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @Test
    void teamDetail_computesPerMemberAggregatesAndRoleTitles() {
        EmployeeResponse teamLead = createEmployee("NV-MON-TL-01");
        EmployeeResponse groupLeader = createEmployee("NV-MON-GL-01");
        EmployeeResponse member1 = createEmployee("NV-MON-M1-01");

        AuditEngagement engagement = createEngagement("CKT-MON-01", teamLead.id());
        AuditEngagementGroup group = createGroup(engagement, groupLeader.id());
        AuditEngagementGroupMember mTeamLead = addMember(group, teamLead.id());
        AuditEngagementGroupMember mGroupLeader = addMember(group, groupLeader.id());
        AuditEngagementGroupMember mMember = addMember(group, member1.id());

        AuditWorkItem wCbkt = createWorkItem("CB01", AuditWorkPhase.CBKT, false);
        AuditWorkItem wThktSample = createWorkItem("TH01", AuditWorkPhase.THKT, true);
        AuditWorkItem wThktNoSample = createWorkItem("TH02", AuditWorkPhase.THKT, false);
        createAssignment(mMember.getId(), wCbkt.getId(), AssignmentStatus.DONE);
        createAssignment(mMember.getId(), wThktSample.getId(), AssignmentStatus.NOT_STARTED);
        createAssignment(mMember.getId(), wThktNoSample.getId(), AssignmentStatus.DONE);

        UUID recommendationId = createRecommendation(engagement.getId(), "KNKT001").getId();
        AuditTtssRecord r1 = newTtssRecord(engagement.getId(), member1.fullName(), "TT001", true);
        r1.setTeamRecommendationId(recommendationId);
        AuditTtssRecord r2 = newTtssRecord(engagement.getId(), member1.fullName(), "TT001", true); // ma trung -> khong tang so LOAI phan biet
        AuditTtssRecord r3 = newTtssRecord(engagement.getId(), member1.fullName(), "TT002", false);
        AuditTtssRecord r4 = newTtssRecord(engagement.getId(), "Nguoi khong thuoc doan", "TT999", true); // khong khop ten thanh vien nao
        ttssRecordRepository.saveAll(List.of(r1, r2, r3, r4));

        CurrentUserPrincipal viewAllPrincipal = principalWithViewAll();
        List<AuditEngagementTeamMemberDetailResponse> detail = monitoringService.teamDetail(engagement.getId(), viewAllPrincipal);

        assertThat(detail).hasSize(3);
        AuditEngagementTeamMemberDetailResponse leadRow = findByMemberId(detail, mTeamLead.getId());
        assertThat(leadRow.roleTitle()).isEqualTo("Trưởng đoàn");
        AuditEngagementTeamMemberDetailResponse groupLeaderRow = findByMemberId(detail, mGroupLeader.getId());
        assertThat(groupLeaderRow.roleTitle()).isEqualTo("Trưởng nhóm");
        AuditEngagementTeamMemberDetailResponse memberRow = findByMemberId(detail, mMember.getId());
        assertThat(memberRow.roleTitle()).isEqualTo("Thành viên");

        // r1+r2+r3 khop ten member1 (r4 khong khop ai) -> 3 dong, 2 ma phan biet (TT001,TT002)
        assertThat(memberRow.totalFindings()).isEqualTo(3);
        assertThat(memberRow.ttssTypeCount()).isEqualTo(2);
        assertThat(memberRow.totalMaterialFindings()).isEqualTo(2); // r1,r2 material=true
        assertThat(memberRow.materialTtssTypeCount()).isEqualTo(1); // chi TT001 la ma trong yeu phan biet
        assertThat(memberRow.recommendationCount()).isEqualTo(1); // chi r1 co teamRecommendationId

        assertThat(memberRow.cbktProgress()).isEqualTo(new ProgressStat(1, 1));
        assertThat(memberRow.thktSampleProgress()).isEqualTo(new ProgressStat(0, 1));
        assertThat(memberRow.thktNoSampleProgress()).isEqualTo(new ProgressStat(1, 1));

        // Thanh vien khac khong co assignment -> tien do 0/0
        assertThat(leadRow.cbktProgress()).isEqualTo(new ProgressStat(0, 0));
    }

    @Test
    void list_aggregatesAcrossEngagementIncludingUnattributedTtss() {
        EmployeeResponse teamLead = createEmployee("NV-MON-TL-02");
        EmployeeResponse member1 = createEmployee("NV-MON-M1-02");
        AuditEngagement engagement = createEngagement("CKT-MON-02", teamLead.id());
        AuditEngagementGroup group = createGroup(engagement, teamLead.id());
        addMember(group, teamLead.id());
        addMember(group, member1.id());

        AuditTtssRecord r1 = newTtssRecord(engagement.getId(), member1.fullName(), "TT001", true);
        r1.setTeamRecommendationId(createRecommendation(engagement.getId(), "KNKT001").getId());
        AuditTtssRecord r2 = newTtssRecord(engagement.getId(), "Nguoi la", "TT777", false); // khong khop thanh vien nao nhung VAN tinh vao tong CKT
        ttssRecordRepository.saveAll(List.of(r1, r2));

        List<AuditEngagementMonitoringResponse> list = monitoringService.list(principalWithViewAll());
        AuditEngagementMonitoringResponse row = list.stream().filter(r -> r.engagement().id().equals(engagement.getId())).findFirst().orElseThrow();

        assertThat(row.memberCount()).isEqualTo(2);
        assertThat(row.totalFindings()).isEqualTo(2); // ca r1 va r2, khac voi teamDetail (chi tinh theo tung nguoi)
        assertThat(row.totalMaterialFindings()).isEqualTo(1);
        assertThat(row.recommendationCount()).isEqualTo(1);
    }

    @Test
    void scoping_nonParticipantWithoutViewAllSeesNothingAndIsForbiddenFromDetail() {
        EmployeeResponse teamLead = createEmployee("NV-MON-TL-03");
        EmployeeResponse outsider = createEmployee("NV-MON-OUT-03");
        AuditEngagement engagement = createEngagement("CKT-MON-03", teamLead.id());

        CurrentUserPrincipal outsiderPrincipal = new CurrentUserPrincipal(UUID.randomUUID(), "outsider03", tenantId, outsider.employeeCode(),
                List.of(), List.of(), UUID.randomUUID().toString());
        CurrentUserPrincipal teamLeadPrincipal = new CurrentUserPrincipal(UUID.randomUUID(), "tl03", tenantId, teamLead.employeeCode(),
                List.of(), List.of(), UUID.randomUUID().toString());

        List<AuditEngagementMonitoringResponse> outsiderList = monitoringService.list(outsiderPrincipal);
        assertThat(outsiderList).noneMatch(r -> r.engagement().id().equals(engagement.getId()));

        List<AuditEngagementMonitoringResponse> teamLeadList = monitoringService.list(teamLeadPrincipal);
        assertThat(teamLeadList).anyMatch(r -> r.engagement().id().equals(engagement.getId()));

        assertThatThrownBy(() -> monitoringService.teamDetail(engagement.getId(), outsiderPrincipal))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateTeamRankingAndMemberScoring_persistUserEnteredFields() {
        EmployeeResponse teamLead = createEmployee("NV-MON-TL-04");
        EmployeeResponse member1 = createEmployee("NV-MON-M1-04");
        AuditEngagement engagement = createEngagement("CKT-MON-04", teamLead.id());
        AuditEngagementGroup group = createGroup(engagement, teamLead.id());
        AuditEngagementGroupMember member = addMember(group, member1.id());

        AuditEngagementMonitoringResponse afterRanking = monitoringService.updateTeamRanking(engagement.getId(), new TeamRankingUpdateRequest("Tốt"));
        assertThat(afterRanking.engagement().teamRanking()).isEqualTo("Tốt");

        AuditEngagementTeamMemberDetailResponse afterScoring = monitoringService.updateTeamMemberScoring(engagement.getId(), member.getId(),
                new TeamMemberScoringRequest(new BigDecimal("8.50"), "Khá", "Hoàn thành đúng hạn"));
        assertThat(afterScoring.score()).isEqualByComparingTo("8.50");
        assertThat(afterScoring.ranking()).isEqualTo("Khá");
        assertThat(afterScoring.note()).isEqualTo("Hoàn thành đúng hạn");
    }

    private AuditEngagementTeamMemberDetailResponse findByMemberId(List<AuditEngagementTeamMemberDetailResponse> rows, UUID memberId) {
        return rows.stream().filter(r -> r.memberId().equals(memberId)).findFirst().orElseThrow();
    }

    private CurrentUserPrincipal principalWithViewAll() {
        return new CurrentUserPrincipal(UUID.randomUUID(), "monitor-viewer", tenantId, null,
                List.of(), List.of("AUDIT.PLAN_ENGAGEMENT.VIEW_ALL"), UUID.randomUUID().toString());
    }

    private AuditRecommendation createRecommendation(UUID engagementId, String code) {
        AuditRecommendation recommendation = new AuditRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setEngagementId(engagementId);
        recommendation.setCode(code);
        recommendation.setContent("Noi dung " + code);
        return recommendationRepository.save(recommendation);
    }

    private AuditTtssRecord newTtssRecord(UUID engagementId, String ttssPerformerName, String findingCode, boolean material) {
        AuditTtssRecord record = new AuditTtssRecord();
        record.setTenantId(tenantId);
        record.setEngagementId(engagementId);
        record.setTtssPerformerName(ttssPerformerName);
        record.setFindingCode(findingCode);
        record.setMaterial(material);
        return record;
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

    private AuditEngagementGroup createGroup(AuditEngagement engagement, UUID leaderEmployeeId) {
        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagement.getId());
        group.setGroupCode("TINDUNG");
        group.setLeaderEmployeeId(leaderEmployeeId);
        return groupRepository.save(group);
    }

    private AuditEngagementGroupMember addMember(AuditEngagementGroup group, UUID employeeId) {
        AuditEngagementGroupMember member = new AuditEngagementGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(group.getId());
        member.setEmployeeId(employeeId);
        return memberRepository.save(member);
    }

    private AuditWorkItem createWorkItem(String code, AuditWorkPhase phase, boolean hasSampleSelection) {
        AuditWorkItem workItem = new AuditWorkItem();
        workItem.setTenantId(tenantId);
        workItem.setPhase(phase);
        workItem.setCode(code);
        workItem.setName("Cong viec " + code);
        workItem.setActive(true);
        workItem.setHasSampleSelection(hasSampleSelection);
        return workItemRepository.save(workItem);
    }

    private AuditEngagementAssignment createAssignment(UUID groupMemberId, UUID workItemId, AssignmentStatus status) {
        AuditEngagementAssignment assignment = new AuditEngagementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setGroupMemberId(groupMemberId);
        assignment.setWorkItemId(workItemId);
        assignment.setStatus(status);
        return assignmentRepository.save(assignment);
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }
}
