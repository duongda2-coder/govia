package com.govia.audit.planengagement.monitoring.service;

import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementTeamMemberDetailResponse;
import com.govia.audit.planengagement.monitoring.dto.ProgressStat;
import com.govia.audit.planengagement.monitoring.dto.TeamMemberScoringRequest;
import com.govia.audit.planengagement.monitoring.dto.TeamRankingUpdateRequest;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.service.AuditEngagementService;
import com.govia.audit.planengagement.service.AuditEngagementTeamService;
import com.govia.audit.planengagement.supervisionteam.repository.AuditSupervisionTeamMemberRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Man hinh "Quản lý đợt kiểm toán" (nguon: "Tao CKT (2).xlsx", sheet cung ten) - lop tren cua
 * {@link AuditEngagementService}/{@link AuditEngagementTeamService}: KHONG luu them du lieu CKT
 * hay thanh vien (tru 2 truong "user nhap" moi la teamRanking/score-ranking-note), chi tong hop
 * so lieu de hien thi giam sat + ap dung phan quyen theo dac ta:
 * "Trưởng KTNB, Phó TKTNB, Thành viên BKS" (permission AUDIT.PLAN_ENGAGEMENT.VIEW_ALL) xem tat ca
 * CKT; nhan vien phong ban (khong co quyen do) chi xem CKT ma minh la truong doan/truong nhom/
 * thanh vien.
 */
@Service
public class AuditEngagementMonitoringService {

    private static final String PERMISSION_VIEW_ALL = "AUDIT.PLAN_ENGAGEMENT.VIEW_ALL";

    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditWorkItemRepository workItemRepository;
    private final AuditTtssRecordRepository ttssRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditEngagementService engagementService;
    private final AuditEngagementTeamService teamService;
    private final AuditSupervisionTeamMemberRepository supervisionTeamMemberRepository;
    private final AuditLogService auditLogService;

    public AuditEngagementMonitoringService(AuditEngagementRepository engagementRepository, AuditEngagementGroupRepository groupRepository,
                                             AuditEngagementGroupMemberRepository memberRepository, AuditEngagementAssignmentRepository assignmentRepository,
                                             AuditWorkItemRepository workItemRepository, AuditTtssRecordRepository ttssRecordRepository,
                                             EmployeeRepository employeeRepository, AuditEngagementService engagementService,
                                             AuditEngagementTeamService teamService, AuditSupervisionTeamMemberRepository supervisionTeamMemberRepository,
                                             AuditLogService auditLogService) {
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.ttssRecordRepository = ttssRecordRepository;
        this.employeeRepository = employeeRepository;
        this.engagementService = engagementService;
        this.teamService = teamService;
        this.supervisionTeamMemberRepository = supervisionTeamMemberRepository;
        this.auditLogService = auditLogService;
    }

    // ===================== Man hinh 1: danh sach CKT + so lieu tong hop =====================

    @Transactional(readOnly = true)
    public List<AuditEngagementMonitoringResponse> list(CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditEngagementResponse> all = engagementService.list();
        if (all.isEmpty()) {
            return List.of();
        }

        List<UUID> allEngagementIds = all.stream().map(AuditEngagementResponse::id).toList();
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdIn(tenantId, allEngagementIds);
        Map<UUID, UUID> engagementIdByGroupId = groups.stream().collect(Collectors.toMap(AuditEngagementGroup::getId, AuditEngagementGroup::getAuditEngagementId));
        List<UUID> allGroupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> allMembers = allGroupIds.isEmpty() ? List.of() : memberRepository.findByTenantIdAndGroupIdIn(tenantId, allGroupIds);
        Map<UUID, List<AuditEngagementGroupMember>> membersByEngagement = new HashMap<>();
        for (AuditEngagementGroupMember member : allMembers) {
            UUID engagementId = engagementIdByGroupId.get(member.getGroupId());
            if (engagementId != null) {
                membersByEngagement.computeIfAbsent(engagementId, k -> new ArrayList<>()).add(member);
            }
        }

        List<AuditEngagementResponse> scoped = hasViewAll(principal) ? all
                : scopeByParticipation(all, groups, membersByEngagement, resolveEmployeeId(tenantId, principal));
        if (scoped.isEmpty()) {
            return List.of();
        }

        List<UUID> scopedIds = scoped.stream().map(AuditEngagementResponse::id).toList();
        List<AuditTtssRecord> ttssRecords = ttssRecordRepository.findByTenantIdAndEngagementIdIn(tenantId, scopedIds);
        Map<UUID, List<AuditTtssRecord>> ttssByEngagement = ttssRecords.stream().collect(Collectors.groupingBy(AuditTtssRecord::getEngagementId));

        return scoped.stream()
                .map(engagement -> buildMonitoringResponse(engagement, membersByEngagement.getOrDefault(engagement.id(), List.of()),
                        ttssByEngagement.getOrDefault(engagement.id(), List.of())))
                .toList();
    }

