package com.govia.identity;

import com.govia.audit.masterdata.dto.MasterDataItemRequest;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.service.MasterDataItemService;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeFilter;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.dto.RoleResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import com.govia.identity.service.RoleService;
import com.govia.identity.service.UserAccountService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chung ca vong Xuat Excel -> Nhap lai Excel dung mau cho Danh muc Chuc vu va Employee -
 * dam bao ExcelImportService khop dung header voi ExportColumn va tao lai du lieu chinh xac.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportExportRoundTripTest {

    @Autowired
    private MasterDataItemService masterDataItemService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        TenantContext.setTenantId(tenant.getId());
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void exportThenImportPositions_recreatesRowsFromTemplate() {
        masterDataItemService.create(AuditMasterDataCategory.POSITION,
                new MasterDataItemRequest("RT-POS-01", "Ky su RoundTrip", null, null, null, null, null, true));
        int totalBeforeReimport = masterDataItemService.list(AuditMasterDataCategory.POSITION).size();
        byte[] excel = masterDataItemService.exportExcel(AuditMasterDataCategory.POSITION);

        MockMultipartFile file = new MockMultipartFile("file", "positions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);

        // Import lai CHINH file vua xuat (chua ma da co san) -> moi dong deu trung ma, khong dong nao duoc tao moi.
        ImportResult result = masterDataItemService.importFromExcel(AuditMasterDataCategory.POSITION, file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(totalBeforeReimport);
        assertThat(result.errors()).anyMatch(e -> e.message().contains("da ton tai"));
    }

    @Test
    void exportThenImportEmployees_recreatesRowFromTemplate() throws Exception {
        employeeService.create(new EmployeeRequest("RT-EMP-01", "Nguyen Van RoundTrip", null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));

        byte[] excel = employeeService.exportExcel(
                new EmployeeFilter(null, null, "RT-EMP-01", null, null, null, null, null, null, null, null));

        MockMultipartFile file = new MockMultipartFile("file", "employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);

        ImportResult result = employeeService.importFromExcel(file);

        // "RT-EMP-01" da ton tai nen import lai chinh no phai bao trung ma, khong tao ban sao.
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors().get(0).message()).contains("da ton tai");

        var page = employeeService.list(
                new EmployeeFilter(null, null, "RT-EMP-01", null, null, null, null, null, null, null, null),
                PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void importEmployees_withUsernameColumn_createsLoginAccountWithTempPassword() throws Exception {
        employeeService.create(new EmployeeRequest("RT-EMP-USR-01", "Nguyen Van Username", null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));

        byte[] excel = employeeService.exportExcel(
                new EmployeeFilter(null, null, "RT-EMP-USR-01", null, null, null, null, null, null, null, null));
        byte[] edited = editExportedRow(excel, "Ma NV", "RT-EMP-USR-01", "RT-EMP-USR-02", "Ten dang nhap", "rt.emp.usr.02");

        MockMultipartFile file = new MockMultipartFile("file", "employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", edited);

        ImportResult result = employeeService.importFromExcel(file);

        assertThat(result.failureCount()).isZero();
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.notices()).anyMatch(n -> n.contains("rt.emp.usr.02") && n.contains("RT-EMP-USR-02"));

        var page = employeeService.list(
                new EmployeeFilter(null, null, "RT-EMP-USR-02", null, null, null, null, null, null, null, null),
                PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).username()).isEqualTo("rt.emp.usr.02");
    }

    /**
     * Doc file Excel da xuat, tim dong du lieu dau tien co gia tri oldKeyValue o cot keyHeader, doi gia tri
     * cot do sang newKeyValue va cot targetHeader sang newTargetValue - dung de gia lap 1 file import da duoc
     * nguoi dung sua tay tu file mau xuat ra.
     */
    private byte[] editExportedRow(byte[] excel, String keyHeader, String oldKeyValue, String newKeyValue,
                                    String targetHeader, String newTargetValue) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            int keyCol = -1;
            int targetCol = -1;
            for (var cell : header) {
                String text = cell.getStringCellValue();
                if (keyHeader.equals(text)) keyCol = cell.getColumnIndex();
                if (targetHeader.equals(text)) targetCol = cell.getColumnIndex();
            }
            assertThat(keyCol).isNotEqualTo(-1);
            assertThat(targetCol).isNotEqualTo(-1);

            DataFormatter formatter = new DataFormatter();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (oldKeyValue.equals(formatter.formatCellValue(row.getCell(keyCol)))) {
                    row.getCell(keyCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(newKeyValue);
                    row.getCell(targetCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(newTargetValue);
                    break;
                }
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void exportThenImportRolePermissions_overwritesTargetRoleExactly() {
        RoleResponse source = roleService.create(new RoleRequest("RT-ROLE-01", "RoundTrip Source", null));
        roleService.setPermissionCodes(source.id(),
                new RolePermissionsRequest(List.of("PEOPLE.EMPLOYEE.VIEW", "PEOPLE.POSITION.VIEW")));
        byte[] excel = roleService.exportPermissionsExcel(source.id());

        RoleResponse target = roleService.create(new RoleRequest("RT-ROLE-02", "RoundTrip Target", null));
        roleService.setPermissionCodes(target.id(), new RolePermissionsRequest(List.of("PEOPLE.ORGUNIT.VIEW")));

        MockMultipartFile file = new MockMultipartFile("file", "role-permissions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);
        ImportResult result = roleService.importPermissionsExcel(target.id(), file);

        assertThat(result.failureCount()).isZero();
        assertThat(result.successCount()).isEqualTo(2);
        // Ghi de toan bo: quyen PEOPLE.ORGUNIT.VIEW co san tren target (khong co trong file import) phai bi go.
        assertThat(roleService.getPermissionCodes(target.id()))
                .containsExactlyInAnyOrder("PEOPLE.EMPLOYEE.VIEW", "PEOPLE.POSITION.VIEW");
    }

    @Test
    void exportRolesExcel_containsCreatedRole() throws Exception {
        roleService.create(new RoleRequest("RBAC_T05_ROLE", "Test Role 5", null));

        byte[] excel = roleService.exportRolesExcel();

        assertThat(anyRowContains(excel, "RBAC_T05_ROLE")).isTrue();
    }

    @Test
    void exportAccountsExcel_containsCreatedAccount() throws Exception {
        EmployeeResponse emp = employeeService.create(new EmployeeRequest("RT-ACC-01", "Nguyen Van Export",
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest("rt.acc.01", "Password123"));

        byte[] excel = userAccountService.exportAccountsExcel();

        assertThat(anyRowContains(excel, "rt.acc.01")).isTrue();
    }

    /** Doc tho tat ca cac o cua file Excel xuat ra, kiem tra co dong nao chua gia tri can tim khong. */
    private boolean anyRowContains(byte[] excel, String value) throws Exception {
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (var cell : row) {
                    if (value.equals(formatter.formatCellValue(cell))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
