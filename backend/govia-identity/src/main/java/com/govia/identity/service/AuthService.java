package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.JwtTokenProvider;
import com.govia.core.security.UserSession;
import com.govia.core.security.UserSessionService;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.ActiveSessionInfo;
import com.govia.identity.dto.ChangePasswordRequest;
import com.govia.identity.dto.LoginOutcome;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResolveRequest;
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
    private final UserSessionService sessionService;
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
                        UserSessionService sessionService,
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
        this.sessionService = sessionService;
        this.auditLogService = auditLogService;
    }

    /** Neu tai khoan dang co phien ACTIVE o noi khac, KHONG dang nhap ngay - tra ve CONFLICT kem
     * danh sach phien dang hoat dong + 1 pendingToken ngan han de FE hoi nguoi dung "da phien cu"
     * hay "dang nhap song song" roi goi tiep resolveLogin(). */
    @Transactional
    public LoginOutcome login(LoginRequest request, String deviceInfo, String ipAddress) {
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

        List<UserSession> activeSessions = sessionService.findActive(tenant.getId(), user.getId());
        if (!activeSessions.isEmpty()) {
            List<ActiveSessionInfo> sessions = activeSessions.stream()
                    .map(s -> new ActiveSessionInfo(s.getDeviceInfo(), s.getIpAddress(), s.getCreatedAt()))
                    .toList();
            return LoginOutcome.conflict(sessions, tokenProvider.generatePendingLoginToken(user.getId()));
        }

        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);

        return LoginOutcome.success(createSessionAndBuildResponse(tenant, user, deviceInfo, ipAddress));
    }

    /** Buoc 2 sau khi FE hien hoi "da phien cu hay dang nhap song song" (chi goi duoc khi da qua
     * login() va nhan CONFLICT - khong can nhap lai mat khau nho pendingToken). */
    @Transactional
    public LoginResponse resolveLogin(LoginResolveRequest request, String deviceInfo, String ipAddress) {
        UUID userId;
        try {
            userId = tokenProvider.parsePendingLoginToken(request.pendingToken());
        } catch (Exception e) {
            throw new BusinessException("INVALID_PENDING_TOKEN", "Yeu cau dang nhap da het han, vui long dang nhap lai", HttpStatus.UNAUTHORIZED);
        }
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay tai khoan", HttpStatus.UNAUTHORIZED));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant khong ton tai", HttpStatus.UNAUTHORIZED));

        if ("KICK_OTHERS".equals(request.action())) {
            sessionService.revokeAll(tenant.getId(), user.getId());
        }

        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);

        return createSessionAndBuildResponse(tenant, user, deviceInfo, ipAddress);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parseClaims(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token khong hop le", HttpStatus.UNAUTHORIZED);
        }
        String jti = claims.getId();
        if (jti == null || !sessionService.isActive(jti)) {
            throw new BusinessException("SESSION_REVOKED", "Phien dang nhap da bi thu hoi", HttpStatus.UNAUTHORIZED);
        }
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "User khong ton tai", HttpStatus.UNAUTHORIZED));
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant khong ton tai", HttpStatus.UNAUTHORIZED));

        sessionService.touch(jti);
        return buildLoginResponse(tenant, user, jti);
    }

    /** Dang xuat CHINH XAC phien hien tai (thu hoi jti) - bat buoc phai co, neu khong session cu
     * van con ACTIVE trong DB sau khi user bam "Dang xuat" roi dang nhap lai ngay se bi he thong
     * hieu nham la dang nhap dong thoi voi CHINH minh va hoi "da phien cu" mot cach vo ly. */
    @Transactional
    public void logout(String jti) {
        if (jti != null) {
            sessionService.revokeByJti(jti);
        }
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

    /** Tao 1 UserSession MOI (1 jti moi) - dung cho that su dang nhap (login/resolveLogin), MOI
     * lan goi la 1 "phien" moi tinh vao so luong dang nhap dong thoi cua tai khoan. */
    private LoginResponse createSessionAndBuildResponse(Tenant tenant, UserAccount user, String deviceInfo, String ipAddress) {
        String jti = UUID.randomUUID().toString();
        sessionService.create(tenant.getId(), user.getId(), jti, deviceInfo, ipAddress);
        return buildLoginResponse(tenant, user, jti);
    }

    /** Sinh cap token gan voi 1 jti CO SAN (dung cho /refresh - khong duoc tao them phien moi moi
     * lan refresh access token sap het han, neu khong se bi hieu nham la dang nhap dong thoi lien
     * tuc tren chinh thiet bi cua minh). */
    private LoginResponse buildLoginResponse(Tenant tenant, UserAccount user, String jti) {
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
                employeeCode, roleCodes, permissionCodes, jti);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername(), jti);

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
