package com.govia.identity;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.progressreport.repository.AuditProgressReportRepository;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.progressreport.entity.AuditProgressReport;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chung DUNG THAT POI (ExcelExportServiceImpl/ExcelImportServiceImpl) qua vong lap "Download
 * Template" -> nguoi dung dien tay 1 vai cot -> "Upload file TTSS", khong mock lop Excel - day la
 * lop chua tung duoc test o AuditTtssAndProgressReportWorkflowTest (test do dung entity dung san,
 * khong qua Excel that).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditTtssTemplateRoundTripTest {

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
    private AuditProgressReportRepository progressReportRepository;

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
    void downloadTemplate_thenFillAndUpload_roundTripsThroughRealPoi() throws Exception {
        EmployeeResponse teamLead = employeeService.create(employeeRequest("NV-TPL-TL"));
        EmployeeResponse worker = employeeService.create(employeeRequest("NV-TPL-WK"));

        AuditMasterDataItem segment = new AuditMasterDataItem();
        segment.setTenantId(tenantId);
        segment.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        segment.setCode("LN");
        segment.setName("Tín dụng");
        segment = masterDataItemRepository.save(segment);

        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("CN-TPL01");
        unit.setName("Chi nhanh template test");
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);

        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode("CKT-TPL-01");
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(9);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setTeamLeadEmployeeId(teamLead.id());
        engagement.setDecisionNumber("QD-TPL-01");
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
        workItem.setCode("LNB0101");
        workItem.setName("Kiem tra ho so vay von");
        workItem.setActive(true);
        workItem = workItemRepository.save(workItem);

        AuditEngagementAssignment assignment = new AuditEngagementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setGroupMemberId(member.getId());
        assignment.setWorkItemId(workItem.getId());
        assignmentRepository.save(assignment);

        byte[] templateBytes = ttssService.downloadTemplate(engagement.getId());
        assertThat(templateBytes).isNotEmpty();

        Map<String, Integer> headerToColumn;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            headerToColumn = new HashMap<>();
            for (Cell cell : headerRow) {
                headerToColumn.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            Row dataRow = sheet.getRow(sheet.getFirstRowNum() + 1);
            assertThat(dataRow).isNotNull();
            assertThat(dataRow.getCell(headerToColumn.get("Nghiệp vụ")).getStringCellValue()).isEqualTo("LN");
            assertThat(dataRow.getCell(headerToColumn.get("Mã công việc")).getStringCellValue()).isEqualTo("LNB0101");

            // Nguoi dung dien tay cac cot TTSS con lai truoc khi upload lai.
            dataRow.createCell(headerToColumn.get("Nội dung TTSS")).setCellValue("Ho so vay thieu chu ky lanh dao chi nhanh");
            dataRow.createCell(headerToColumn.get("Mã phát hiện")).setCellValue("LN0101_01");
            dataRow.createCell(headerToColumn.get("Tên phát hiện")).setCellValue("Thieu chu ky phe duyet");
            dataRow.createCell(headerToColumn.get("Trọng yếu")).setCellValue("x");
            dataRow.createCell(headerToColumn.get("Tên KH")).setCellValue("Nguyen Van A");
            dataRow.createCell(headerToColumn.get("Số tiền")).setCellValue(1500000);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            templateBytes = out.toByteArray();
        }

        MockMultipartFile filledFile = new MockMultipartFile("file", "mau_upload_ttss.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);
        CurrentUserPrincipal workerPrincipal = new CurrentUserPrincipal(UUID.randomUUID(), "worker-tpl", tenantId,
                worker.employeeCode(), List.of(), List.of(), UUID.randomUUID().toString());

        List<AuditTtssRecordResponse> uploaded = ttssService.upload(engagement.getId(), filledFile, "Bao cao lan dau", workerPrincipal);

        assertThat(uploaded).hasSize(1);
        AuditTtssRecordResponse record = uploaded.get(0);
        assertThat(record.businessSegmentCode()).isEqualTo("LN");
        assertThat(record.ttssContent()).isEqualTo("Ho so vay thieu chu ky lanh dao chi nhanh");
        assertThat(record.findingCode()).isEqualTo("LN0101_01");
        assertThat(record.findingName()).isEqualTo("Thieu chu ky phe duyet");
        assertThat(record.material()).isTrue();
        assertThat(record.customerName()).isEqualTo("Nguyen Van A");
        assertThat(record.amount()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(record.ttssPerformerName()).isEqualTo(worker.fullName());

        List<AuditProgressReport> reports = progressReportRepository.findByTenantIdAndEngagementIdOrderByReportDateDesc(tenantId, engagement.getId());
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getTotalFindings()).isEqualTo(1);
        assertThat(reports.get(0).getTotalMaterialFindings()).isEqualTo(1);
    }

    private EmployeeRequest employeeRequest(String code) {
        return new EmployeeRequest(code, "Nguyen Van " + code, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null);
    }
}
