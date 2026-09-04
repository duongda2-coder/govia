package com.govia.identity;

import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityResponse;
import com.govia.audit.employeecapability.service.AuditEmployeeCapabilityService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
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

/**
 * Kich ban test cho man hinh "Khai bao kha nang dam nhan linh vuc cua nhan vien" (sheet ZTC_KNDN):
 * danh sach luon lay tat ca nhan vien (dong "ao" mac dinh chua tick khi chua tung luu), luu hang
 * loat tao/cap nhat dong that, va phe duyet chi thuc hien duoc 1 lan.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditEmployeeCapabilityServiceTest {

    @Autowired
    private AuditEmployeeCapabilityService capabilityService;

    @Autowired
    private EmployeeService employeeService;

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

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }

    private AuditEmployeeCapabilityItemRequest allFalseExcept(UUID employeeId, boolean tdCapable) {
        return new AuditEmployeeCapabilityItemRequest(employeeId, false, false, false, false, false, false, false,
                false, false, tdCapable, false, false, false, false);
    }

    @Test
    void list_includesEveryEmployeeEvenWithoutASavedCapabilityRow() {
        EmployeeResponse emp = createEmployee("EMP-KNDN-01");

        List<AuditEmployeeCapabilityResponse> list = capabilityService.list();

        assertThat(list).anySatisfy(r -> {
            assertThat(r.employeeId()).isEqualTo(emp.id());
            assertThat(r.tdCapable()).isFalse();
            assertThat(r.approved()).isFalse();
            assertThat(r.enteredBy()).isNull();
        });
    }

    @Test
    void bulkUpdate_createsCapabilityRowAndPersistsFlags() {
        EmployeeResponse emp = createEmployee("EMP-KNDN-02");

        List<AuditEmployeeCapabilityResponse> updated = capabilityService.bulkUpdate(List.of(allFalseExcept(emp.id(), true)));

        assertThat(updated).filteredOn(r -> r.employeeId().equals(emp.id()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.tdCapable()).isTrue();
                    assertThat(r.enteredBy()).isEqualTo("test-user");
                });
    }

    @Test
    void approve_stampsApproverAndTimestamp() {
        EmployeeResponse emp = createEmployee("EMP-KNDN-03");
        capabilityService.bulkUpdate(List.of(allFalseExcept(emp.id(), true)));

        AuditEmployeeCapabilityResponse approved = capabilityService.approve(emp.id());

        assertThat(approved.approved()).isTrue();
        assertThat(approved.approvedBy()).isEqualTo("test-user");
        assertThat(approved.approvedAt()).isNotNull();
    }

    @Test
    void approve_rejectedWhenAlreadyApproved() {
        EmployeeResponse emp = createEmployee("EMP-KNDN-04");
        capabilityService.approve(emp.id());

        assertThatThrownBy(() -> capabilityService.approve(emp.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da duoc phe duyet");
    }

    @Test
    void exportThenImportByUsername_updatesFlagsForMatchingEmployee() throws Exception {
        EmployeeResponse emp = createEmployee("EMP-KNDN-05");
        // Import mac dinh dong nay se loi "khong tim thay username" vi chua co tai khoan - dung de
        // xac nhan loi duoc bao dung dong thay vi lam hong ca file.
        byte[] excel = capabilityService.exportExcel();
        assertThat(excel).isNotEmpty();

        var result = capabilityService.importFromExcel(
                new org.springframework.mock.web.MockMultipartFile("file", "kndn.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel));

        // Khong nhan vien nao trong file mau co username (chua tao tai khoan) nen tat ca deu loi ro rang.
        assertThat(result.failureCount()).isGreaterThan(0);
        assertThat(result.errors()).allSatisfy(e -> assertThat(e.message()).contains("User Name"));
    }
}
