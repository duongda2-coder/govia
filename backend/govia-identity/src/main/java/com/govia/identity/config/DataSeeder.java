package com.govia.identity.config;

import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.entity.OrganizationUnit;
import com.govia.identity.entity.Permission;
import com.govia.identity.entity.Position;
import com.govia.identity.entity.Role;
import com.govia.identity.entity.RolePermission;
import com.govia.identity.entity.Tenant;
import com.govia.identity.entity.TenantStatus;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.entity.UserRole;
import com.govia.identity.entity.UserStatus;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.OrganizationUnitRepository;
import com.govia.identity.repository.PermissionRepository;
import com.govia.identity.repository.PositionRepository;
import com.govia.identity.repository.RolePermissionRepository;
import com.govia.identity.repository.RoleRepository;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.repository.UserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed du lieu demo khi database rong: tenant "default", 1 org unit, 1 employee,
 * user "admin"/"Admin@123", role SUPER_ADMIN voi quyen "*".
 * Chi chay 1 lan (kiem tra tenant "default" da ton tai chua) - xoa/tat class nay
 * khi trien khai production that.
 * @Order(1): phai chay TRUOC WorkflowSampleProcessSeeder (can tenant "default" da ton tai).
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final OrganizationUnitRepository orgUnitRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(TenantRepository tenantRepository,
                       OrganizationUnitRepository orgUnitRepository,
                       PositionRepository positionRepository,
                       EmployeeRepository employeeRepository,
                       UserAccountRepository userAccountRepository,
                       RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.positionRepository = positionRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tenantRepository.findByCode("default").isPresent()) {
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setCode("default");
        tenant.setName("GOVIA Default Tenant");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        OrganizationUnit headOffice = new OrganizationUnit();
        headOffice.setTenantId(tenant.getId());
        headOffice.setCode("HO");
        headOffice.setName("Head Office");
        headOffice.setType("COMPANY");
        headOffice = orgUnitRepository.save(headOffice);

        Position adminPosition = new Position();
        adminPosition.setTenantId(tenant.getId());
        adminPosition.setCode("PLATFORM_ADMIN");
        adminPosition.setName("Platform Administrator");
        adminPosition = positionRepository.save(adminPosition);

        Employee admin = new Employee();
        admin.setTenantId(tenant.getId());
        admin.setEmployeeCode("EMP0001");
        admin.setFullName("System Administrator");
        admin.setEmail("admin@govia.local");
        admin.setOrgUnitId(headOffice.getId());
        admin.setPositionId(adminPosition.getId());
        admin.setStatus(EmployeeStatus.ACTIVE);
        admin = employeeRepository.save(admin);

        Permission wildcard = new Permission();
        wildcard.setCode("*");
        wildcard.setModule("PLATFORM");
        wildcard.setDescription("Toan quyen tren toan platform - chi gan cho SUPER_ADMIN");
        wildcard = permissionRepository.save(wildcard);

        Role superAdmin = new Role();
        superAdmin.setTenantId(tenant.getId());
        superAdmin.setCode("SUPER_ADMIN");
        superAdmin.setName("Super Administrator");
        superAdmin.setSystemDefined(true);
        superAdmin = roleRepository.save(superAdmin);

        RolePermission rolePermission = new RolePermission();
        rolePermission.setTenantId(tenant.getId());
        rolePermission.setRoleId(superAdmin.getId());
        rolePermission.setPermissionId(wildcard.getId());
        rolePermissionRepository.save(rolePermission);

        UserAccount adminUser = new UserAccount();
        adminUser.setTenantId(tenant.getId());
        adminUser.setEmployeeId(admin.getId());
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
        adminUser.setEmail("admin@govia.local");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser = userAccountRepository.save(adminUser);

        UserRole userRole = new UserRole();
        userRole.setTenantId(tenant.getId());
        userRole.setUserId(adminUser.getId());
        userRole.setRoleId(superAdmin.getId());
        userRoleRepository.save(userRole);
    }
}
