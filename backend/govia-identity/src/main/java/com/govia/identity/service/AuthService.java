package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.JwtTokenProvider;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.ChangePasswordRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.Permission;
import com.govia.identity.entity.RolePermission;
import com.govia.identity.entity.Tenant;
import com.govia.identity.entity.TenantStatus;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.entity.UserRole;
import com.govia.identity.entity.UserStatus;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.PermissionRepository;
import com.govia.identity.repository.RolePermissionRepository;
import com.govia.identity.repository.RoleRepository;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.repository.UserRoleRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(TenantRepository tenantRepository,
                        UserAccountRepository userAccountRepository,
                        EmployeeRepository employeeRepository,
                        UserRoleRepository userRoleRepository,
                        RoleRepository roleRepository,
                        PermissionRepository permissionRepository,
                        RolePermissionRepository rolePermissionRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider tokenProvider,
                        AuditLogService auditLogService) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findByCode(request.tenantCode())
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant khong ton tai", HttpStatus.UNAUTHORIZED));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessException("TENANT_SUSPENDED", "Tenant dang bi tam ngung", HttpStatus.UNAUTHORIZED);
        }

        UserAccount user = userAccountRepository.findByTenantIdAndUsername(tenant.getId(), request.username())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Sai ten dang nhap hoac mat khau", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("USER_NOT_ACTIVE", "Tai khoan khong o trang thai hoat dong", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Sai ten dang nhap hoac mat khau", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);

        return buildLoginResponse(tenant, user);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parseClaims(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token khong hop le", HttpStatus.UNAUTHORIZED);
        }
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "User khong ton tai", HttpStatus.UNAUTHORIZED));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant khong ton tai", HttpStatus.UNAUTHORIZED));

        return buildLoginResponse(tenant, user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay tai khoan", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Mat khau hien tai khong dung");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);

        auditLogService.record("UserAccount", user.getId(), AuditAction.UPDATE,
                "Doi mat khau tai khoan " + user.getUsername());
    }

    private LoginResponse buildLoginResponse(Tenant tenant, UserAccount user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<String> roleCodes = userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).map(r -> r.getCode()).orElse(null))
                .filter(code -> code != null)
                .toList();

        List<String> permissionCodes = computePermissionCodes(userRoles);

        String employeeCode = null;
        if (user.getEmployeeId() != null) {
            employeeCode = employeeRepository.findById(user.getEmployeeId())
                    .map(Employee::getEmployeeCode)
                    .orElse(null);
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), tenant.getId(),
                employeeCode, roleCodes, permissionCodes);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return new LoginResponse(accessToken, refreshToken, user.getId(), user.getUsername(), employeeCode,
                tenant.getId(), roleCodes, permissionCodes);
    }

    /**
     * Gom quyen tu tat ca role duoc gan cho user. Neu bat ky role nao co quyen wildcard "*"
     * (chi SUPER_ADMIN duoc seed san quyen nay) thi tra ve TOAN BO permission trong catalog -
     * dam bao admin luon full quyen ke ca khi co man hinh/quyen moi duoc them sau nay.
     * Tai khoan chua duoc gan role nao se nhan danh sach rong => khong lam duoc gi ca.
     */
    private List<String> computePermissionCodes(List<UserRole> userRoles) {
        List<UUID> permissionIds = userRoles.stream()
                .flatMap(ur -> rolePermissionRepository.findByRoleId(ur.getRoleId()).stream())
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();

        List<Permission> grantedPermissions = permissionRepository.findAllById(permissionIds);
        boolean hasWildcard = grantedPermissions.stream().anyMatch(p -> "*".equals(p.getCode()));

        if (hasWildcard) {
            return permissionRepository.findAll().stream()
                    .map(Permission::getCode)
                    .filter(code -> !"*".equals(code))
                    .toList();
        }
        return grantedPermissions.stream().map(Permission::getCode).toList();
    }
}
