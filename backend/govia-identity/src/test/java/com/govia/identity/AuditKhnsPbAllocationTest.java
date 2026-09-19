package com.govia.identity;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamUpdateRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbAllocationResult;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsPosition;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamObjectRepository;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamRepository;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsNamService;
import com.govia.audit.khkt.khnsnam.service.AuditKhnsPbService;
import com.govia.audit.khkt.scale.entity.AuditKhktScale;
import com.govia.audit.khkt.scale.repository.AuditKhktScaleRepository;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedSegmentRepository;
import com.govia.audit.khkt.thang.dto.AuditKhktThangRowResponse;
import com.govia.audit.khkt.thang.entity.AuditKhktThang;
import com.govia.audit.khkt.thang.repository.AuditKhktThangRepository;
import com.govia.audit.khkt.thang.service.AuditKhktThangService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeAuditorClassification;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kiem chung test18.9: (1) KHKT_THANG chi liet ke doi tuong da phe duyet o TH2; (2) nut "Phan bo
 * nhan su" o KHNS_PB phan bo ca nam tu cac thang da khai bao - chay tren H2 in-memory (Liquibase that),
 * khong dung DB dev. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditKhnsPbAllocationTest {

    private static final int YEAR = 2099;

    @Autowired private AuditKhktThangService thangService;
    @Autowired private AuditKhnsPbService pbService;
    @Autowired private AuditKhnsNamService khnsNamService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditMasterDataItemRepository masterDataItemRepository;
    @Autowired private AuditKhktScaleRepository scaleRepository;
    @Autowired private AuditKhktThConfirmedRepository confirmedRepository;
    @Autowired private AuditKhktThConfirmedSegmentRepository confirmedSegmentRepository;
    @Autowired private AuditKhktThangRepository thangRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditEmployeeCapabilityRepository capabilityRepository;
    @Autowired private AuditKhnsNamRepository khnsNamRepository;
    @Autowired private AuditKhnsNamObjectRepository khnsNamObjectRepository;

    private UUID tenantId;
    private UUID lnId;
    private UUID gaId;

    @BeforeEach
    void setUp() {
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
        scaleRepository.deleteAll();
        lnId = segment("LN");
        gaId = segment("GA");
        scale(1, "5000", "Quy mo 1");
        scale(2, "10000", "Quy mo 2");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void thangListsOnlyApprovedObjects() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, "12000", Set.of(3), lnId, gaId);
        object("OBJ-PENDING", AuditKhktApprovalStatus.PENDING, true, "12000", Set.of(3), gaId);
        object("OBJ-NOT-SELECTED", AuditKhktApprovalStatus.APPROVED, false, "12000", Set.of(3), gaId);

        List<AuditKhktThangRowResponse> rows = thangService.list(YEAR);

        assertThat(rows).extracting(AuditKhktThangRowResponse::auditObjectCode).containsExactly("OBJ-A");
    }

    @Test
    void allocateAssignsWholeYearByRulesAndOverwritesPreviousAllocation() {
        // OBJ-A: tin dung quy mo lon (3 KTV bac>=2) + TCKT, thang 3. OBJ-D: chi TCKT, thang 3 va 4.
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, "12000", Set.of(3), lnId, gaId);
        object("OBJ-D", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(3, 4), gaId);
        object("OBJ-PENDING", AuditKhktApprovalStatus.PENDING, true, null, Set.of(3), gaId);

        Employee e1 = employee("E1", EmployeeAuditorClassification.TYPE_3, true, false, "LN");
        Employee e2 = employee("E2", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        Employee e3 = employee("E3", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        Employee e6 = employee("E6", EmployeeAuditorClassification.TYPE_3, false, false, "LN");
        Employee lowGrade = employee("E-LOW", EmployeeAuditorClassification.TYPE_1, false, false, "LN");
        Employee e4 = employee("E4", EmployeeAuditorClassification.TYPE_1, true, false, "GA");
        Employee e5 = employee("E5", EmployeeAuditorClassification.TYPE_1, false, false, "GA");
        Employee e7 = employee("E7", EmployeeAuditorClassification.TYPE_1, false, false, "GA");
        Employee onLeave = employee("E-LEAVE", EmployeeAuditorClassification.TYPE_3, false, true, "LN");
        Employee related = employee("E-REL", EmployeeAuditorClassification.TYPE_3, false, false, "LN");
        related.setRelatedPersonBranches("OBJ-A");
        employeeRepository.save(related);

        // phan bo cu (thu cong) cua nguoi dang nghi -> phai bi xoa khi phan bo lai ca nam
        AuditKhnsNam stale = new AuditKhnsNam();
        stale.setTenantId(tenantId);
        stale.setYear(YEAR);
        stale.setEmployeeId(onLeave.getId());
        stale.setMonth5AuditObjectCode("OBJ-OLD");
        khnsNamRepository.save(stale);

        AuditKhnsPbAllocationResult result = pbService.allocate(YEAR);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.objectCount()).isEqualTo(2);
        assertThat(result.fullyStaffedCount()).isEqualTo(2);

        // OBJ-A (thang 3): Truong doan E4 + 3 tin dung bac>=2 (E2, E3, E6) + 1 TCKT (E5/E7); khong co E-LOW/E-LEAVE/E-REL
        List<UUID> teamA = teamOf("OBJ-A", 3);
        assertThat(teamA).hasSize(5).contains(e4.getId(), e2.getId(), e3.getId(), e6.getId());
        assertThat(teamA).doesNotContain(lowGrade.getId(), onLeave.getId(), related.getId());
        // OBJ-D (2 thang) do E1 (Truong doan) + 1 TCKT - cung 1 doan ca 2 thang
        List<UUID> teamD3 = teamOf("OBJ-D", 3);
        assertThat(teamD3).hasSize(2).contains(e1.getId());
        assertThat(teamOf("OBJ-D", 4)).containsExactlyInAnyOrderElementsOf(teamD3);

        assertThat(plan(e4).getRoleInTeam()).isEqualTo(AuditKhnsRoleInTeam.TEAM_LEAD);
        assertThat(plan(e1).getRoleInTeam()).isEqualTo(AuditKhnsRoleInTeam.TEAM_LEAD);
        assertThat(plan(e2).getRoleInTeam()).isIn(AuditKhnsRoleInTeam.MEMBER, AuditKhnsRoleInTeam.GROUP_LEAD);
        assertThat(objectCodes(plan(e4))).containsExactly("OBJ-A");
        assertThat(objectCodes(plan(e1))).containsExactly("OBJ-D");
        AuditKhnsNamObject leadObject = khnsNamObjectRepository.findByTenantIdAndKhnsNamIdIn(tenantId, List.of(plan(e4).getId())).get(0);
        assertThat(AuditKhnsPosition.parse(leadObject.getPositions()))
                .containsExactlyInAnyOrder(AuditKhnsPosition.TEAM_LEAD, AuditKhnsPosition.QTDH_GROUP_LEAD, AuditKhnsPosition.QTDH_MEMBER);

        assertThat(khnsNamRepository.findByTenantIdAndYearAndEmployeeId(tenantId, YEAR, lowGrade.getId())).isEmpty();
        assertThat(khnsNamRepository.findByTenantIdAndYearAndEmployeeId(tenantId, YEAR, related.getId())).isEmpty();
        assertThat(plan(onLeave).getMonth5AuditObjectCode()).isNull();
        assertThat(result.employeesAssigned()).isEqualTo(7);
        assertThat(java.util.List.of(e5, e7)).allSatisfy(e -> assertThat(khnsNamRepository.findByTenantIdAndYearAndEmployeeId(tenantId, YEAR, e.getId())).isPresent());

        // chay lai: van du dinh bien (khong nhan doi phan bo)
        AuditKhnsPbAllocationResult again = pbService.allocate(YEAR);
        assertThat(again.warnings()).isEmpty();
        assertThat(teamOf("OBJ-A", 3)).hasSize(5);
    }

    @Test
    void listRowsReturnsOneRowPerAllocatedEmployeeAndUnitWithPositionsSegmentsAndMonths() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, "12000", Set.of(3), lnId, gaId);
        object("OBJ-D", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(3, 4), gaId);
        employee("E1", EmployeeAuditorClassification.TYPE_3, true, false, "LN");
        employee("E2", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        employee("E3", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        employee("E6", EmployeeAuditorClassification.TYPE_3, false, false, "LN");
        employee("E4", EmployeeAuditorClassification.TYPE_1, true, false, "GA");
        employee("E5", EmployeeAuditorClassification.TYPE_1, false, false, "GA");
        employee("E7", EmployeeAuditorClassification.TYPE_1, false, false, "GA");
        employee("E8-UNUSED", EmployeeAuditorClassification.TYPE_1, false, false);
        pbService.allocate(YEAR);

        List<AuditKhnsPbRowResponse> rows = pbService.listRows(YEAR);

        // OBJ-A: Truong doan + 3 tin dung + 1 TCKT = 5 dong; OBJ-D: Truong doan + 1 TCKT = 2 dong (moi nguoi 1 dong, nhieu thang)
        assertThat(rows).hasSize(7);
        assertThat(rows).extracting(AuditKhnsPbRowResponse::auditObjectCode).containsExactly("OBJ-A", "OBJ-A", "OBJ-A", "OBJ-A", "OBJ-A", "OBJ-D", "OBJ-D");

        // sap xep theo don vi, Truong doan truoc; Truong doan giu 3 chuc vu, nghiep vu QTDH
        AuditKhnsPbRowResponse a = rows.get(0);
        assertThat(a.roleInTeam()).isEqualTo(AuditKhnsRoleInTeam.TEAM_LEAD);
        assertThat(a.positions()).containsExactlyInAnyOrder("TEAM_LEAD", "QTDH_GROUP_LEAD", "QTDH_MEMBER");
        assertThat(a.segmentCodes()).containsExactly("CE");
        assertThat(a.auditObjectName()).isEqualTo("OBJ-A");
        assertThat(a.businessSegmentCodes()).containsExactlyInAnyOrder("LN", "GA");
        assertThat(a.creditScale()).isEqualTo(2);
        assertThat(a.months()).containsExactly(3);

        List<AuditKhnsPbRowResponse> creditRows = rows.subList(1, 5).stream().filter(r -> r.segmentCodes().contains("LN")).toList();
        assertThat(creditRows).hasSize(3);
        assertThat(creditRows).allSatisfy(r -> assertThat(r.positions()).contains("TD_MEMBER").doesNotContain("NTD_MEMBER", "TEAM_LEAD"));
        assertThat(creditRows.stream().filter(r -> r.positions().contains("TD_GROUP_LEAD"))).hasSize(1);
        // nguoi TCKT duy nhat cua don vi la Truong nhom NTD dong thoi la thanh vien NTD
        AuditKhnsPbRowResponse ntd = rows.subList(1, 5).stream().filter(r -> r.segmentCodes().contains("GA")).findFirst().orElseThrow();
        assertThat(ntd.positions()).containsExactlyInAnyOrder("NTD_GROUP_LEAD", "NTD_MEMBER");
        assertThat(ntd.segmentCodes()).containsExactly("GA");

        AuditKhnsPbRowResponse d = rows.get(5);
        assertThat(d.roleInTeam()).isEqualTo(AuditKhnsRoleInTeam.TEAM_LEAD);
        assertThat(d.months()).containsExactly(3, 4);
        // can bo khong duoc phan bo khong xuat hien
        assertThat(rows).noneMatch(r -> r.employeeName().contains("UNUSED"));
    }

    @Test
    void updateRejectsMixingCreditWithNtdAndMoreThanThreeSegmentsAndKeepsPositionsPerUnit() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(3), lnId, gaId);
        Employee e1 = employee("E1", EmployeeAuditorClassification.TYPE_1, false, false, "LN", "GA");

        assertThatThrownBy(() -> khnsNamService.update(e1.getId(), YEAR, updateRequest("OBJ-A",
                List.of("TD_MEMBER", "NTD_MEMBER"), List.of("LN"))))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo("AUDIT_KHNS_PB_CREDIT_AND_NTD");
        assertThatThrownBy(() -> khnsNamService.update(e1.getId(), YEAR, updateRequest("OBJ-A",
                List.of("NTD_MEMBER"), List.of("LN", "GA"))))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo("AUDIT_KHNS_PB_CREDIT_AND_NTD");
        assertThatThrownBy(() -> khnsNamService.update(e1.getId(), YEAR, updateRequest("OBJ-A",
                List.of("NTD_MEMBER"), List.of("GA", "FA", "TF", "DP"))))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo("AUDIT_KHNS_PB_TOO_MANY_SEGMENTS");

        // hop le: Truong nhom Tin dung + thanh vien Tin dung -> chuc vu chung suy ra la Truong nhom
        khnsNamService.update(e1.getId(), YEAR, updateRequest("OBJ-A", List.of("TD_GROUP_LEAD", "TD_MEMBER"), List.of("LN")));
        AuditKhnsPbRowResponse row = pbService.listRows(YEAR).get(0);
        assertThat(row.roleInTeam()).isEqualTo(AuditKhnsRoleInTeam.GROUP_LEAD);
        assertThat(row.positions()).containsExactlyInAnyOrder("TD_GROUP_LEAD", "TD_MEMBER");
        assertThat(row.segmentCodes()).containsExactly("LN");

        // luu lai tu man KHNS_NAM (khong gui chuc vu chi tiet, giu nguyen chuc vu chung) -> chuc vu chi tiet duoc giu
        khnsNamService.update(e1.getId(), YEAR, new AuditKhnsNamUpdateRequest(AuditKhnsRoleInTeam.GROUP_LEAD, null, List.of("OBJ-A"),
                null, null, null, null, null, null, "OBJ-A", null, null, null, null, null, null, null, null, null, null));
        assertThat(pbService.listRows(YEAR).get(0).positions()).containsExactlyInAnyOrder("TD_GROUP_LEAD", "TD_MEMBER");
    }

    @Test
    void khnsNamListComesFromAllocationOnlyAfterSyncAndShowsMonthNamesPositionsAndTotalTeams() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, "12000", Set.of(3), lnId, gaId);
        object("OBJ-D", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(4), gaId);
        Employee lead = employee("E1", EmployeeAuditorClassification.TYPE_3, true, false, "LN");
        employee("E2", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        employee("E3", EmployeeAuditorClassification.TYPE_2, false, false, "LN");
        employee("E6", EmployeeAuditorClassification.TYPE_3, false, false, "LN");
        employee("E5", EmployeeAuditorClassification.TYPE_1, false, false, "GA");
        employee("E7", EmployeeAuditorClassification.TYPE_1, true, false, "GA");
        employee("E8-UNUSED", EmployeeAuditorClassification.TYPE_1, false, false);
        pbService.allocate(YEAR);

        // chua bam "Cap nhat danh sach can bo" -> danh sach KHNS_NAM trong
        assertThat(khnsNamService.list(YEAR, false, true)).isEmpty();

        int listed = khnsNamService.syncListFromAllocation(YEAR);

        List<com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse> rows = khnsNamService.list(YEAR, false, true);
        assertThat(rows).hasSize(listed).noneMatch(r -> r.employeeName().contains("UNUSED"));
        assertThat(listed).isEqualTo(pbService.listRows(YEAR).stream().map(AuditKhnsPbRowResponse::employeeId).distinct().count());
        assertThat(rows).allSatisfy(r -> assertThat(r.year()).isEqualTo(YEAR));

        // Truong doan cua OBJ-D (thang 4): chuc vu lay tu KHNS_PB, thang 4 hien TEN doi tuong, tong so doan = 1
        com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse leadRow = rows.stream()
                .filter(r -> r.positionDetails().stream().anyMatch(d -> d.positions().contains("TEAM_LEAD")))
                .filter(r -> r.monthAuditObjectNames().get(3) != null).findFirst().orElseThrow();
        // cot "Chuc vu" giong y nguyen dong tuong ung o man hinh KHNS_PB (chuc vu + nghiep vu)
        AuditKhnsPbRowResponse pbRow = pbService.listRows(YEAR).stream().filter(r -> r.employeeId().equals(leadRow.employeeId())).findFirst().orElseThrow();
        assertThat(leadRow.positionDetails()).containsExactly(
                new com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse.PositionDetail(pbRow.positions(), pbRow.segmentNames()));
        assertThat(leadRow.positionDetails().get(0).positions()).contains("QTDH_GROUP_LEAD", "QTDH_MEMBER");
        assertThat(leadRow.monthAuditObjectNames()).hasSize(12);
        assertThat(leadRow.monthAuditObjectNames().get(3)).isEqualTo("OBJ-D");
        assertThat(leadRow.monthAuditObjectNames().get(0)).isNull();
        assertThat(leadRow.totalTeamsCount()).isEqualTo(1);

        // sua thong tin nhap tay khong dong vao phan bo thang/chuc vu
        UUID leadId = UUID.fromString(leadRow.employeeId());
        khnsNamService.updateInfo(leadId, YEAR, new com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamInfoRequest("Kiem nhiem A", "QD-01", null, null, "ghi chu"));
        com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse after = khnsNamService.list(YEAR, false, true).stream()
                .filter(r -> r.employeeId().equals(leadRow.employeeId())).findFirst().orElseThrow();
        assertThat(after.otherDuties()).isEqualTo("Kiem nhiem A");
        assertThat(after.monthAuditObjectNames()).isEqualTo(leadRow.monthAuditObjectNames());
        assertThat(after.positionDetails()).isEqualTo(leadRow.positionDetails());

        // phan bo lai khong con cua 1 doi tuong -> bam cap nhat lai thi can bo khong con duoc phan bo bi go khoi danh sach
        for (AuditKhnsNam plan : khnsNamRepository.findByTenantIdAndYear(tenantId, YEAR)) {
            plan.setMonth3AuditObjectCode(null);
            plan.setMonth4AuditObjectCode(null);
            khnsNamRepository.save(plan);
            khnsNamObjectRepository.deleteByTenantIdAndKhnsNamId(tenantId, plan.getId());
        }
        assertThat(khnsNamService.syncListFromAllocation(YEAR)).isZero();
        assertThat(khnsNamService.list(YEAR, false, true)).isEmpty();
        assertThat(lead.getId()).isNotNull();
    }

    private AuditKhnsNamUpdateRequest updateRequest(String objectCode, List<String> positions, List<String> segments) {
        return new AuditKhnsNamUpdateRequest(null, null, List.of(objectCode), null, null, null, null, null, null, objectCode, null, null,
                null, null, null, null, null, null, null, List.of(new AuditKhnsNamUpdateRequest.ObjectAssignment(objectCode, positions, segments)));
    }

    @Test
    void listRowsIsEmptyWhenNobodyIsAllocated() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(3), gaId);
        employee("E1", EmployeeAuditorClassification.TYPE_1, true, false, "GA");

        assertThat(pbService.listRows(YEAR)).isEmpty();
    }

    @Test
    void allocateReportsShortfallInsteadOfFailing() {
        object("OBJ-A", AuditKhktApprovalStatus.APPROVED, true, "12000", Set.of(3), lnId);
        employee("E1", EmployeeAuditorClassification.TYPE_3, true, false, "LN");

        AuditKhnsPbAllocationResult result = pbService.allocate(YEAR);

        assertThat(result.fullyStaffedCount()).isZero();
        assertThat(result.warnings()).isNotEmpty().allMatch(w -> w.contains("OBJ-A"));
    }

    @Test
    void creditSegmentCodedTdIsTreatedAsCredit() {
        // moi truong demo dat ma nghiep vu Tin dung la "TD" thay vi "LN" - van phai bo tri 2 KTV tin dung
        UUID tdId = segment("TD");
        object("OBJ-TD", AuditKhktApprovalStatus.APPROVED, true, null, Set.of(3), tdId);
        Employee lead = employee("T1", EmployeeAuditorClassification.TYPE_1, true, false, "LN");
        Employee other = employee("T2", EmployeeAuditorClassification.TYPE_1, false, false, "LN");
        Employee third = employee("T3", EmployeeAuditorClassification.TYPE_1, false, false, "LN");

        AuditKhnsPbAllocationResult result = pbService.allocate(YEAR);

        assertThat(result.warnings()).isEmpty();
        assertThat(teamOf("OBJ-TD", 3)).containsExactlyInAnyOrder(lead.getId(), other.getId(), third.getId());
    }

    @Test
    void allocateWithNoApprovedObjectsReturnsHint() {
        object("OBJ-PENDING", AuditKhktApprovalStatus.PENDING, true, null, Set.of(3), gaId);

        AuditKhnsPbAllocationResult result = pbService.allocate(YEAR);

        assertThat(result.objectCount()).isZero();
        assertThat(result.warnings()).hasSize(1);
    }

    // ---------- helpers ----------

    private UUID segment(String code) {
        AuditMasterDataItem item = new AuditMasterDataItem();
        item.setTenantId(tenantId);
        item.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        item.setCode(code);
        item.setName(code);
        return masterDataItemRepository.save(item).getId();
    }

    private void scale(int order, String creditThreshold, String name) {
        AuditKhktScale s = new AuditKhktScale();
        s.setTenantId(tenantId);
        s.setSortOrder(order);
        s.setCreditThreshold(new BigDecimal(creditThreshold));
        s.setFundingThreshold(new BigDecimal(creditThreshold));
        s.setScaleName(name);
        scaleRepository.save(s);
    }

    private void object(String code, AuditKhktApprovalStatus status, boolean selection3, String loan, Set<Integer> months, UUID... segmentIds) {
        AuditKhktThConfirmed row = new AuditKhktThConfirmed();
        row.setTenantId(tenantId);
        row.setYear(YEAR);
        row.setSourceType(AuditKhktSourceType.OTHER);
        row.setAuditObjectCode(code);
        row.setAuditObjectName(code);
        row.setSelection3(selection3);
        row.setApprovalStatus(status);
        row.setKhktgsAfterAdjustment((AuditKhktSelectionChoice) null);
        row.setOnBalanceSheetLoan(loan == null ? null : new BigDecimal(loan));
        row = confirmedRepository.save(row);
        for (UUID segmentId : segmentIds) {
            AuditKhktThConfirmedSegment link = new AuditKhktThConfirmedSegment();
            link.setTenantId(tenantId);
            link.setConfirmedId(row.getId());
            link.setBusinessSegmentId(segmentId);
            confirmedSegmentRepository.save(link);
        }
        AuditKhktThang thang = new AuditKhktThang();
        thang.setTenantId(tenantId);
        thang.setYear(YEAR);
        thang.setAuditObjectCode(code);
        months.forEach(m -> setMonth(thang, m));
        thangRepository.save(thang);
    }

    private void setMonth(AuditKhktThang t, int month) {
        Consumer<AuditKhktThang> setter = switch (month) {
            case 3 -> x -> x.setMonth3(true);
            case 4 -> x -> x.setMonth4(true);
            default -> throw new IllegalArgumentException("month " + month);
        };
        setter.accept(t);
    }

    private Employee employee(String code, EmployeeAuditorClassification grade, boolean leadCapable, boolean onLeave, String... capabilities) {
        Employee e = new Employee();
        e.setTenantId(tenantId);
        e.setEmployeeCode("PB-" + code);
        e.setFullName("Nhan vien " + code);
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setAuditorClassification(grade);
        e.setTeamLeadCapable(leadCapable);
        e.setOnLeave(onLeave);
        e = employeeRepository.save(e);

        AuditEmployeeCapability c = new AuditEmployeeCapability();
        c.setTenantId(tenantId);
        c.setEmployeeId(e.getId());
        for (String cap : capabilities) {
            if (cap.equals("LN")) {
                c.setTdCapable(true);
            } else if (cap.equals("GA")) {
                c.setTcktCapable(true);
            }
        }
        capabilityRepository.save(c);
        return e;
    }

    private AuditKhnsNam plan(Employee e) {
        return khnsNamRepository.findByTenantIdAndYearAndEmployeeId(tenantId, YEAR, e.getId()).orElseThrow();
    }

    private List<String> objectCodes(AuditKhnsNam plan) {
        return khnsNamObjectRepository.findByTenantIdAndKhnsNamIdIn(tenantId, List.of(plan.getId())).stream()
                .map(AuditKhnsNamObject::getAuditObjectCode).collect(Collectors.toList());
    }

    private List<UUID> teamOf(String objectCode, int month) {
        return khnsNamRepository.findByTenantIdAndYear(tenantId, YEAR).stream()
                .filter(p -> objectCode.equals(monthCode(p, month)))
                .map(AuditKhnsNam::getEmployeeId).collect(Collectors.toList());
    }

    private String monthCode(AuditKhnsNam p, int month) {
        return switch (month) {
            case 3 -> p.getMonth3AuditObjectCode();
            case 4 -> p.getMonth4AuditObjectCode();
            default -> throw new IllegalArgumentException("month " + month);
        };
    }
}
