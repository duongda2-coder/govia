package com.govia.audit.planengagement.supervisionteam.service;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.service.AuditEngagementMonitoringService;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionCandidateResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionEvaluationResponse;
import com.govia.audit.planengagement.supervisionteam.dto.AuditSupervisionTeamMemberResponse;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionEvaluationRequest;
import com.govia.audit.planengagement.supervisionteam.dto.SaveSupervisionTeamRequest;
import com.govia.audit.planengagement.supervisionteam.entity.AuditSupervisionEvaluation;
import com.govia.audit.planengagement.supervisionteam.entity.AuditSupervisionTeamMember;
import com.govia.audit.planengagement.supervisionteam.repository.AuditSupervisionEvaluationRepository;
import com.govia.audit.planengagement.supervisionteam.repository.AuditSupervisionTeamMemberRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * "To giam sat" (sheet cung ten cua Tao CKT (4).xlsx): nguoi tao CKT chon thanh vien to giam sat
 * tu danh sach nhan vien co {@link AuditEmployeeCapability#isToGiamSatCapable()}; thanh vien duoc
 * chon co quyen XEM (khong sua) cuoc kiem toan do o cac man hinh "Quản lý đợt kiểm toán"/TTSS - xem
 * phan mo rong hasViewAll-style trong AuditEngagementMonitoringService/AuditTtssService - va tu ghi
 * danh gia cua CHINH MINH (khong duoc tich ho/tich cheo).
 */
@Service
public class AuditSupervisionTeamService {

    private static final String PERMISSION_VIEW_ALL = "AUDIT.PLAN_ENGAGEMENT.VIEW_ALL";

    private final AuditEngagementRepository engagementRepository;
    private final AuditSupervisionTeamMemberRepository teamMemberRepository;
    private final AuditSupervisionEvaluationRepository evaluationRepository;
    private final AuditEmployeeCapabilityRepository capabilityRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditEngagementMonitoringService monitoringService;
    private final AuditLogService auditLogService;

    public AuditSupervisionTeamService(AuditEngagementRepository engagementRepository,
                                        AuditSupervisionTeamMemberRepository teamMemberRepository,
                                        AuditSupervisionEvaluationRepository evaluationRepository,
                                        AuditEmployeeCapabilityRepository capabilityRepository,
                                        EmployeeRepository employeeRepository,
                                        UserAccountRepository userAccountRepository,
                                        AuditEngagementMonitoringService monitoringService,
                                        AuditLogService auditLogService) {
        this.engagementRepository = engagementRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.evaluationRepository = evaluationRepository;
        this.capabilityRepository = capabilityRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.monitoringService = monitoringService;
        this.auditLogService = auditLogService;
    }

    // ===================== "Chon to giam sat" =====================

    @Transactional(readOnly = true)
    public List<AuditSupervisionCandidateResponse> listCandidates(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        Set<UUID> capableEmployeeIds = capabilityRepository.findByTenantId(tenantId).stream()
                .filter(AuditEmployeeCapability::isToGiamSatCapable)
                .map(AuditEmployeeCapability::getEmployeeId)
                .collect(Collectors.toSet());
        List<Employee> capableEmployees = employeeRepository.findAllById(capableEmployeeIds).stream()
                .filter(e -> e.getTenantId().equals(tenantId))
                .toList();
        Map<UUID, String> usernames = usernamesByEmployeeIds(capableEmployees.stream().map(Employee::getId).collect(Collectors.toSet()));
        Set<UUID> selectedIds = teamMemberRepository.findByTenantIdAndEngagementId(tenantId, engagementId).stream()
                .map(AuditSupervisionTeamMember::getEmployeeId).collect(Collectors.toSet());
        return capableEmployees.stream()
                .sorted(Comparator.comparing(Employee::getFullName))
                .map(e -> new AuditSupervisionCandidateResponse(e.getId(), e.getEmployeeCode(), e.getFullName(),
                        usernames.get(e.getId()), selectedIds.contains(e.getId())))
                .toList();
    }

    /** Chi nguoi tao ra CKT (createdBy) moi duoc thay doi danh sach to giam sat - thay the TOAN BO. */
    @Transactional
    public List<AuditSupervisionTeamMemberResponse> saveTeam(UUID engagementId, SaveSupervisionTeamRequest request, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireCreator(engagement, principal);

        Set<UUID> capableIds = capabilityRepository.findByTenantId(tenantId).stream()
                .filter(AuditEmployeeCapability::isToGiamSatCapable)
                .map(AuditEmployeeCapability::getEmployeeId)
                .collect(Collectors.toSet());
        for (UUID employeeId : request.employeeIds()) {
            if (!capableIds.contains(employeeId)) {
                throw new BusinessException("EMPLOYEE_NOT_SUPERVISION_CAPABLE", "Nhan vien nay khong co kha nang dam nhiem to giam sat");
            }
        }

        teamMemberRepository.deleteByTenantIdAndEngagementId(tenantId, engagementId);
        for (UUID employeeId : new HashSet<>(request.employeeIds())) {
            AuditSupervisionTeamMember member = new AuditSupervisionTeamMember();
            member.setTenantId(tenantId);
            member.setEngagementId(engagementId);
            member.setEmployeeId(employeeId);
            teamMemberRepository.save(member);
        }

        auditLogService.record("AuditSupervisionTeamMember", engagementId, AuditAction.UPDATE,
                "Chon to giam sat cho CKT " + engagement.getCode() + ": " + request.employeeIds().size() + " thanh vien");
        return listTeam(engagementId);
    }

    @Transactional(readOnly = true)
    public List<AuditSupervisionTeamMemberResponse> listTeam(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        List<AuditSupervisionTeamMember> members = teamMemberRepository.findByTenantIdAndEngagementId(tenantId, engagementId);
        Map<UUID, Employee> employees = employeeRepository.findAllById(members.stream().map(AuditSupervisionTeamMember::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, String> usernames = usernamesByEmployeeIds(employees.keySet());
        return members.stream().map(m -> {
            Employee employee = employees.get(m.getEmployeeId());
            return new AuditSupervisionTeamMemberResponse(m.getId(), m.getEmployeeId(),
                    employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(),
                    usernames.get(m.getEmployeeId()));
        }).toList();
    }

    // ===================== "To giam sat thuc hien danh gia" =====================

    @Transactional(readOnly = true)
    public AuditSupervisionEvaluationResponse getMyEvaluation(UUID engagementId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        UUID employeeId = requireSupervisionMember(tenantId, engagementId, principal);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        String username = usernamesByEmployeeIds(Set.of(employeeId)).get(employeeId);
        return evaluationRepository.findByTenantIdAndEngagementIdAndEmployeeId(tenantId, engagementId, employeeId)
                .map(row -> toEvaluationResponse(row, employee, username))
                .orElseGet(() -> new AuditSupervisionEvaluationResponse(null, engagementId, employeeId,
                        employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(), username,
                        false, false, false, null, null));
    }

    /** "khong duoc tich ho, tich cheo": khong nhan employeeId tu client - luon ghi vao dong cua
     * CHINH nguoi goi (suy ra tu principal), evaluatedAt luon = thoi diem luu gan nhat. */
    @Transactional
    public AuditSupervisionEvaluationResponse saveMyEvaluation(UUID engagementId, SaveSupervisionEvaluationRequest request, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        UUID employeeId = requireSupervisionMember(tenantId, engagementId, principal);

        AuditSupervisionEvaluation row = evaluationRepository.findByTenantIdAndEngagementIdAndEmployeeId(tenantId, engagementId, employeeId)
                .orElseGet(() -> {
                    AuditSupervisionEvaluation created = new AuditSupervisionEvaluation();
                    created.setTenantId(tenantId);
                    created.setEngagementId(engagementId);
                    created.setEmployeeId(employeeId);
                    return created;
                });
        row.setProgress(request.progress());
        row.setContentAssured(request.contentAssured());
        row.setQualityAssured(request.qualityAssured());
        row.setNote(request.note());
        row.setEvaluatedAt(Instant.now());
        row = evaluationRepository.save(row);

        auditLogService.record("AuditSupervisionEvaluation", row.getId(), AuditAction.UPDATE,
                "Cap nhat danh gia to giam sat cho CKT " + engagement.getCode());
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        String username = usernamesByEmployeeIds(Set.of(employeeId)).get(employeeId);
        return toEvaluationResponse(row, employee, username);
    }

    /** "Ket qua danh gia cua to giam sat" - TAT CA dong (ke ca thanh vien chua danh gia lan nao,
     * hien thi rong), chi xem, danh cho ca to giam sat lan Truong/Pho KTNB/BKS (VIEW_ALL). */
    @Transactional(readOnly = true)
    public List<AuditSupervisionEvaluationResponse> listAllEvaluations(UUID engagementId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        if (!hasViewAll(principal)) {
            requireSupervisionMember(tenantId, engagementId, principal);
        }

        List<AuditSupervisionTeamMember> members = teamMemberRepository.findByTenantIdAndEngagementId(tenantId, engagementId);
        Map<UUID, Employee> employees = employeeRepository.findAllById(members.stream().map(AuditSupervisionTeamMember::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, String> usernames = usernamesByEmployeeIds(employees.keySet());
        Map<UUID, AuditSupervisionEvaluation> evaluationsByEmployee = evaluationRepository.findByTenantIdAndEngagementId(tenantId, engagementId)
                .stream().collect(Collectors.toMap(AuditSupervisionEvaluation::getEmployeeId, e -> e));

        return members.stream().map(m -> {
            Employee employee = employees.get(m.getEmployeeId());
            String username = usernames.get(m.getEmployeeId());
            AuditSupervisionEvaluation row = evaluationsByEmployee.get(m.getEmployeeId());
            if (row == null) {
                return new AuditSupervisionEvaluationResponse(null, engagementId, m.getEmployeeId(),
                        employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(), username,
                        false, false, false, null, null);
            }
            return toEvaluationResponse(row, employee, username);
        }).toList();
    }

    // ===================== "Man hinh quan ly to giam sat" =====================

    @Transactional(readOnly = true)
    public List<AuditEngagementMonitoringResponse> listMyEngagements(CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        UUID employeeId = resolveEmployeeId(tenantId, principal);
        if (employeeId == null) {
            return List.of();
        }
        List<UUID> engagementIds = teamMemberRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(AuditSupervisionTeamMember::getEngagementId).distinct().toList();
        return monitoringService.listByEngagementIds(engagementIds);
    }

    // ===================== Helpers =====================

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private void requireCreator(AuditEngagement engagement, CurrentUserPrincipal principal) {
        if (principal == null || principal.username() == null || !principal.username().equalsIgnoreCase(engagement.getCreatedBy())) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_CREATOR", "Chi nguoi tao cuoc kiem toan moi duoc chon to giam sat", HttpStatus.FORBIDDEN);
        }
    }

    /** Tra ve employeeId cua nguoi goi neu la thanh vien to giam sat cua engagementId, nguoc lai nem 403. */
    private UUID requireSupervisionMember(UUID tenantId, UUID engagementId, CurrentUserPrincipal principal) {
        UUID employeeId = resolveEmployeeId(tenantId, principal);
        if (employeeId == null || !teamMemberRepository.existsByTenantIdAndEngagementIdAndEmployeeId(tenantId, engagementId, employeeId)) {
            throw new BusinessException("AUDIT_NOT_SUPERVISION_MEMBER", "Ban khong thuoc to giam sat cua cuoc kiem toan nay", HttpStatus.FORBIDDEN);
        }
        return employeeId;
    }

    private boolean hasViewAll(CurrentUserPrincipal principal) {
        return principal != null && principal.permissions() != null && principal.permissions().contains(PERMISSION_VIEW_ALL);
    }

    private UUID resolveEmployeeId(UUID tenantId, CurrentUserPrincipal principal) {
        if (principal == null || principal.employeeCode() == null) {
            return null;
        }
        return employeeRepository.findByTenantIdAndEmployeeCode(tenantId, principal.employeeCode()).map(Employee::getId).orElse(null);
    }

    private Map<UUID, String> usernamesByEmployeeIds(Set<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findByEmployeeIdIn(employeeIds).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
    }

    private AuditSupervisionEvaluationResponse toEvaluationResponse(AuditSupervisionEvaluation row, Employee employee, String username) {
        return new AuditSupervisionEvaluationResponse(row.getId(), row.getEngagementId(), row.getEmployeeId(),
                employee == null ? null : employee.getEmployeeCode(), employee == null ? null : employee.getFullName(), username,
                row.isProgress(), row.isContentAssured(), row.isQualityAssured(), row.getNote(), row.getEvaluatedAt());
    }
}
