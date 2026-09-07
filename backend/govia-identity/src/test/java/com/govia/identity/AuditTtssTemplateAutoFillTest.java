package com.govia.identity;

import com.govia.audit.cmtd1.entity.AuditCmTd1;
import com.govia.audit.cmtd1.repository.AuditCmTd1Repository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chung AuditTtssSampleSelectionResolver duoc goi dung tu downloadTemplate(): chi tu dong dien
 * cot "Mã KH/TKHT/Mã CB"/"Tên KH" khi CKT nay chi co DUNG 1 dong AuditCmTd1 (khong mo ho); con neu
 * co 2 dong thi phai de trong nhu truoc (tranh dien nham - xem AuditTtssSampleSelectionResolver).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditTtssTemplateAutoFillTest {

    @Autowired
    private AuditTtssService ttssService;
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
    private AuditEngagementAssignmentRepository assignmentRepository;
    @Autowired
    private AuditWorkItemRepository workItemRepository;
    @Autowired
    private AuditMasterDataItemRepository masterDataItemRepository;
    @Autowired
    private AuditCmTd1Repository cmTd1Repository;

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
    void downloadTemplate_fillsCustomerColumns_whenExactlyOneSampleRowMatches() throws Exception {
        Fixture fixture = seedEngagementWithLnAssignment("01", "LNB0201");

        AuditCmTd1 sample = new AuditCmTd1();
        sample.setTenantId(tenantId);
        sample.setEngagementId(fixture.engagement.getId());
        sample.setAssignedEmployeeId(fixture.worker.id());
        sample.setBranchCode("CN01");
        sample.setAuditDate(LocalDate.now());
        sample.setCustomerCode("KH001");
        sample.setCustomerName("Nguyen Van Mau");
        cmTd1Repository.save(sample);

        Map<String, Integer> headerToColumn = new HashMap<>();
        Row dataRow = downloadAndReadFirstDataRow(fixture, headerToColumn);

        assertThat(dataRow.getCell(headerToColumn.get("Mã KH/TKHT/Mã CB")).getStringCellValue()).isEqualTo("KH001");
        assertThat(dataRow.getCell(headerToColumn.get("Tên KH")).getStringCellValue()).isEqualTo("Nguyen Van Mau");
    }

    @Test
    void downloadTemplate_leavesCustomerColumnsBlank_whenMultipleSampleRowsMatch() throws Exception {
        Fixture fixture = seedEngagementWithLnAssignment("02", "LNB0202");

        for (int i = 0; i < 2; i++) {
            AuditCmTd1 sample = new AuditCmTd1();
            sample.setTenantId(tenantId);
            sample.setEngagementId(fixture.engagement.getId());
            sample.setAssignedEmployeeId(fixture.worker.id());
            sample.setBranchCode("CN01");
            sample.setAuditDate(LocalDate.now());
            sample.setCustomerCode("KH00" + i);
            sample.setCustomerName("Khach hang " + i);
            cmTd1Repository.save(sample);
        }

        Map<String, Integer> headerToColumn = new HashMap<>();
        Row dataRow = downloadAndReadFirstDataRow(fixture, headerToColumn);

        Cell customerCodeCell = dataRow.getCell(headerToColumn.get("Mã KH/TKHT/Mã CB"));
        Cell customerNameCell = dataRow.getCell(headerToColumn.get("Tên KH"));
        assertThat(customerCodeCell == null || customerCodeCell.getStringCellValue().isBlank()).isTrue();
        assertThat(customerNameCell == null || customerNameCell.getStringCellValue().isBlank()).isTrue();
    }

    private Row downloadAndReadFirstDataRow(Fixture fixture, Map<String, Integer> headerToColumn) throws Exception {
        CurrentUserPrincipal teamLeadPrincipal = new CurrentUserPrincipal(UUID.randomUUID(), "teamlead-af", tenantId,
                fixture.teamLead.employeeCode(), List.of(), List.of(), UUID.randomUUID().toString());
        byte[] templateBytes = ttssService.downloadTemplate(fixture.engagement.getId(), teamLeadPrincipal);
        assertThat(templateBytes).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : headerRow) {
                headerToColumn.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            Row dataRow = sheet.getRow(sheet.getFirstRowNum() + 1);
            assertThat(dataRow).isNotNull();
            return dataRow;
        }
    }

    private Fixture seedEngagementWithLnAssignment(String suffix, String workItemCode) {
        EmployeeResponse teamLead = employeeService.create(employeeRequest("NVAFTL" + suffix));
        EmployeeResponse worker = employeeService.create(employeeRequest("NVAFWK" + suffix));

        AuditMasterDataItem segment = new AuditMasterDataItem();
        segment.setTenantId(tenantId);
        segment.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        segment.setCode("LN");
        segment.setName("Tín dụng");
        segment = masterDataItemRepository.save(segment);

        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("CNAF" + suffix);
        unit.setName("Chi nhanh auto-fill test");
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);

        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode("CKT-AF-" + suffix);
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(9);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setTeamLeadEmployeeId(teamLead.id());
        engagement.setDecisionNumber("QD-AF-" + suffix);
        engagement = engagementRepository.save(engagement);

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagement.getId());
        group.setGroupCode(AuditEngagementGroupCode.TINDUNG);
        group.setLeaderEmployeeId(teamLead.id());
        group = groupRepository.save(group);

        AuditEngagementGroupMember member = new AuditEngagementGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(group.getId());
        member.setEmployeeId(worker.id());
        member.setBusinessSegment1Id(segment.getId());
        member = memberRepository.save(member);

        AuditWorkItem workItem = new AuditWorkItem();
        workItem.setTenantId(tenantId);
        workItem.setPhase(AuditWorkPhase.THKT);
        workItem.setBusinessSegmentId(segment.getId());
        workItem.setCode(workItemCode);
        workItem.setName("Kiem tra ho so vay von");
        workItem.setActive(true);
        workItem = workItemRepository.save(workItem);

        AuditEngagementAssignment assignment = new AuditEngagementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setGroupMemberId(member.getId());
        assignment.setWorkItemId(workItem.getId());
        assignmentRepository.save(assignment);

        return new Fixture(engagement, teamLead, worker);
    }

    private EmployeeRequest employeeRequest(String code) {
        return new EmployeeRequest(code, "Nguyen Van " + code, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null);
    }

    private record Fixture(AuditEngagement engagement, EmployeeResponse teamLead, EmployeeResponse worker) {
    }
}
