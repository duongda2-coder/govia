package com.govia.identity;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.tdkp.assignment.AuditTdkpAssignmentDto;
import com.govia.audit.tdkp.assignment.AuditTdkpAssignmentService;
import com.govia.audit.tdkp.assignment.TdkpAssignmentScope;
import com.govia.audit.tdkp.branch.AuditTdkpBranchDto;
import com.govia.audit.tdkp.branch.AuditTdkpBranchService;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoRecommendationDto;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoRecommendationService;
import com.govia.audit.tdkp.ceo.TdkpCeoScope;
import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.audit.tdkp.common.TdkpTarget;
import com.govia.audit.tdkp.report.AuditTdkpReportArchiveService;
import com.govia.audit.tdkp.report.AuditTdkpReportDto;
import com.govia.audit.tdkp.report.AuditTdkpReportService;
import com.govia.audit.tdkp.report.AuditTdkpReportType;
import com.govia.audit.tdkp.resolution.AuditTdkpResolutionDto;
import com.govia.audit.tdkp.resolution.AuditTdkpResolutionService;
import com.govia.audit.tdkp.unitrec.AuditTdkpUnitRecommendationDto;
import com.govia.audit.tdkp.unitrec.AuditTdkpUnitRecommendationService;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.TenantRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kiem chung nhom man hinh "Theo doi khac phuc" (TDKP - file 6.TDKP_29.5.2026): phan cong, kien nghi HDTV/TGD (ALL -> KH), kien nghi chi nhanh
 * (chuyen tu TTSS + quy tac hien trang), nghi quyet, kien nghi don vi, 5 bao cao Excel - chay tren H2 in-memory (Liquibase that). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditTdkpTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditMasterDataItemRepository masterDataItemRepository;
    @Autowired private AuditObjectUnitRepository unitRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditEngagementRepository engagementRepository;
    @Autowired private AuditRecommendationRepository auditRecommendationRepository;
    @Autowired private AuditTtssRecordRepository ttssRepository;
    @Autowired private AuditTdkpAssignmentService assignmentService;
    @Autowired private AuditTdkpCeoRecommendationService ceoService;
    @Autowired private AuditTdkpBranchService branchService;
    @Autowired private AuditTdkpResolutionService resolutionService;
    @Autowired private AuditTdkpUnitRecommendationService unitService;
    @Autowired private AuditTdkpReportService reportService;
    @Autowired private AuditTdkpReportArchiveService archiveService;

    private UUID tenantId;
    private UUID typeId;
    private UUID segmentId;
    private AuditObjectUnit unit;
    private Employee employee;

    @BeforeEach
    void setUp() {
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("tdkp-tester");
        typeId = masterItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, "CCS", "Cơ chế chính sách");
        segmentId = masterItem(AuditMasterDataCategory.BUSINESS_SEGMENT, "TDKP-LN", "Tín dụng");
        unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("C200");
        unit.setName("Chi nhánh Điện Biên");
        unit.setUnitType("CN");
        unit = unitRepository.save(unit);
        employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setEmployeeCode("TDKP-E1");
        employee.setFullName("Lê Thị Thu Hà");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee = employeeRepository.save(employee);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- quy tac thuan ----------

    @Test
    void deadlineStateAndCodeGeneration() {
        LocalDate today = LocalDate.of(2026, 5, 29);
        assertThat(TdkpSupport.deadlineState(today.minusDays(1), today)).isEqualTo("OVERDUE");
        assertThat(TdkpSupport.deadlineState(today, today)).isEqualTo("ON_TIME");
        assertThat(TdkpSupport.deadlineState(today.plusDays(1), today)).isEqualTo("ON_TIME");
        assertThat(TdkpSupport.deadlineState(null, today)).isNull();
        assertThat(TdkpSupport.nextCode("KN", List.of())).isEqualTo("KN001");
        assertThat(TdkpSupport.nextCode("KN", List.of("KN001", "KN009", "KNIA050"))).isEqualTo("KN010");
        // ngay Excel: so serial 44275 = 20/03/2021
        assertThat(TdkpSupport.parseDate("44275")).isEqualTo(LocalDate.of(2021, 3, 20));
        assertThat(TdkpSupport.parseDate("17.03.2021")).isEqualTo(LocalDate.of(2021, 3, 17));
    }

    @Test
    void defectStatusRuleFollowsSheetCn() {
        assertThat(AuditTdkpBranchService.computeDefectStatus(List.of())).isNull();
        assertThat(AuditTdkpBranchService.computeDefectStatus(List.of(TdkpStatus.DONE, TdkpStatus.DONE))).isEqualTo(TdkpStatus.DONE);
        assertThat(AuditTdkpBranchService.computeDefectStatus(List.of(TdkpStatus.NOT_STARTED, TdkpStatus.NOT_STARTED))).isEqualTo(TdkpStatus.NOT_STARTED);
        assertThat(AuditTdkpBranchService.computeDefectStatus(List.of(TdkpStatus.IN_PROGRESS, TdkpStatus.IN_PROGRESS))).isEqualTo(TdkpStatus.IN_PROGRESS);
        assertThat(AuditTdkpBranchService.computeDefectStatus(List.of(TdkpStatus.DONE, TdkpStatus.NOT_STARTED))).isEqualTo(TdkpStatus.IN_PROGRESS);
        // chua chon hien trang = Chua thuc hien
        assertThat(AuditTdkpBranchService.computeDefectStatus(java.util.Arrays.asList(null, TdkpStatus.NOT_STARTED))).isEqualTo(TdkpStatus.NOT_STARTED);
    }

    // ---------- sheet 1: phan cong ----------

    @Test
    void assignmentValidatesDatesAndDerivesEmployeeInfo() {
        assertThatThrownBy(() -> assignmentService.create(new AuditTdkpAssignmentDto.Request(TdkpAssignmentScope.CEO, typeId, segmentId, TdkpTarget.HDTV, null, null,
                employee.getId(), LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 1)))).isInstanceOf(BusinessException.class).hasMessageContaining("Ngay ket thuc");
        assertThatThrownBy(() -> assignmentService.create(new AuditTdkpAssignmentDto.Request(TdkpAssignmentScope.BRANCH, typeId, segmentId, null, null, null,
                employee.getId(), null, null))).isInstanceOf(BusinessException.class);

        AuditTdkpAssignmentDto.Response ceo = assignmentService.create(new AuditTdkpAssignmentDto.Request(TdkpAssignmentScope.CEO, typeId, segmentId, TdkpTarget.TGD,
                null, null, employee.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        assignmentService.create(new AuditTdkpAssignmentDto.Request(TdkpAssignmentScope.BRANCH, typeId, segmentId, null, unit.getId(), null, employee.getId(), null, null));

        assertThat(ceo.employeeName()).isEqualTo("Lê Thị Thu Hà");
        assertThat(ceo.targetObjectLabel()).isEqualTo("TGĐ");
        assertThat(ceo.recommendationTypeName()).isEqualTo("Cơ chế chính sách");
        assertThat(assignmentService.list(TdkpAssignmentScope.CEO)).hasSize(1);
        assertThat(assignmentService.list(TdkpAssignmentScope.BRANCH)).singleElement().satisfies(r -> assertThat(r.auditObjectUnitCode()).isEqualTo("C200"));
        assertThat(sheetRows(assignmentService.exportExcel(TdkpAssignmentScope.BRANCH))).hasSize(2);
    }

    // ---------- sheet 2/3: kien nghi HDTV/TGD ----------

    @Test
    void ceoRecommendationsImportChecksListsAndTransferToKhIsIdempotent() throws Exception {
        List<List<String>> table = List.of(
                List.of("Số báo cáo", "Ngày báo cáo", "Nội dung kiến nghị", "Phân loại kiến nghị", "Mã mảng nghiệp vụ", "Đối tượng được kiến nghị", "Đơn vị thực hiện",
                        "Chỉ đạo của HĐTV, TGĐ", "Thời hạn hoàn thành", "Hiện trạng", "Đánh giá tình hình thực hiện kiến nghị của Phòng nghiệp vụ", "Ghi chú"),
                List.of("BC012021/HĐTV", "44275", "Xây dựng quy trình Quản lý thuê ngoài", "Cơ chế chính sách", "TDKP-LN", "HĐTV", "C200", "Đầu mối triển khai", "44550",
                        "Đang thực hiện", "Đang dự thảo", "Bổ sung bản mềm"),
                List.of("BC022021/HĐTV", "44275", "Kiến nghị có phân loại sai", "Loại không tồn tại", "", "TGĐ", "", "", "", "", "", ""),
                List.of("BC032021/HĐTV", "44275", "Kiến nghị có hiện trạng sai", "", "", "TGĐ", "", "", "", "Xong rồi", "", ""));
        ImportResult result = ceoService.importFromExcel(TdkpCeoScope.ALL, xlsx(table));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.errors()).extracting(ImportResult.ImportRowError::message).anyMatch(m -> m.contains("Phan loai kien nghi")).anyMatch(m -> m.contains("Hien trang"));

        AuditTdkpCeoRecommendationDto.Response saved = ceoService.list(TdkpCeoScope.ALL).get(0);
        assertThat(saved.recommendationTypeName()).isEqualTo("Cơ chế chính sách");
        assertThat(saved.reportDate()).isEqualTo(LocalDate.of(2021, 3, 20));
        assertThat(saved.deadline()).isEqualTo(LocalDate.of(2021, 12, 20));
        assertThat(saved.statusLabel()).isEqualTo("Đang thực hiện");
        assertThat(saved.deadlineState()).isEqualTo("OVERDUE");
        assertThat(saved.executingUnitName()).isEqualTo("Chi nhánh Điện Biên");
        assertThat(saved.lastEditedBy()).isEqualTo("tdkp-tester");

        AuditTdkpCeoRecommendationDto.TransferResult first = ceoService.transferFromAll();
        AuditTdkpCeoRecommendationDto.TransferResult second = ceoService.transferFromAll();
        assertThat(first.transferred()).isEqualTo(1);
        assertThat(second.transferred()).isZero();
        assertThat(second.skipped()).isEqualTo(1);
        AuditTdkpCeoRecommendationDto.Response kh = ceoService.list(TdkpCeoScope.KH).get(0);
        assertThat(kh.content()).isEqualTo(saved.content());
        assertThat(kh.sourceId()).isEqualTo(saved.id());
        // ZTC_TDKP_CEO_KH cho phep sua ca cac cot tu dong day sang; ALL khong bi anh huong
        ceoService.update(TdkpCeoScope.KH, kh.id(), new AuditTdkpCeoRecommendationDto.Request(kh.reportNumber(), kh.reportDate(), "Nội dung đã sửa ở KH", typeId, segmentId,
                TdkpTarget.TGD, null, kh.directive(), kh.deadline(), TdkpStatus.DONE, kh.evaluation(), kh.note()));
        assertThat(ceoService.list(TdkpCeoScope.ALL).get(0).content()).isEqualTo("Xây dựng quy trình Quản lý thuê ngoài");
        assertThat(sheetRows(ceoService.exportExcel(TdkpCeoScope.KH))).hasSize(2);
    }

    // ---------- sheet 4: chi nhanh ----------

    @Test
    void branchTransferFromTtssBuildsRecommendationsAndDefects() {
        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode("CN" + "C200" + "2020" + "01");
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2020);
        engagement.setTeamLeadEmployeeId(employee.getId());
        engagement = engagementRepository.save(engagement);
        AuditRecommendation recommendation = new AuditRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setEngagementId(engagement.getId());
        recommendation.setCode("KNKT001");
        recommendation.setContent("Yêu cầu Chi nhánh khắc phục các tồn tại, sai sót");
        recommendation.setBusinessSegmentId(segmentId);
        recommendation = auditRecommendationRepository.save(recommendation);
        ttss(engagement.getId(), recommendation.getId(), AssignmentApprovalStatus.APPROVED, "Sai sót 1", "Khách A", "LAV-1");
        ttss(engagement.getId(), recommendation.getId(), AssignmentApprovalStatus.APPROVED, "Sai sót 2", "Khách B", "LAV-2");
        ttss(engagement.getId(), recommendation.getId(), AssignmentApprovalStatus.PENDING, "Chua duyet", "Khách C", "LAV-3");
        ttss(engagement.getId(), null, null, "Chua gan kien nghi", "Khách D", "LAV-4");

        AuditTdkpBranchDto.TransferResult first = branchService.transferFromExecution(null);
        AuditTdkpBranchDto.TransferResult second = branchService.transferFromExecution(2020);

        assertThat(first.recommendationsCreated()).isEqualTo(1);
        assertThat(first.defectsCreated()).isEqualTo(2);
        assertThat(second.recommendationsCreated()).isZero();
        assertThat(second.defectsCreated()).isZero();

        AuditTdkpBranchDto.RecommendationResponse row = branchService.listRecommendations().get(0);
        assertThat(row.managementCode()).isEqualTo("KN001");
        assertThat(row.branchName()).isEqualTo("Chi nhánh Điện Biên");
        assertThat(row.branchCode()).isEqualTo("C200");
        assertThat(row.auditYear()).isEqualTo(2020);
        assertThat(row.businessSegmentCode()).isEqualTo("TDKP-LN");
        assertThat(row.defectCount()).isEqualTo(2);

        List<AuditTdkpBranchDto.DefectResponse> defects = branchService.listDefects(row.id());
        assertThat(defects).hasSize(2);
        assertThat(defects).allSatisfy(d -> assertThat(d.recommendationDefectStatus()).isEqualTo(TdkpStatus.NOT_STARTED));
        // 1 sai sot da chinh sua, 1 chua -> hien trang chung cua kien nghi la "Dang thuc hien"
        AuditTdkpBranchDto.DefectResponse first1 = defects.get(0);
        branchService.updateDefect(first1.id(), new AuditTdkpBranchDto.DefectRequest(row.id(), first1.defectContent(), first1.customerEntry(), first1.creditContract(),
                first1.defectCode(), first1.defectType(), TdkpStatus.DONE, first1.relatedStaff()));
        assertThat(branchService.listDefects(row.id())).allSatisfy(d -> assertThat(d.recommendationDefectStatus()).isEqualTo(TdkpStatus.IN_PROGRESS));
        assertThat(branchService.listRecommendations().get(0).defectDoneCount()).isEqualTo(1);

        // ma quan ly tu sinh tiep theo, xoa kien nghi xoa luon sai sot
        AuditTdkpBranchDto.RecommendationResponse manual = branchService.createRecommendation(new AuditTdkpBranchDto.RecommendationRequest(unit.getId(), 2021,
                "Kiến nghị nhập tay", typeId, segmentId, LocalDate.of(2026, 12, 31), null, TdkpStatus.NOT_STARTED, null, null));
        assertThat(manual.managementCode()).isEqualTo("KN002");
        branchService.deleteRecommendation(row.id());
        assertThat(branchService.listDefects(null)).isEmpty();
    }

    // ---------- sheet 5, 6 ----------

    @Test
    void resolutionAndUnitRecommendationGenerateCodesAndValidateImport() throws Exception {
        AuditTdkpResolutionDto.Response nq1 = resolutionService.create(new AuditTdkpResolutionDto.Request("BC012021/HĐTV", LocalDate.of(2021, 3, 17), null, "Triển khai kế hoạch",
                null, null, null, null, null, null, null, "Đang đánh giá", TdkpStatus.IN_PROGRESS, null, "Đang đánh giá", null, null, "Nguyễn Thành Công", null, null, null, null));
        AuditTdkpResolutionDto.Response nq2 = resolutionService.create(new AuditTdkpResolutionDto.Request("02/NQ", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null));
        assertThat(nq1.code()).isEqualTo("NQ001");
        assertThat(nq2.code()).isEqualTo("NQ002");

        AuditTdkpUnitRecommendationDto.Response kn = unitService.create(new AuditTdkpUnitRecommendationDto.Request("BC01", LocalDate.of(2021, 3, 17), unit.getName(), null,
                "Xây dựng quy trình", LocalDate.of(2021, 3, 17), "Đang thực hiện", TdkpStatus.IN_PROGRESS, "Đúng hạn", null, null));
        assertThat(kn.code()).isEqualTo("KNIA001");
        assertThat(kn.deadlineState()).isEqualTo("OVERDUE");
        // "Đơn vị kiến nghị" gio la text tu do (khong con FK toi danh muc doi tuong kiem toan) - dong
        // BC03 thieu "Nội dung kiến nghị" (bat buoc) de van giu duoc 1 dong loi trong bai test nay.
        ImportResult result = unitService.importFromExcel(xlsx(List.of(
                List.of("Số báo cáo", "Đơn vị kiến nghị", "Nội dung kiến nghị", "Hiện trạng"),
                List.of("BC02", "Chi nhánh Điện Biên", "Nội dung hợp lệ", "Chưa thực hiện"),
                List.of("BC03", "Đơn vị lạ", "", ""))));
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(unitService.list()).extracting(AuditTdkpUnitRecommendationDto.Response::code).contains("KNIA001", "KNIA002");
    }

    // ---------- sheet 7-12: bao cao ----------

    @Test
    void allFiveReportsBuildAndProduceReadableWorkbooks() throws Exception {
        ceoService.create(TdkpCeoScope.KH, new AuditTdkpCeoRecommendationDto.Request("BC01", LocalDate.of(2026, 1, 5), "Kiến nghị HĐTV", typeId, segmentId, TdkpTarget.HDTV,
                unit.getId(), "Chỉ đạo", LocalDate.of(2026, 3, 1), TdkpStatus.IN_PROGRESS, "Đánh giá", "Ghi chú"));
        AuditTdkpBranchDto.RecommendationResponse rec = branchService.createRecommendation(new AuditTdkpBranchDto.RecommendationRequest(unit.getId(), 2020,
                "Kiến nghị chi nhánh", typeId, segmentId, LocalDate.of(2026, 3, 1), "Đã sửa", TdkpStatus.DONE, "Tốt", null));
        branchService.createRecommendation(new AuditTdkpBranchDto.RecommendationRequest(unit.getId(), 2020, "Kiến nghị chưa xong", typeId, segmentId, null, null,
                TdkpStatus.NOT_STARTED, null, null));
        branchService.createDefect(new AuditTdkpBranchDto.DefectRequest(rec.id(), "Sai sót A", "Khách A", "LAV-1", null, "Loại 1", TdkpStatus.DONE, null));
        branchService.createDefect(new AuditTdkpBranchDto.DefectRequest(rec.id(), "Sai sót B", "Khách B", "LAV-2", null, "Loại 2", TdkpStatus.NOT_STARTED, null));
        unitService.create(new AuditTdkpUnitRecommendationDto.Request("BC-DV", LocalDate.of(2026, 1, 5), unit.getName(), null, "Kiến nghị đơn vị", null, null, TdkpStatus.DONE, null, null, null));
        resolutionService.create(new AuditTdkpResolutionDto.Request("11/NQ-HĐTV", LocalDate.of(2026, 1, 21), null, "Bổ sung phương án", null, null, null, null, null, null, null,
                "Đang làm", TdkpStatus.IN_PROGRESS, null, "Tốt", null, null, "NTC", null, null, null, null));

        LocalDate asOf = LocalDate.of(2026, 2, 28);
        AuditTdkpReportDto.ReportData bc01 = reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC01, asOf, null, null, "12/BKS", asOf, null, null, null));
        assertThat(bc01.rows()).hasSize(1);
        assertThat(bc01.rows().get(0)).hasSize(14).contains("Trong hạn", "Kiến nghị HĐTV");
        assertThat(bc01.totals().get(3)).isEqualTo("1");
        assertThat(bc01.subtitle()).contains("28/02/2026").contains("12/BKS");
        // "Den thoi diem" 28/03/2026 > han 01/03/2026 -> Qua han
        assertThat(reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC01, LocalDate.of(2026, 3, 28), null, null, null, null, null, null, null))
                .rows().get(0)).contains("Quá hạn");

        assertThatThrownBy(() -> reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC02, asOf, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        AuditTdkpReportDto.ReportData bc02 = reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC02, asOf, 2020, unit.getId(), null, null, null, "10/QĐ", null));
        assertThat(bc02.heading()).isEqualTo("Chi nhánh Điện Biên");
        assertThat(bc02.rows()).hasSize(3); // 2 sai sot cua kien nghi 1 + 1 dong cho kien nghi khong co sai sot
        assertThat(bc02.stats().get(0)).containsExactly("Tổng số kiến nghị", "2");
        assertThat(bc02.stats().get(1)).containsExactly("Tổng số tồn tại sai sót liên quan", "2");
        assertThat(bc02.stats().get(2)).containsExactly("Số kiến nghị đã thực hiện", "1", "50.0%");
        assertThat(bc02.stats().get(3)).containsExactly("Số tồn tại sai sót đã chỉnh sửa", "1", "50.0%");
        assertThat(bc02.stats().get(4)).containsExactly("Số kiến nghị còn phải chỉnh sửa", "1");
        assertThat(bc02.stats().get(5)).containsExactly("Số tồn tại sai sót còn phải chỉnh sửa", "1");

        AuditTdkpReportDto.ReportData bc03 = reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC03, asOf, null, null, null, null, null, null, null));
        assertThat(bc03.rows()).hasSize(2);
        // tong: 2 kien nghi, 2 TTSS, da chinh sua 1 kien nghi + 1 TTSS, con lai 1 kien nghi + 1 TTSS
        assertThat(bc03.totals().subList(7, 13)).containsExactly("2", "2", "1", "1", "1", "1");

        AuditTdkpReportDto.ReportData bc04 = reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC04, asOf, null, null, null, null, null, null, null));
        assertThat(bc04.rows()).hasSize(1);
        AuditTdkpReportDto.ReportData bc05 = reportService.build(new AuditTdkpReportDto.ReportRequest(AuditTdkpReportType.BC05, asOf, 2026, null, null, null, null, null, null));
        assertThat(bc05.rows()).hasSize(1);
        assertThat(bc05.heading()).isEqualTo("2026");

        for (AuditTdkpReportType type : AuditTdkpReportType.values()) {
            byte[] file = reportService.export(new AuditTdkpReportDto.ReportRequest(type, asOf, null, unit.getId(), null, null, null, null, null));
            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
                Sheet sheet = workbook.getSheetAt(0);
                assertThat(sheet.getLastRowNum()).as(type.name()).isGreaterThanOrEqualTo(4);
                List<String> texts = new ArrayList<>();
                sheet.forEach(row -> row.forEach(cell -> texts.add(cell.toString())));
                assertThat(texts).as(type.name()).contains(type == AuditTdkpReportType.BC05 ? "SỐ NQ" : "STT");
            }
        }

        // luu tru bao cao da xuat: xoa ban ghi khong de lai file dinh kem
        AuditTdkpReportDto.ArchiveResponse archive = archiveService.create(new AuditTdkpReportDto.ArchiveRequest(AuditTdkpReportType.BC01, "BC01 den 28/02/2026", asOf, null));
        assertThat(archiveService.list()).singleElement().satisfies(a -> assertThat(a.reportTypeTitle()).contains("HĐTV"));
        archiveService.delete(archive.id());
        assertThat(archiveService.list()).isEmpty();
    }

    // ---------- tien ich ----------

    private UUID masterItem(AuditMasterDataCategory category, String code, String name) {
        AuditMasterDataItem item = new AuditMasterDataItem();
        item.setTenantId(tenantId);
        item.setCategory(category);
        item.setCode(code);
        item.setName(name);
        return masterDataItemRepository.save(item).getId();
    }

    private void ttss(UUID engagementId, UUID recommendationId, AssignmentApprovalStatus status, String content, String customer, String contract) {
        AuditTtssRecord record = new AuditTtssRecord();
        record.setTenantId(tenantId);
        record.setEngagementId(engagementId);
        record.setTeamRecommendationId(recommendationId);
        record.setRecommendationApprovalStatus(status);
        record.setTtssContent(content);
        record.setCustomerName(customer);
        record.setReferenceNumber(contract);
        record.setFindingName("Loại " + content);
        ttssRepository.save(record);
    }

    private MockMultipartFile xlsx(List<List<String>> table) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("import");
            for (int r = 0; r < table.size(); r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < table.get(r).size(); c++) {
                    row.createCell(c).setCellValue(table.get(r).get(c));
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private List<Row> sheetRows(byte[] file) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            List<Row> rows = new ArrayList<>();
            workbook.getSheetAt(0).forEach(rows::add);
            return rows;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