    /** "Xếp loại đoàn" - cot duy nhat cua man hinh 1 do user nhap truc tiep (khong co cong thuc). */
    @Transactional
    public AuditEngagementMonitoringResponse updateTeamRanking(UUID engagementId, TeamRankingUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        engagement.setTeamRanking(request.teamRanking());
        engagementRepository.save(engagement);
        auditLogService.record("AuditEngagement", engagementId, AuditAction.UPDATE, "Cap nhat xep loai doan CKT " + engagement.getCode());

        AuditEngagementResponse response = engagementService.get(engagementId);
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<AuditEngagementGroupMember> members = groups.isEmpty() ? List.of()
                : memberRepository.findByTenantIdAndGroupIdIn(tenantId, groups.stream().map(AuditEngagementGroup::getId).toList());
        List<AuditTtssRecord> ttssRecords = ttssRecordRepository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId);
        return buildMonitoringResponse(response, members, ttssRecords);
    }

    // ===================== "Chi tiết đoàn" cua "QL CKT quy trinh" (Tao CKT (4).xlsx) =====================

    /** 1 dong / CKT con cua 1 CKT quy trinh, cung cong thuc tong hop voi man hinh 1 nhung KHONG loc
     * theo tham gia (day la man hinh giam sat cap CKT quy trinh, da duoc chan quyen o controller). */
    @Transactional(readOnly = true)
    public List<AuditEngagementMonitoringResponse> listByProcessEngagement(UUID processEngagementId) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditEngagement> children = engagementRepository.findByTenantIdAndProcessEngagementIdOrderByCreatedAtAsc(tenantId, processEngagementId);
        if (children.isEmpty()) {
            return List.of();
        }
        List<UUID> childIds = children.stream().map(AuditEngagement::getId).toList();
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdIn(tenantId, childIds);
        Map<UUID, UUID> engagementIdByGroupId = groups.stream().collect(Collectors.toMap(AuditEngagementGroup::getId, AuditEngagementGroup::getAuditEngagementId));
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> allMembers = groupIds.isEmpty() ? List.of() : memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
        Map<UUID, List<AuditEngagementGroupMember>> membersByEngagement = new HashMap<>();
        for (AuditEngagementGroupMember member : allMembers) {
            UUID engagementId = engagementIdByGroupId.get(member.getGroupId());
            if (engagementId != null) {
                membersByEngagement.computeIfAbsent(engagementId, k -> new ArrayList<>()).add(member);
            }
        }
        List<AuditTtssRecord> ttssRecords = ttssRecordRepository.findByTenantIdAndEngagementIdIn(tenantId, childIds);
        Map<UUID, List<AuditTtssRecord>> ttssByEngagement = ttssRecords.stream().collect(Collectors.groupingBy(AuditTtssRecord::getEngagementId));

        return childIds.stream()
                .map(id -> buildMonitoringResponse(engagementService.get(id), membersByEngagement.getOrDefault(id, List.of()),
                        ttssByEngagement.getOrDefault(id, List.of())))
                .toList();
    }

    /** "Man hinh quan ly to giam sat" (sheet "To giam sat" cua Tao CKT (4).xlsx) - danh sach CKT ma
     * nhan vien dang dang nhap la thanh vien to giam sat, dung lai dung cong thuc tong hop voi man
     * hinh 1 nhung KHONG loc theo tham gia doan (da duoc AuditSupervisionTeamService loc san theo
     * to giam sat truoc khi goi vao day). */
    @Transactional(readOnly = true)
    public List<AuditEngagementMonitoringResponse> listByEngagementIds(List<UUID> engagementIds) {
        if (engagementIds.isEmpty()) {
            return List.of();
        }
        UUID tenantId = TenantContext.getTenantId();
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdIn(tenantId, engagementIds);
        Map<UUID, UUID> engagementIdByGroupId = groups.stream().collect(Collectors.toMap(AuditEngagementGroup::getId, AuditEngagementGroup::getAuditEngagementId));
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> allMembers = groupIds.isEmpty() ? List.of() : memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
        Map<UUID, List<AuditEngagementGroupMember>> membersByEngagement = new HashMap<>();
        for (AuditEngagementGroupMember member : allMembers) {
            UUID engagementId = engagementIdByGroupId.get(member.getGroupId());
            if (engagementId != null) {
                membersByEngagement.computeIfAbsent(engagementId, k -> new ArrayList<>()).add(member);
            }
        }
        List<AuditTtssRecord> ttssRecords = ttssRecordRepository.findByTenantIdAndEngagementIdIn(tenantId, engagementIds);
        Map<UUID, List<AuditTtssRecord>> ttssByEngagement = ttssRecords.stream().collect(Collectors.groupingBy(AuditTtssRecord::getEngagementId));

        return engagementIds.stream()
                .map(id -> buildMonitoringResponse(engagementService.get(id), membersByEngagement.getOrDefault(id, List.of()),
                        ttssByEngagement.getOrDefault(id, List.of())))
                .toList();
    }

    // ===================== Man hinh 2: "Chi tiết đoàn kiểm toán" =====================

    @Transactional(readOnly = true)
    public List<AuditEngagementTeamMemberDetailResponse> teamDetail(UUID engagementId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireViewAllowed(tenantId, engagement, principal);
        return computeTeamDetail(engagement);
    }

    /** "Điểm/Xếp loại/Ghi chú thành viên" - do user nhap truc tiep tren tung dong (khong co cong thuc). */
    @Transactional
    public AuditEngagementTeamMemberDetailResponse updateTeamMemberScoring(UUID engagementId, UUID memberId, TeamMemberScoringRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditEngagementGroupMember member = memberRepository.findById(memberId)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_MEMBER_NOT_FOUND", "Khong tim thay thanh vien", HttpStatus.NOT_FOUND));
        AuditEngagementGroup group = groupRepository.findById(member.getGroupId())
                .filter(g -> g.getTenantId().equals(tenantId) && g.getAuditEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_MEMBER_NOT_FOUND", "Khong tim thay thanh vien", HttpStatus.NOT_FOUND));

        member.setScore(request.score());
        member.setRanking(request.ranking());
        member.setNote(request.note());
        memberRepository.save(member);
        auditLogService.record("AuditEngagementGroupMember", memberId, AuditAction.UPDATE, "Cap nhat diem/xep loai thanh vien CKT " + engagement.getCode());

        return computeTeamDetail(engagement).stream()
                .filter(r -> r.memberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_MEMBER_NOT_FOUND", "Khong tim thay thanh vien", HttpStatus.NOT_FOUND));
    }

    // ===================== Helpers =====================

    private List<AuditEngagementTeamMemberDetailResponse> computeTeamDetail(AuditEngagement engagement) {
        UUID tenantId = engagement.getTenantId();
        List<AuditEngagementGroupMemberResponse> memberInfos = teamService.listMembersByEngagement(engagement.getId());
        if (memberInfos.isEmpty()) {
            return List.of();
        }

        List<UUID> memberIds = memberInfos.stream().map(AuditEngagementGroupMemberResponse::id).toList();
        Map<UUID, AuditEngagementGroupMember> memberEntities = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(AuditEngagementGroupMember::getId, m -> m));

        List<AuditTtssRecord> ttssRecords = ttssRecordRepository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagement.getId());
        Map<String, List<AuditTtssRecord>> ttssByPerformerName = ttssRecords.stream()
                .filter(r -> r.getTtssPerformerName() != null)
                .collect(Collectors.groupingBy(r -> normalizeName(r.getTtssPerformerName())));

        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId, memberIds);
        Map<UUID, List<AuditEngagementAssignment>> assignmentsByMember = assignments.stream()
                .collect(Collectors.groupingBy(AuditEngagementAssignment::getGroupMemberId));
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));

        List<AuditEngagementTeamMemberDetailResponse> result = new ArrayList<>();
        int stt = 1;
        for (AuditEngagementGroupMemberResponse info : memberInfos) {
            AuditEngagementGroupMember entity = memberEntities.get(info.id());
            List<AuditTtssRecord> memberTtss = ttssByPerformerName.getOrDefault(normalizeName(info.employeeName()), List.of());
            List<AuditEngagementAssignment> memberAssignments = assignmentsByMember.getOrDefault(info.id(), List.of());
            result.add(toDetailResponse(stt++, engagement, info, entity, memberTtss, memberAssignments, workItems));
        }
        return result;
    }

    private AuditEngagementTeamMemberDetailResponse toDetailResponse(int stt, AuditEngagement engagement, AuditEngagementGroupMemberResponse info,
                                                                       AuditEngagementGroupMember entity, List<AuditTtssRecord> ttss,
                                                                       List<AuditEngagementAssignment> assignments, Map<UUID, AuditWorkItem> workItems) {
        String roleTitle = roleTitle(engagement, info);
        String segments = joinSegments(info);
        int totalFindings = ttss.size();
        int ttssTypeCount = distinctFindingCodeCount(ttss, r -> true);
        int totalMaterialFindings = (int) ttss.stream().filter(AuditTtssRecord::isMaterial).count();
        int materialTtssTypeCount = distinctFindingCodeCount(ttss, AuditTtssRecord::isMaterial);
        int recommendationCount = (int) ttss.stream().filter(r -> r.getTeamRecommendationId() != null).count();

        ProgressStat cbkt = progressFor(assignments, workItems, AuditWorkPhase.CBKT, null);
        ProgressStat thktSample = progressFor(assignments, workItems, AuditWorkPhase.THKT, true);
        ProgressStat thktNoSample = progressFor(assignments, workItems, AuditWorkPhase.THKT, false);

        return new AuditEngagementTeamMemberDetailResponse(stt, info.id(), info.employeeId(), info.employeeCode(), info.employeeName(), roleTitle, segments,
                totalFindings, ttssTypeCount, totalMaterialFindings, materialTtssTypeCount, recommendationCount,
                cbkt, thktSample, thktNoSample,
                entity == null ? null : entity.getScore(), entity == null ? null : entity.getRanking(), entity == null ? null : entity.getNote());
    }

    private String roleTitle(AuditEngagement engagement, AuditEngagementGroupMemberResponse info) {
        if (info.employeeId().equals(engagement.getTeamLeadEmployeeId())) {
            return "Trưởng đoàn";
        }
        if (info.leaderEmployeeId() != null && info.employeeId().equals(info.leaderEmployeeId())) {
            return "Trưởng nhóm";
        }
        return "Thành viên";
    }

    private String joinSegments(AuditEngagementGroupMemberResponse info) {
        return Stream.of(info.businessSegment1Code(), info.businessSegment2Code(), info.businessSegment3Code())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private int distinctFindingCodeCount(List<AuditTtssRecord> ttss, Predicate<AuditTtssRecord> filter) {
        return (int) ttss.stream().filter(filter).map(AuditTtssRecord::getFindingCode).filter(Objects::nonNull).distinct().count();
    }

    /** hasSampleSelection == null => khong loc theo co mau/khong mau (dung cho CBKT). */
    private ProgressStat progressFor(List<AuditEngagementAssignment> assignments, Map<UUID, AuditWorkItem> workItems, AuditWorkPhase phase,
                                      Boolean hasSampleSelection) {
        int total = 0;
        int completed = 0;
        for (AuditEngagementAssignment assignment : assignments) {
            AuditWorkItem workItem = workItems.get(assignment.getWorkItemId());
            if (workItem == null || workItem.getPhase() != phase) {
                continue;
            }
            if (hasSampleSelection != null && workItem.isHasSampleSelection() != hasSampleSelection) {
                continue;
            }
            total++;
            if (assignment.getStatus() == AssignmentStatus.DONE) {
                completed++;
            }
        }
        return new ProgressStat(completed, total);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private AuditEngagementMonitoringResponse buildMonitoringResponse(AuditEngagementResponse engagement, List<AuditEngagementGroupMember> members,
                                                                       List<AuditTtssRecord> ttssRecords) {
        int memberCount = members.size();
        int businessSegmentCount = distinctSegmentCount(members);
        int totalFindings = ttssRecords.size();
        int totalMaterialFindings = (int) ttssRecords.stream().filter(AuditTtssRecord::isMaterial).count();
        int recommendationCount = (int) ttssRecords.stream().filter(r -> r.getTeamRecommendationId() != null).count();
        return new AuditEngagementMonitoringResponse(engagement, memberCount, businessSegmentCount, totalFindings, totalMaterialFindings, recommendationCount);
    }

    private int distinctSegmentCount(List<AuditEngagementGroupMember> members) {
        Set<UUID> segments = new HashSet<>();
        for (AuditEngagementGroupMember member : members) {
            if (member.getBusinessSegment1Id() != null) segments.add(member.getBusinessSegment1Id());
            if (member.getBusinessSegment2Id() != null) segments.add(member.getBusinessSegment2Id());
            if (member.getBusinessSegment3Id() != null) segments.add(member.getBusinessSegment3Id());
        }
        return segments.size();
    }

    private List<AuditEngagementResponse> scopeByParticipation(List<AuditEngagementResponse> all, List<AuditEngagementGroup> groups,
                                                                 Map<UUID, List<AuditEngagementGroupMember>> membersByEngagement, UUID currentEmployeeId) {
        if (currentEmployeeId == null) {
            return List.of();
        }
        Set<UUID> participantEngagementIds = new HashSet<>();
        for (AuditEngagementResponse engagement : all) {
            if (currentEmployeeId.equals(engagement.teamLeadEmployeeId())) {
                participantEngagementIds.add(engagement.id());
            }
        }
        for (AuditEngagementGroup group : groups) {
            if (currentEmployeeId.equals(group.getLeaderEmployeeId())) {
                participantEngagementIds.add(group.getAuditEngagementId());
            }
        }
        membersByEngagement.forEach((engagementId, members) -> {
            if (members.stream().anyMatch(m -> currentEmployeeId.equals(m.getEmployeeId()))) {
                participantEngagementIds.add(engagementId);
            }
        });
        supervisionTeamMemberRepository.findByTenantIdAndEmployeeId(TenantContext.getTenantId(), currentEmployeeId)
                .forEach(m -> participantEngagementIds.add(m.getEngagementId()));
        return all.stream().filter(e -> participantEngagementIds.contains(e.id())).toList();
    }

    private void requireViewAllowed(UUID tenantId, AuditEngagement engagement, CurrentUserPrincipal principal) {
        if (hasViewAll(principal)) {
            return;
        }
        UUID currentEmployeeId = resolveEmployeeId(tenantId, principal);
        if (currentEmployeeId != null) {
            if (currentEmployeeId.equals(engagement.getTeamLeadEmployeeId())) {
                return;
            }
            List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagement.getId());
            if (groups.stream().anyMatch(g -> currentEmployeeId.equals(g.getLeaderEmployeeId()))) {
                return;
            }
            List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
            List<AuditEngagementGroupMember> members = groupIds.isEmpty() ? List.of() : memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
            if (members.stream().anyMatch(m -> currentEmployeeId.equals(m.getEmployeeId()))) {
                return;
            }
            if (supervisionTeamMemberRepository.existsByTenantIdAndEngagementIdAndEmployeeId(tenantId, engagement.getId(), currentEmployeeId)) {
                return;
            }
        }
        throw new BusinessException("AUDIT_ENGAGEMENT_NOT_PARTICIPANT", "Ban khong tham gia cuoc kiem toan nay", HttpStatus.FORBIDDEN);
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

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }
}
