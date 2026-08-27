package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.EmployeeFilter;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kich ban test cho CRUD nhan vien: trung ma, quan ly khong ton tai, vong lap quan ly,
 * loc danh sach, xuat Excel/Word. Dung H2 in-memory (profile "test") - khong can Postgres that.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeServiceTest {

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

    private EmployeeRequest sampleRequest(String code, UUID managerId) {
        return new EmployeeRequest(code, "Nguyen Van " + code, code.toLowerCase() + "@govia.local", null,
                "0900000000", null, null, null, null, null, null, managerId, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null);
    }

    private EmployeeFilter keywordFilter(String keyword) {
        return new EmployeeFilter(null, null, keyword, null, null, null, null, null, null, null);
    }

    @Test
    void createEmployee_succeedsWithActiveStatus() {
        EmployeeResponse created = employeeService.create(sampleRequest("EMP-T01", null));

        assertThat(created.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(created.employeeCode()).isEqualTo("EMP-T01");
    }

    @Test
    void duplicateEmployeeCode_isRejected() {
        employeeService.create(sampleRequest("EMP-T02", null));

        assertThatThrownBy(() -> employeeService.create(sampleRequest("EMP-T02", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da ton tai");
    }

    @Test
    void managerNotFound_isRejected() {
        assertThatThrownBy(() -> employeeService.create(sampleRequest("EMP-T03", UUID.randomUUID())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quan ly");
    }

    @Test
    void selfManager_isRejectedOnUpdate() {
        EmployeeResponse emp = employeeService.create(sampleRequest("EMP-T04", null));

        assertThatThrownBy(() -> employeeService.update(emp.id(), sampleRequest("EMP-T04", emp.id())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chinh minh");
    }

    @Test
    void circularManagerChain_isRejected() {
        EmployeeResponse a = employeeService.create(sampleRequest("EMP-T05A", null));
        EmployeeResponse b = employeeService.create(sampleRequest("EMP-T05B", a.id())); // B bao cao cho A

        // Thu doi A bao cao cho B -> vong lap A -> B -> A
        assertThatThrownBy(() -> employeeService.update(a.id(), sampleRequest("EMP-T05A", b.id())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vong lap");
    }

    @Test
    void changeStatus_updatesEmployee() {
        EmployeeResponse emp = employeeService.create(sampleRequest("EMP-T06", null));

        EmployeeResponse updated = employeeService.changeStatus(emp.id(), EmployeeStatus.ON_LEAVE);

        assertThat(updated.status()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    @Test
    void listByKeyword_findsMatchingEmployee() {
        employeeService.create(sampleRequest("EMP-T07", null));

        Page<EmployeeResponse> page = employeeService.list(keywordFilter("EMP-T07"), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmployeeResponse::employeeCode).contains("EMP-T07");
    }

    @Test
    void listByColumnFilter_matchesOnlyThatColumn() {
        employeeService.create(sampleRequest("EMP-T09", null));

        EmployeeFilter byCode = new EmployeeFilter(null, null, null, "EMP-T09", null, null, null, null, null, null);
        Page<EmployeeResponse> page = employeeService.list(byCode, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(EmployeeResponse::employeeCode).containsExactly("EMP-T09");
    }

    @Test
    void listSortedByFullName_ordersAscending() {
        employeeService.create(sampleRequest("EMP-T10Z", null));
        employeeService.create(sampleRequest("EMP-T10A", null));

        Page<EmployeeResponse> page = employeeService.list(keywordFilter("EMP-T10"),
                PageRequest.of(0, 10, Sort.by("fullName").ascending()));

        assertThat(page.getContent()).extracting(EmployeeResponse::employeeCode)
                .containsExactly("EMP-T10A", "EMP-T10Z");
    }

    @Test
    void exportExcelAndWord_produceNonEmptyFiles() {
        employeeService.create(sampleRequest("EMP-T08", null));

        byte[] excel = employeeService.exportExcel(keywordFilter("EMP-T08"));
        byte[] word = employeeService.exportWord(keywordFilter("EMP-T08"));

        assertThat(excel).isNotEmpty();
        assertThat(word).isNotEmpty();
    }

    @Test
    void deleteEmployee_succeedsWhenNoDependents() {
        EmployeeResponse emp = employeeService.create(sampleRequest("EMP-T11", null));

        employeeService.delete(emp.id());

        Page<EmployeeResponse> page = employeeService.list(keywordFilter("EMP-T11"), PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void deleteEmployee_rejectedWhenIsManagerOfSomeone() {
        EmployeeResponse manager = employeeService.create(sampleRequest("EMP-T12A", null));
        employeeService.create(sampleRequest("EMP-T12B", manager.id()));

        assertThatThrownBy(() -> employeeService.delete(manager.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quan ly");
    }
}
