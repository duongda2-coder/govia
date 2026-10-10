package com.govia.audit.agent.service;

import com.govia.audit.agent.config.AgentProperties;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Job "Gợi ý AI" theo lich (G4, muc M3) - lich RIENG cua module agent (Spring @Scheduled), KHONG them timer vao BPMN
 * Flowable. Mac dinh 06:00 thu 2-6 (ngoai gio lam viec), doi qua GOVIA_AGENT_SCHEDULE_CRON; tat qua
 * GOVIA_AGENT_SCHEDULE_ENABLED=false (bean nay khong duoc tao, he thong khong co lich nao).
 *
 * <p>Moi nguoi dung dang hoat dong co quyen AUDIT.AGENT.VIEW duoc chay rieng VOI DUNG QUYEN CUA HO: dung lai
 * SecurityContext + TenantContext giong het JwtAuthenticationFilter (vai tro + quyen tinh nhu luc dang nhap, wildcard
 * "*" cua SUPER_ADMIN = moi quyen) - nen goi y chi dua tren du lieu nguoi do duoc xem tren man hinh nghiep vu.
 * Chi doc o bang nhan su/vai tro; khong tao phien dang nhap, khong sinh token.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "govia.agent.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentSuggestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgentSuggestionScheduler.class);
    static final String AGENT_PERMISSION = "AUDIT.AGENT.VIEW";

    private final AgentSuggestionService suggestionService;
    private final AgentProperties agentProperties;
    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeRepository employeeRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AgentSuggestionScheduler(AgentSuggestionService suggestionService, AgentProperties agentProperties, TenantRepository tenantRepository,
                                    UserAccountRepository userAccountRepository, UserRoleRepository userRoleRepository, RoleRepository roleRepository,
                                    RolePermissionRepository rolePermissionRepository, PermissionRepository permissionRepository,
                                    EmployeeRepository employeeRepository) {
        this.suggestionService = suggestionService;
        this.agentProperties = agentProperties;
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Scheduled(cron = "${govia.agent.schedule.cron:0 0 6 * * MON-FRI}")
    public void runScheduled() {
        runAll();
    }

    /** Chay cho moi nguoi dung cua moi tenant dang hoat dong; tra ve so nguoi dung da chay. Khong chay chong 2 lan. */
    public int runAll() {
        if (!agentProperties.isEnabled() || !running.compareAndSet(false, true)) {
            return 0;
        }
        long start = System.currentTimeMillis();
        int users = 0;
        int suggestions = 0;
        try {
            for (Tenant tenant : tenantRepository.findAll()) {
                if (tenant.getStatus() != TenantStatus.ACTIVE) {
                    continue;
                }
                for (UserAccount user : userAccountRepository.findByTenantId(tenant.getId())) {
                    if (user.getStatus() != UserStatus.ACTIVE) {
                        continue;
                    }
                    CurrentUserPrincipal principal = principal(tenant.getId(), user);
                    if (!principal.permissions().contains(AGENT_PERMISSION)) {
                        continue;
                    }
                    try {
                        suggestions += runAs(principal);
                        users++;
                    } catch (RuntimeException e) {
                        log.warn("Goi y AI cho {} bi loi: {}", user.getUsername(), e.getMessage());
                    }
                }
            }
        } finally {
            running.set(false);
        }
        log.info("Goi y AI theo lich: {} nguoi dung, {} goi y, {} ms", users, suggestions, System.currentTimeMillis() - start);
        return users;
    }

    /** Dat dung ngu canh bao mat cua 1 nguoi dung (nhu JwtAuthenticationFilter) roi chay cac kiem tra; xong thi tra lai
     * ngu canh cu cua thread (job theo lich: rong; quan tri bam "chay cho moi nguoi": phien cua quan tri). */
    int runAs(CurrentUserPrincipal principal) {
        SecurityContext previous = SecurityContextHolder.getContext();
        UUID previousTenant = TenantContext.getTenantId();
        String previousUser = TenantContext.getCurrentUser();
        List<GrantedAuthority> authorities = new ArrayList<>();
        principal.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        principal.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        SecurityContextHolder.setContext(context);
        TenantContext.setTenantId(principal.tenantId());
        TenantContext.setCurrentUser(principal.username());
        try {
            return suggestionService.generate(principal);
        } finally {
            SecurityContextHolder.setContext(previous);
            TenantContext.setTenantId(previousTenant);
            TenantContext.setCurrentUser(previousUser);
        }
    }

    /** Phien (vai tro + quyen) ma job dung cho 1 nguoi dung - de quan tri/test doi chieu voi luc dang nhap. */
    public CurrentUserPrincipal principalOf(UUID tenantId, UUID userId) {
        return principal(tenantId, userAccountRepository.findById(userId).orElseThrow());
    }

    /** Vai tro + quyen tinh giong AuthService luc dang nhap (chi doc). */
    CurrentUserPrincipal principal(UUID tenantId, UserAccount user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<String> roles = userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).map(r -> r.getCode()).orElse(null))
                .filter(Objects::nonNull).toList();
        List<UUID> permissionIds = userRoles.stream()
                .flatMap(ur -> rolePermissionRepository.findByRoleId(ur.getRoleId()).stream())
                .map(RolePermission::getPermissionId).distinct().toList();
        List<Permission> granted = permissionRepository.findAllById(permissionIds);
        List<String> permissions = granted.stream().anyMatch(p -> "*".equals(p.getCode()))
                ? permissionRepository.findAll().stream().map(Permission::getCode).toList()
                : granted.stream().map(Permission::getCode).toList();
        String employeeCode = user.getEmployeeId() == null ? null
                : employeeRepository.findById(user.getEmployeeId()).map(Employee::getEmployeeCode).orElse(null);
        return new CurrentUserPrincipal(user.getId(), user.getUsername(), tenantId, employeeCode, roles, permissions, null);
    }
}
