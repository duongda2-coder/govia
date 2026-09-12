package com.govia.identity;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberRequest;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementTeamMemberDetailResponse;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.processengagement.entity.AuditProcessEngagement;
import com.govia.audit.planengagement.processengagement.repository.AuditProcessEngagementRepository;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportResponse;
import com.govia.audit.planengagement.progressreport.service.AuditProgressReportService;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditEngagementTeamService;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import com.govia.audit.workitemqt.repository.AuditWorkItemQtRepository;
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
 * Kiem chung 3 man hinh con lai duoc flag la "chua sua" trong
 * [[qt_work_item_catalog_separation]]: TTSS (downloadTemplate), Bao cao tien do
 * (countAssignedSamples qua recordUpload), va "Chi tiet doan kiem toan" (teamDetail) deu phai doc
 * dung AuditWorkItemQt (qua AuditAssignmentWorkItemResolver) cho 1 CKT quy trinh, thay vi bo sot
 * cac phan cong co workItemId == null.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditProcessEngagementDownstreamScreensTest {

    @Autowired
    private AuditEngagementTeamService teamService;
    @Autowired
    private AuditTtssService ttssService;
    @Autowired
    private AuditProgressReportService progressReportService;
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
    private AuditEngagementAssignmentRepository assignmentRepository;
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
    void downloadTemplate_includesBareRowForQtWorkItem_assignedButNoSampleData() throws Exception {
        Fixture fixture = seedQtFixture("DS01", AuditWorkPhase.THKT, false);

        CurrentUserPrincipal teamLeadPrincipal = principalFor(fixture.teamLead, "ds01tl");
        byte[] templateBytes = ttssService.downloadTemplate(fixture.childEngagement.getId(), teamLeadPrincipal);
        assertThat(templateBytes).isNotEmpty();

        Map<String, Integer> headerToColumn = new HashMap<>();
        List<Row> dataRows;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : headerRow) {
                headerToColumn.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            dataRows = new java.util.ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    dataRows.add(row);
                }
            }
        }

        assertThat(dataRows).hasSize(1);
        assertThat(dataRows.get(0).getCell(headerToColumn.get("mã công việc")).getStringCellValue()).isEqualTo(fixture.workItemQt.getCode());
    }

    @Test
    void recordUpload_countsAssignedQtWorkItem_asSample() {
        Fixture fixture = seedQtFixture("DS02", AuditWorkPhase.CBKT, false);

        AuditProgressReportResponse report = progressReportService.recordUpload(fixture.childEngagement.getId(), fixture.worker.id(),
                fixture.segment.getId(), List.of(), "test upload", "worker-ds02", null);

        assertThat(report.totalSamples()).isEqualTo(1);
        assertThat(report.completedSamples()).isZero();

        AuditEngagementAssignment assignment = assignmentRepository.findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(tenantId, fixture.memberId)
                .get(0);
        assignment.setStatus(AssignmentStatus.DONE);
        assignmentRepository.save(assignment);

        AuditProgressReportResponse secondReport = progressReportService.recordUpload(fixture.childEngagement.getId(), fixture.worker.id(),
                fixture.segment.getId(), List.of(), "test upload 2", "worker-ds02", null);
        assertThat(secondReport.totalSamples()).isEqualTo(1);
        assertThat(secondReport.completedSamples()).isEqualTo(1);
    }

    @Test
    void teamDetail_countsAssignedQtWorkItem_inCorrectProgressBucket() {
        Fixture fixture = seedQtFixture("DS03", AuditWorkPhase.THKT, false);

        List<AuditEngagementTeamMemberDetailResponse> detail = monitoringService.teamDetail(fixture.childEngagement.getId(),
                principalFor(fixture.teamLead, "ds03tl"));

        AuditEngagementTeamMemberDetailResponse workerRow = detail.stream()
                .filter(r -> r.employeeId().equals(fixture.worker.id())).findFirst().orElseThrow();
        assertThat(workerRow.thktNoSampleProgress().total()).isEqualTo(1);
        assertThat(workerRow.thktNoSampleProgress().completed()).isZero();
        assertThat(workerRow.cbktProgress().total()).isZero();
        assertThat(workerRow.thktSampleProgress().total()).isZero();
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

    /** LN da co san trong AuditTtssSampleSelectionResolver.SUPPORTED_SEGMENT_CODES nen dung duoc
     * ca cho downloadTemplate() (cac test khac deu dung "LN"). */
    private Fixture seedQtFixture(String suffix, AuditWorkPhase phase, boolean hasSampleSelection) {
        AuditMasterDataItem segment = createSegment("LN");
        EmployeeResponse teamLead = createEmployee("NVDS-TL-" + suffix);
        EmployeeResponse worker = createEmployee("NVDS-WK-" + suffix);

        AuditProcessEngagement processEngagement = new AuditProcessEngagement();
        processEngagement.setTenantId(tenantId);
        processEngagement.setCode("QTDS" + suffix);
        processEngagement.setObjectType("QT");
        processEngagement.setBusinessSegmentId(segment.getId());
        processEngagement.setYear(2026);
        processEngagement.setExpectedMonth(9);
        processEngagement.setDecisionDate(LocalDate.now());
        processEngagement.setTeamLeadEmployeeId(teamLead.id());
        processEngagement.setDecisionNumber("QD-QTDS" + suffix);
        processEngagement.setWorkSetCode("WSET-" + suffix);
        processEngagement = processEngagementRepository.save(processEngagement);

        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode(("DS" + suffix).substring(0, Math.min(8, ("DS" + suffix).length())));
        unit.setName("Chi nhanh test DS" + suffix);
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);

        AuditEngagement childEngagement = new AuditEngagement();
        childEngagement.setTenantId(tenantId);
        childEngagement.setCode("QTDS" + suffix + "_01");
        childEngagement.setAuditObjectUnitId(unit.getId());
        childEngagement.setYear(2026);
        childEngagement.setExpectedMonth(9);
        childEngagement.setDecisionDate(LocalDate.now());
        childEngagement.setTeamLeadEmployeeId(teamLead.id());
        childEngagement.setDecisionNumber("QD-" + suffix);
        childEngagement.setProcessEngagementId(processEngagement.getId());
        childEngagement = engagementRepository.save(childEngagement);

        AuditWorkItemQt workItemQt = new AuditWorkItemQt();
        workItemQt.setTenantId(tenantId);
        workItemQt.setPhase(phase);
        workItemQt.setBusinessSegmentId(segment.getId());
        workItemQt.setCode("QTDSW" + suffix);
        workItemQt.setName("Cong viec quy trinh DS" + suffix);
        workItemQt.setWorkSetCode("WSET-" + suffix);
        workItemQt.setHasSampleSelection(hasSampleSelection);
        workItemQt.setActive(true);
        workItemQt = workItemQtRepository.save(workItemQt);

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(childEngagement.getId());
        group.setGroupCode(segment.getCode());
        group.setLeaderEmployeeId(teamLead.id());
        group = groupRepository.save(group);

        AuditEngagementGroupMemberResponse member = teamService.addMember(childEngagement.getId(), group.getId(),
                new AuditEngagementGroupMemberRequest(worker.id(), segment.getId(), null, null));

        return new Fixture(childEngagement, teamLead, worker, segment, workItemQt, member.id());
    }

    private CurrentUserPrincipal principalFor(EmployeeResponse employee, String username) {
        return new CurrentUserPrincipal(UUID.randomUUID(), username, tenantId, employee.employeeCode(), List.of(), List.of(),
                UUID.randomUUID().toString());
    }

    private record Fixture(AuditEngagement childEngagement, EmployeeResponse teamLead, EmployeeResponse worker,
                            AuditMasterDataItem segment, AuditWorkItemQt workItemQt, UUID memberId) {
    }
}
