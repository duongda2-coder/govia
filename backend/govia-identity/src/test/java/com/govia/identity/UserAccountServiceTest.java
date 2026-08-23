package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.dto.RoleResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.repository.UserRoleRepository;
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

/** Kich ban test tao tai khoan dang nhap gan voi nhan vien: thanh cong, trung username, da co tai khoan. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountServiceTest {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleService roleService;

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
        EmployeeRequest request = new EmployeeRequest(code, "Nguyen Van " + code, null, null, null, null, null, null, null, null, null, null, null);
        return employeeService.create(request);
    }

    @Test
    void createAccount_succeedsAndAppearsInEmployeeResponse() {
        EmployeeResponse employee = createEmployee("ACC-T01");

        userAccountService.createForEmployee(employee.id(), new CreateUserAccountRequest("acc.t01", "Password123"));

        assertThat(userAccountRepository.existsByEmployeeId(employee.id())).isTrue();
        EmployeeResponse reloaded = employeeService.getById(employee.id());
        assertThat(reloaded.username()).isEqualTo("acc.t01");
    }

    @Test
    void createAccount_rejectedWhenEmployeeAlreadyHasOne() {
        EmployeeResponse employee = createEmployee("ACC-T02");
        userAccountService.createForEmployee(employee.id(), new CreateUserAccountRequest("acc.t02", "Password123"));

        assertThatThrownBy(() -> userAccountService.createForEmployee(employee.id(), new CreateUserAccountRequest("acc.t02b", "Password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da co tai khoan");
    }

    @Test
    void createAccount_rejectedWhenUsernameDuplicate() {
        EmployeeResponse first = createEmployee("ACC-T03A");
        EmployeeResponse second = createEmployee("ACC-T03B");
        userAccountService.createForEmployee(first.id(), new CreateUserAccountRequest("acc.dup", "Password123"));

        assertThatThrownBy(() -> userAccountService.createForEmployee(second.id(), new CreateUserAccountRequest("acc.dup", "Password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ten dang nhap da ton tai");
    }

    @Test
    void copyRoles_overwritesTargetWithExactSourceRoleSet() {
        EmployeeResponse sourceEmp = createEmployee("ACC-T04A");
        EmployeeResponse targetEmp = createEmployee("ACC-T04B");
        userAccountService.createForEmployee(sourceEmp.id(), new CreateUserAccountRequest("acc.t04a", "Password123"));
        userAccountService.createForEmployee(targetEmp.id(), new CreateUserAccountRequest("acc.t04b", "Password123"));
        UUID sourceAccountId = userAccountRepository.findByEmployeeId(sourceEmp.id()).orElseThrow().getId();
        UUID targetAccountId = userAccountRepository.findByEmployeeId(targetEmp.id()).orElseThrow().getId();

        RoleResponse roleA = roleService.create(new RoleRequest("ACC_T04_ROLE_A", "Role A", null));
        RoleResponse roleB = roleService.create(new RoleRequest("ACC_T04_ROLE_B", "Role B", null));
        RoleResponse staleRole = roleService.create(new RoleRequest("ACC_T04_ROLE_STALE", "Role Stale", null));

        userAccountService.assignRoles(sourceAccountId, new AssignRolesRequest(List.of(roleA.id(), roleB.id())));
        userAccountService.assignRoles(targetAccountId, new AssignRolesRequest(List.of(staleRole.id())));

        userAccountService.copyRoles(targetAccountId, sourceAccountId);

        List<UUID> targetRoleIds = userRoleRepository.findByUserId(targetAccountId).stream()
                .map(ur -> ur.getRoleId()).toList();
        assertThat(targetRoleIds).containsExactlyInAnyOrder(roleA.id(), roleB.id());
    }

    @Test
    void copyRoles_rejectedWhenSourceAccountNotFound() {
        EmployeeResponse targetEmp = createEmployee("ACC-T05");
        userAccountService.createForEmployee(targetEmp.id(), new CreateUserAccountRequest("acc.t05", "Password123"));
        UUID targetAccountId = userAccountRepository.findByEmployeeId(targetEmp.id()).orElseThrow().getId();

        assertThatThrownBy(() -> userAccountService.copyRoles(targetAccountId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tai khoan nguon");
    }
}
