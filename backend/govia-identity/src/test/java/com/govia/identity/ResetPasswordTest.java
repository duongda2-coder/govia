package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.AdminResetPasswordRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.AuthService;
import com.govia.identity.service.EmployeeService;
import com.govia.identity.service.UserAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kich ban admin dat lai mat khau ho nhan vien (nhan vien quen mat khau, nho admin ho tro).
 * Test o tang service - viec chi SUPER_ADMIN moi goi duoc endpoint nay da duoc dam bao boi
 * @PreAuthorize tren EmployeeController, khong lap lai logic phan quyen o day.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResetPasswordTest {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-admin");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resetPassword_succeedsAndAllowsLoginWithNewPassword() {
        EmployeeResponse emp = employeeService.create(new EmployeeRequest("RST-T01", "Nguyen Van RST-T01",
                null, null, null, null, null, null, null, null, null, null));
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest("rst.t01", "OldPass123"));

        userAccountService.resetPassword(emp.id(), new AdminResetPasswordRequest("BrandNewPass789"));

        LoginResponse response = authService.login(new LoginRequest("default", "rst.t01", "BrandNewPass789"));
        assertThat(response.accessToken()).isNotBlank();

        assertThatThrownBy(() -> authService.login(new LoginRequest("default", "rst.t01", "OldPass123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resetPassword_rejectedWhenEmployeeHasNoAccount() {
        EmployeeResponse emp = employeeService.create(new EmployeeRequest("RST-T02", "Nguyen Van RST-T02",
                null, null, null, null, null, null, null, null, null, null));

        assertThatThrownBy(() -> userAccountService.resetPassword(emp.id(), new AdminResetPasswordRequest("BrandNewPass789")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chua co tai khoan");
    }
}
