package com.govia.identity;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chung DUNG THAT POI (ExcelExportServiceImpl/ExcelImportServiceImpl) qua vong lap "Tải mẫu"
 * -> nguoi dung sua/them dong -> "Import Data" cho danh muc "Lưu mã kiến nghị" (issue "chưa có phần
 * import kiến nghị lên") - cung 1 kieu test voi AuditTtssTemplateRoundTripTest, khong mock lop Excel.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditRecommendationTemplateRoundTripTest {

    @Autowired
    private AuditRecommendationService recommendationService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired
    private AuditEngagementRepository engagementRepository;
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
    void downloadTemplate_thenEditAndAddRow_thenUpload_upsertsByCodeThroughRealPoi() throws Exception {
        EmployeeResponse teamLead = employeeService.create(employeeRequest("NV-RCM-TL"));

        AuditMasterDataItem segment = new AuditMasterDataItem();
        segment.setTenantId(tenantId);
        segment.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        segment.setCode("LN");
        segment.setName("Tín dụng");
        segment = masterDataItemRepository.save(segment);

        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("CN-RCM01");
        unit.setName("Chi nhanh recommendation template test");
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);

        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode("CKT-RCM-01");
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(9);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setTeamLeadEmployeeId(teamLead.id());
        engagement.setDecisionNumber("QD-RCM-01");
        engagement = engagementRepository.save(engagement);

        byte[] templateBytes = recommendationService.downloadTemplate(engagement.getId());
        assertThat(templateBytes).isNotEmpty();

        Map<String, Integer> headerToColumn;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            headerToColumn = new HashMap<>();
            for (Cell cell : headerRow) {
                headerToColumn.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            // Dong 1 la KNKT000 tu dong seed - kiem tra dung noi dung mac dinh roi SUA lai qua Excel.
            Row defaultRow = sheet.getRow(sheet.getFirstRowNum() + 1);
            assertThat(defaultRow).isNotNull();
            assertThat(defaultRow.getCell(headerToColumn.get("Mã kiến nghị")).getStringCellValue()).isEqualTo("KNKT000");
            defaultRow.getCell(headerToColumn.get("Nội dung kiến nghị")).setCellValue("Kien nghi chung (da sua qua import)");

            // Them 1 dong moi KNKT001 chua tung ton tai.
            Row newRow = sheet.createRow(sheet.getFirstRowNum() + 2);
            newRow.createCell(headerToColumn.get("Mã kiến nghị")).setCellValue("KNKT001");
            newRow.createCell(headerToColumn.get("Loại nghiệp vụ")).setCellValue("LN");
            newRow.createCell(headerToColumn.get("Nội dung kiến nghị")).setCellValue("Thieu chu ky phe duyet ho so vay");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            templateBytes = out.toByteArray();
        }

        MockMultipartFile filledFile = new MockMultipartFile("file", "mau_kien_nghi.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

        List<AuditRecommendationResponse> uploaded = recommendationService.upload(engagement.getId(), filledFile);

        assertThat(uploaded).hasSize(2);
        AuditRecommendationResponse defaultAfterUpload = uploaded.stream().filter(r -> r.code().equals("KNKT000")).findFirst().orElseThrow();
        assertThat(defaultAfterUpload.content()).isEqualTo("Kien nghi chung (da sua qua import)");

        AuditRecommendationResponse created = uploaded.stream().filter(r -> r.code().equals("KNKT001")).findFirst().orElseThrow();
        assertThat(created.businessSegmentCode()).isEqualTo("LN");
        assertThat(created.content()).isEqualTo("Thieu chu ky phe duyet ho so vay");

        List<AuditRecommendationResponse> reloaded = recommendationService.list(engagement.getId());
        assertThat(reloaded).hasSize(2);
        assertThat(reloaded).extracting(AuditRecommendationResponse::code).containsExactlyInAnyOrder("KNKT000", "KNKT001");
    }

    private EmployeeRequest employeeRequest(String code) {
        return new EmployeeRequest(code, "Nguyen Van " + code, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null);
    }
}
