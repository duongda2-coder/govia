package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.AccountSummaryResponse;
import com.govia.identity.dto.AdminResetPasswordRequest;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.Role;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.entity.UserRole;
import com.govia.identity.entity.UserStatus;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.RoleRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tao tai khoan dang nhap gan voi 1 Employee - dung tu man hinh nhan su (them nhan vien + cap tai khoan cung luc). */
@Service
public class UserAccountService {

    private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               EmployeeRepository employeeRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               PasswordEncoder passwordEncoder,
                               AuditLogService auditLogService,
                               ExcelExportService excelExportService) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
    }

    @Transactional
    public void createForEmployee(UUID employeeId, CreateUserAccountRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));

        if (userAccountRepository.existsByEmployeeId(employeeId)) {
            throw new BusinessException("EMPLOYEE_ALREADY_HAS_ACCOUNT", "Nhan vien nay da co tai khoan dang nhap");
        }
        userAccountRepository.findByTenantIdAndUsername(tenantId, request.username()).ifPresent(u -> {
            throw new BusinessException("USERNAME_DUPLICATE", "Ten dang nhap da ton tai: " + request.username());
        });

        UserAccount account = new UserAccount();
        account.setTenantId(tenantId);
        account.setEmployeeId(employee.getId());
        account.setUsername(request.username());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setEmail(employee.getEmail());
        account.setStatus(UserStatus.ACTIVE);
        userAccountRepository.save(account);

        auditLogService.record("UserAccount", employee.getId(), AuditAction.CREATE,
                "Tao tai khoan dang nhap \"" + request.username() + "\" cho nhan vien " + employee.getEmployeeCode());
    }

    /**
     * Tao tai khoan dang nhap voi mat khau ngau nhien - dung khi import Excel nhan vien co san cot
     * "Ten dang nhap" nhung khong the co cot mat khau trong file. Tra ve mat khau tam (dang plain
     * text, CHUA duoc hash) de noi goi hien thi lai cho admin gui cho nhan vien doi lai.
     */
    @Transactional
    public String createForEmployeeWithGeneratedPassword(UUID employeeId, String username) {
        String tempPassword = generateTempPassword();
        createForEmployee(employeeId, new CreateUserAccountRequest(username, tempPassword));
        return tempPassword;
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(TEMP_PASSWORD_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** Admin dat lai mat khau cho tai khoan cua 1 nhan vien - dung khi nhan vien quen mat khau va nho admin ho tro. */
    @Transactional
    public void resetPassword(UUID employeeId, AdminResetPasswordRequest request) {
        UserAccount account = userAccountRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Nhan vien nay chua co tai khoan dang nhap", HttpStatus.NOT_FOUND));

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(account);

        auditLogService.record("UserAccount", employeeId, AuditAction.UPDATE,
                "Admin dat lai mat khau tai khoan " + account.getUsername());
    }

    /** Danh sach toan bo tai khoan trong tenant kem vai tro dang gan - dung cho man hinh admin gan quyen. */
    public List<AccountSummaryResponse> listAccounts() {
        UUID tenantId = TenantContext.getTenantId();
        return userAccountRepository.findByTenantId(tenantId).stream()
                .map(account -> {
                    Employee employee = account.getEmployeeId() == null ? null
                            : employeeRepository.findById(account.getEmployeeId()).orElse(null);
                    List<String> roleCodes = userRoleRepository.findByUserId(account.getId()).stream()
                            .map(ur -> roleRepository.findById(ur.getRoleId()).map(Role::getCode).orElse(null))
                            .filter(code -> code != null)
                            .toList();
                    return new AccountSummaryResponse(
                            account.getId(),
                            account.getUsername(),
                            employee == null ? null : employee.getId(),
                            employee == null ? null : employee.getEmployeeCode(),
                            employee == null ? null : employee.getFullName(),
                            account.getStatus().name(),
                            roleCodes);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportAccountsExcel() {
        List<Map<String, Object>> rows = listAccounts().stream()
                .map(a -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("username", a.username());
                    row.put("employeeCode", a.employeeCode());
                    row.put("employeeName", a.employeeName());
                    row.put("status", a.status());
                    row.put("roleCodes", String.join(", ", a.roleCodes()));
                    return row;
                })
                .toList();
        return excelExportService.export("Tai khoan", accountExportColumns(), rows);
    }

    private List<ExportColumn> accountExportColumns() {
        return List.of(
                new ExportColumn("username", "Ten dang nhap"),
                new ExportColumn("employeeCode", "Ma nhan vien"),
                new ExportColumn("employeeName", "Ho ten nhan vien"),
                new ExportColumn("status", "Trang thai"),
                new ExportColumn("roleCodes", "Vai tro"));
    }

    /** Thay toan bo vai tro cua 1 tai khoan - tai khoan moi tao mac dinh KHONG co role nao (khong co quyen gi) cho den khi admin gan o day. */
    @Transactional
    public void assignRoles(UUID accountId, AssignRolesRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UserAccount account = userAccountRepository.findById(accountId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Khong tim thay tai khoan", HttpStatus.NOT_FOUND));

        List<UUID> roleIds = request.roleIds() == null ? List.of() : request.roleIds();
        List<Role> roles = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getTenantId().equals(tenantId))
                .toList();

        userRoleRepository.deleteByUserId(accountId);
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setTenantId(tenantId);
            userRole.setUserId(accountId);
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        }

        auditLogService.record("UserAccount", accountId, AuditAction.UPDATE,
                "Gan vai tro cho tai khoan " + account.getUsername() + ": "
                        + roles.stream().map(Role::getCode).reduce((a, b) -> a + ", " + b).orElse("(khong co)"));
    }

    /** Sao chep TOAN BO vai tro (nen quyen) tu 1 tai khoan nguon sang tai khoan dich - GHI DE vai tro hien co cua dich. */
    @Transactional
    public void copyRoles(UUID targetAccountId, UUID sourceAccountId) {
        UUID tenantId = TenantContext.getTenantId();
        UserAccount source = userAccountRepository.findById(sourceAccountId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Khong tim thay tai khoan nguon", HttpStatus.NOT_FOUND));
        userAccountRepository.findById(targetAccountId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Khong tim thay tai khoan dich", HttpStatus.NOT_FOUND));

        List<UUID> roleIds = userRoleRepository.findByUserId(sourceAccountId).stream().map(UserRole::getRoleId).toList();
        assignRoles(targetAccountId, new AssignRolesRequest(roleIds));

        auditLogService.record("UserAccount", targetAccountId, AuditAction.UPDATE,
                "Sao chep vai tro tu tai khoan " + source.getUsername() + " sang tai khoan nay");
    }

    /**
     * Xoa cung tai khoan dang nhap - chi SUPER_ADMIN duoc goi (xem AccountController). Dung khi can
     * xoa han 1 Employee dang bi EmployeeService.delete() chan lai (EMPLOYEE_HAS_USER_ACCOUNT): admin
     * xoa tai khoan nay truoc, roi quay lai xoa nhan vien. 2 guard an toan: khong cho xoa tai khoan
     * dang dang nhap cua chinh minh, va khong cho xoa tai khoan SUPER_ADMIN cuoi cung con lai (tranh
     * khoa het he thong khong con ai quan tri duoc).
     */
    @Transactional
    public void delete(UUID accountId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        UserAccount account = userAccountRepository.findById(accountId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Khong tim thay tai khoan", HttpStatus.NOT_FOUND));

        if (principal.userId().equals(accountId)) {
            throw new BusinessException("CANNOT_DELETE_OWN_ACCOUNT", "Khong the xoa tai khoan dang dang nhap cua chinh minh");
        }

        List<UUID> roleIds = userRoleRepository.findByUserId(accountId).stream().map(UserRole::getRoleId).toList();
        roleRepository.findByTenantIdAndCode(tenantId, SUPER_ADMIN_ROLE_CODE).ifPresent(superAdminRole -> {
            if (roleIds.contains(superAdminRole.getId()) && userRoleRepository.countByRoleId(superAdminRole.getId()) <= 1) {
                throw new BusinessException("CANNOT_DELETE_LAST_SUPER_ADMIN", "Khong the xoa: day la tai khoan SUPER_ADMIN cuoi cung con lai");
            }
        });

        userRoleRepository.deleteByUserId(accountId);
        userAccountRepository.delete(account);

        auditLogService.record("UserAccount", accountId, AuditAction.DELETE, "Xoa tai khoan dang nhap " + account.getUsername());
    }
}
