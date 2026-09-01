package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.dto.RoleResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.service.AuthService;
import com.govia.identity.service.EmployeeService;
import com.govia.identity.service.RoleService;
import com.govia.identity.service.UserAccountService;
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
 * Kich ban cot loi cua RBAC: tai khoan moi tao mac dinh KHONG co quyen gi cho den khi
 * admin gan role co quyen tuong ung.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleAccountPermissionTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        TenantContext.setTenantId(tenant.getId());
        TenantContext.setCurrentUser("test-admin");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null));
    }

    @Test
    void freshAccountWithNoRole_hasNoPermissions() {
        EmployeeResponse emp = createEmployee("RBAC-T01");
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest("rbac.t01", "Password123"));

        LoginResponse response = authService.login(new LoginRequest("default", "rbac.t01", "Password123"), null, null).login();

        assertThat(response.permissions()).isEmpty();
    }

    @Test
    void assigningRoleWithPermission_grantsExactlyThatPermission() {
        EmployeeResponse emp = createEmployee("RBAC-T02");
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest("rbac.t02", "Password123"));
        UUID accountId = userAccountRepository.findByEmployeeId(emp.id()).orElseThrow().getId();

        RoleResponse viewerRole = roleService.create(new RoleRequest("EMPLOYEE_VIEWER", "Employee Viewer", "Chi xem nhan vien"));
        roleService.setPermissionCodes(viewerRole.id(), new RolePermissionsRequest(List.of("PEOPLE.EMPLOYEE.VIEW")));

        userAccountService.assignRoles(accountId, new AssignRolesRequest(List.of(viewerRole.id())));

        LoginResponse response = authService.login(new LoginRequest("default", "rbac.t02", "Password123"), null, null).login();

        assertThat(response.permissions()).containsExactly("PEOPLE.EMPLOYEE.VIEW");
        assertThat(response.roles()).containsExactly("EMPLOYEE_VIEWER");
    }

    @Test
    void settingPermissionsTwice_keepingSameCode_doesNotFail() {
        RoleResponse role = roleService.create(new RoleRequest("RBAC_T04_ROLE", "Test Role 4", null));
        roleService.setPermissionCodes(role.id(), new RolePermissionsRequest(List.of("PEOPLE.EMPLOYEE.VIEW")));

        roleService.setPermissionCodes(role.id(),
                new RolePermissionsRequest(List.of("PEOPLE.EMPLOYEE.VIEW", "PEOPLE.POSITION.VIEW")));

        assertThat(roleService.getPermissionCodes(role.id()))
                .containsExactlyInAnyOrder("PEOPLE.EMPLOYEE.VIEW", "PEOPLE.POSITION.VIEW");
    }

    @Test
    void superAdminRole_cannotBeEditedOrDeleted() {
        RoleResponse superAdmin = roleService.list().stream()
                .filter(r -> "SUPER_ADMIN".equals(r.code()))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> roleService.update(superAdmin.id(), new RoleRequest("SUPER_ADMIN", "Renamed", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("he thong");
        assertThatThrownBy(() -> roleService.delete(superAdmin.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("he thong");
    }

    @Test
    void role_cannotBeDeletedWhileAssignedToAnAccount() {
        EmployeeResponse emp = createEmployee("RBAC-T03");
        userAccountService.createForEmployee(emp.id(), new CreateUserAccountRequest("rbac.t03", "Password123"));
        UUID accountId = userAccountRepository.findByEmployeeId(emp.id()).orElseThrow().getId();

        RoleResponse role = roleService.create(new RoleRequest("RBAC_T03_ROLE", "Test Role", null));
        userAccountService.assignRoles(accountId, new AssignRolesRequest(List.of(role.id())));

        assertThatThrownBy(() -> roleService.delete(role.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dang duoc gan");
    }
}
