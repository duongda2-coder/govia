package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.ChangePasswordRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
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
 * Kich ban test doi mat khau tu phuc vu - tao rieng 1 tai khoan test (khong dung "admin" seed san)
 * de tranh lam hong mat khau ma cac test khac (AuthSmokeTest, AbstractApiTest) dang phu thuoc.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChangePasswordTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TenantRepository tenantRepository;

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

    private UserAccount createTestAccount(String employeeCode, String username, String password) {
        EmployeeResponse emp = employeeService.create(new EmployeeRequest(employeeCode, "Nguyen Van " + employeeCode,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest(username, password));
        return userAccountRepository.findByTenantIdAndUsername(tenantId, username).orElseThrow();
    }

    @Test
    void changePassword_succeedsAndAllowsLoginWithNewPassword() {
        UserAccount account = createTestAccount("PWD-T01", "pwd.t01", "OldPass123");

        authService.changePassword(account.getId(), new ChangePasswordRequest("OldPass123", "NewPass456"));

        LoginResponse response = authService.login(new LoginRequest("default", "pwd.t01", "NewPass456"), null, null).login();
        assertThat(response.accessToken()).isNotBlank();

        assertThatThrownBy(() -> authService.login(new LoginRequest("default", "pwd.t01", "OldPass123"), null, null).login())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changePassword_rejectedWhenCurrentPasswordWrong() {
        UserAccount account = createTestAccount("PWD-T02", "pwd.t02", "OldPass123");

        assertThatThrownBy(() -> authService.changePassword(account.getId(), new ChangePasswordRequest("WrongPass", "NewPass456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khong dung");
    }
}
