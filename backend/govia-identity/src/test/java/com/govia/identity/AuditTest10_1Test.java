package com.govia.identity;

import com.govia.audit.khkt.thang.dto.AuditKhktThangUpdateRequest;
import com.govia.audit.khkt.thang.service.AuditKhktThangService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.tdkp.branch.AuditTdkpBranchDto;
import com.govia.audit.tdkp.branch.AuditTdkpBranchService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** "test 10.1": admin xoa han CKT, chi 1 thang kiem toan/doi tuong, "Loại kiến nghị" TTSS lay tu danh muc theo ma KN nguoi upload,
 * bang sai sot TDKP CN co them Ma KN thanh vien + Loai KN. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditTest10_1Test {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditMasterDataItemRepository masterDataItemRepository;
    @Autowired private AuditObjectUnitRepository unitRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditEngagementRepository engagementRepository;
    @Autowired private AuditEngagementGroupRepository groupRepository;
    @Autowired private AuditEngagementGroupMemberRepository memberRepository;
    @Autowired private AuditRecommendationRepository recommendationRepository;
    @Autowired private AuditTtssRecordRepository ttssRepository;
    @Autowired private AuditTtssService ttssService;
    @Autowired private AuditTdkpBranchService branchService;
    @Autowired private AuditEngagementMonitoringService monitoringService;
    @Autowired private AuditKhktThangService thangService;

    private UUID tenantId;
    private AuditEngagement engagement;
    private UUID recommendationId;

    @BeforeEach
    void setUp() {
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("t101-tester");

        AuditMasterDataItem type = new AuditMasterDataItem();
        type.setTenantId(tenantId);
        type.setCategory(AuditMasterDataCategory.RECOMMENDATION_TYPE);
        type.setCode("KN00");
        type.setName("Kiến nghị chung");
        masterDataItemRepository.save(type);

        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("T101");
        unit.setName("Chi nhánh T101");
        unit.setUnitType("CN");
        unit = unitRepository.save(unit);
        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmployeeCode("T101-E1");
        employee.setFullName("Nguyễn Văn T");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee = employeeRepository.save(employee);

        engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode("CNT101202001");
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2020);
        engagement.setTeamLeadEmployeeId(employee.getId());
        engagement = engagementRepository.save(engagement);

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagement.getId());
        group.setGroupCode("TD");
        group.setLeaderEmployeeId(employee.getId());
        group = groupRepository.save(group);
        AuditEngagementGroupMember member = new AuditEngagementGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(group.getId());
        member.setEmployeeId(employee.getId());
        memberRepository.save(member);

        AuditRecommendation recommendation = new AuditRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setEngagementId(engagement.getId());
        recommendation.setCode("KNKT001");
        recommendation.setContent("Khắc phục sai sót");
        recommendationId = recommendationRepository.save(recommendation).getId();

        AuditTtssRecord record = new AuditTtssRecord();
        record.setTenantId(tenantId);
        record.setEngagementId(engagement.getId());
        record.getTeamRecommendationIds().add(recommendationId);
        record.setRecommendationApprovalStatus(AssignmentApprovalStatus.APPROVED);
        record.setTtssContent("Sai sót 1");
        record.setFindingCode("TS01");
        record.setUploaderRecommendationCode("kn00 ");
        ttssRepository.save(record);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void recommendationTypeIsLookedUpFromCatalogByUploaderCode() {
        List<AuditTtssRecordResponse> rows = ttssService.listByEngagementIds(List.of(engagement.getId()));
        assertThat(rows).singleElement().satisfies(r -> assertThat(r.uploaderRecommendationName()).isEqualTo("Kiến nghị chung"));
    }

    @Test
    void branchDefectsShowMemberRecommendationCodeAndType() {
        branchService.transferFromExecution(2020);
        List<AuditTdkpBranchDto.DefectResponse> defects = branchService.listDefects(null);
        assertThat(defects).singleElement().satisfies(d -> {
            assertThat(d.memberRecommendationCode()).isEqualTo("kn00 ");
            assertThat(d.recommendationTypeName()).isEqualTo("Kiến nghị chung");
        });
    }

    @Test
    void adminDeleteRemovesEngagementWithAllItsData() {
        monitoringService.adminDelete(engagement.getId());
        assertThat(engagementRepository.findById(engagement.getId())).isEmpty();
        assertThat(groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagement.getId())).isEmpty();
        assertThat(ttssRepository.findByTenantIdAndEngagementIdIn(tenantId, List.of(engagement.getId()))).isEmpty();
        assertThat(recommendationRepository.findById(recommendationId)).isEmpty();
    }

    @Test
    void adminDeleteIsBlockedOnceTransferredToTdkp() {
        branchService.transferFromExecution(2020);
        assertThatThrownBy(() -> monitoringService.adminDelete(engagement.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Theo doi khac phuc");
    }

    @Test
    void auditMonthDeclarationAllowsOnlyOneMonth() {
        AuditKhktThangUpdateRequest twoMonths = new AuditKhktThangUpdateRequest(true, false, true, false, false, false, false, false, false, false, false, false, null);
        assertThatThrownBy(() -> thangService.update(2020, "T101", twoMonths))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1 thang");
    }
}
