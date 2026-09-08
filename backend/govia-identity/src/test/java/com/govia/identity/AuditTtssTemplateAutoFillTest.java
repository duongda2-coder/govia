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
 * Kiem chung downloadTemplate() xuat 1 dong TTSS cho TUNG dong mau da upload (AuditCmTd1/CmNtd*),
 * KHONG con la 1 dong/cong viec da phan cong nhu truoc (doi theo yeu cau nguoi dung 2026-09-08 -
 * xem javadoc AuditTtssService.downloadTemplate()): 1 dong mau -> 1 dong TTSS, tat ca deu duoc dien
 * du lieu day du (khong con khai niem "mo ho, de trong" o muc dong mau nay nua - moi dong TTSS gan
 * DUNG 1 dong mau cua chinh no). "Mã công việc" van chi dien duoc khi nguoi duoc phan cong dong mau
 * do co DUNG 1 cong viec trong segment tuong ung (van co the mo ho - xem AuditTtssSampleSelectionResolver).
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
    void downloadTemplate_emitsOneRowFilled_whenExactlyOneSampleRowExists() throws Exception {
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
        List<Row> dataRows = downloadAndReadDataRows(fixture, headerToColumn);

        assertThat(dataRows).hasSize(1);
        Row dataRow = dataRows.get(0);
        assertThat(dataRow.getCell(headerToColumn.get("mã công việc")).getStringCellValue()).isEqualTo("LNB0201");
        assertThat(dataRow.getCell(headerToColumn.get("Mã KH/TKHT/Mã CB")).getStringCellValue()).isEqualTo("KH001");
        assertThat(dataRow.getCell(headerToColumn.get("Tên KH")).getStringCellValue()).isEqualTo("Nguyen Van Mau");
    }

    /** Doi tu "de trong khi mo ho" (hanh vi cu) sang "1 dong mau -> 1 dong TTSS" (hanh vi moi) - 2
     * dong mau cua CUNG 1 nguoi phai ra DUNG 2 dong TTSS, moi dong dien du lieu cua chinh no, va CA
     * HAI deu dien duoc "mã công việc" (nguoi do van chi co DUNG 1 cong viec trong segment LN, viec
     * co 2 dong mau khong lam mo ho them cot nay). */
    @Test
    void downloadTemplate_emitsOneRowPerSample_whenMultipleSampleRowsExist() throws Exception {
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
        List<Row> dataRows = downloadAndReadDataRows(fixture, headerToColumn);

        assertThat(dataRows).hasSize(2);
        assertThat(dataRows).extracting(r -> r.getCell(headerToColumn.get("Mã KH/TKHT/Mã CB")).getStringCellValue())
                .containsExactlyInAnyOrder("KH000", "KH001");
        assertThat(dataRows).extracting(r -> r.getCell(headerToColumn.get("Tên KH")).getStringCellValue())
                .containsExactlyInAnyOrder("Khach hang 0", "Khach hang 1");
        assertThat(dataRows).allSatisfy(r -> assertThat(r.getCell(headerToColumn.get("mã công việc")).getStringCellValue()).isEqualTo("LNB0202"));
    }

    private List<Row> downloadAndReadDataRows(Fixture fixture, Map<String, Integer> headerToColumn) throws Exception {
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
            List<Row> dataRows = new java.util.ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    dataRows.add(row);
                }
            }
            return dataRows;
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
