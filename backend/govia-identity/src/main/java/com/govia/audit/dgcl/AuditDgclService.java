package com.govia.audit.dgcl;

import com.govia.audit.dgcl.AuditDgclDto.Capability;
import com.govia.audit.dgcl.AuditDgclDto.Line;
import com.govia.audit.dgcl.AuditDgclDto.LineInput;
import com.govia.audit.dgcl.AuditDgclDto.SaveRequest;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.dgcl.AuditDgclDto.Summary;
import com.govia.audit.dgcl.DgclCriteriaCatalog.Item;
import com.govia.audit.dgcl.DgclScoring.Calc;
import com.govia.audit.dgcl.DgclScoring.ComplianceResult;
import com.govia.audit.dgcl.DgclScoring.LineValues;
import com.govia.audit.dgcl.DgclScoring.QualityResult;
import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.planengagement.dto.AuditEngagementGroupMemberResponse;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.service.AuditEngagementService;
import com.govia.audit.planengagement.service.AuditEngagementTeamService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Phan he "Đánh giá chất lượng" (DGCL_CN.xlsx, sheet logic): sau khi ket thuc CKT, NSD duoc tich "Thực hiện ĐGCL"
 * o man hinh KNDN cham diem tung thanh vien doan + ca cuoc kiem toan theo 3 phu luc PL01A/PL01B/PL01F; NSD duoc tich
 * "Kiểm soát ĐGCL" khoa/mo khoa ket qua. Quyen thao tac lay tu KNDN (khong phai permission cua role) theo dung dac ta;
 * permission AUDIT.DGCL.VIEW/EXPORT chi mo man hinh/xuat PL04B1.
 */
@Service
public class AuditDgclService {

    /** subjectKey cua dong "Cuộc kiểm toán" (danh gia ca doan, PL4B1 muc II). */
    public static final String TEAM_SUBJECT = "TEAM";
    public static final String ATTACHMENT_ENTITY = "AUDIT_DGCL_LINE";
    static final String TEAM_NAME = "Cuộc kiểm toán";
    static final String ROLE_TEAM_LEAD = "TD";
    static final String ROLE_MEMBER = "TV";

    private final AuditDgclEvaluationRepository evaluationRepository;
    private final AuditDgclEvaluationLineRepository lineRepository;
    private final DgclCriteriaCatalog catalog;
    private final AuditEngagementService engagementService;
    private final AuditEngagementTeamService teamService;
    private final EmployeeRepository employeeRepository;
    private final AuditEmployeeCapabilityRepository capabilityRepository;
    private final AuditLogService auditLogService;

    public AuditDgclService(AuditDgclEvaluationRepository evaluationRepository, AuditDgclEvaluationLineRepository lineRepository,
                            DgclCriteriaCatalog catalog, AuditEngagementService engagementService, AuditEngagementTeamService teamService,
                            EmployeeRepository employeeRepository, AuditEmployeeCapabilityRepository capabilityRepository,
                            AuditLogService auditLogService) {
        this.evaluationRepository = evaluationRepository;
        this.lineRepository = lineRepository;
        this.catalog = catalog;
        this.engagementService = engagementService;
        this.teamService = teamService;
        this.employeeRepository = employeeRepository;
        this.capabilityRepository = capabilityRepository;
        this.auditLogService = auditLogService;
    }

    /** Nguoi dang thao tac (Employee cua tai khoan dang nhap) + 2 co KNDN. */
    record Actor(String name, boolean canEvaluate, boolean canControl) {
    }

    /** Doi tuong duoc danh gia (1 dong man hinh ngoai). */
    record Subject(String key, UUID employeeId, String employeeCode, String name, String segmentCodes, String role) {
        boolean team() {
            return TEAM_SUBJECT.equals(key);
        }
    }

    // ===================== Man hinh 1: danh sach CKT =====================

    /** test 29.9: thanh vien doan nao chi thay/danh gia CKT cua doan do (truong doan, thanh vien nhom, to giam sat - dung
     * dinh nghia "được phân công" cua {@link AuditEngagementService#listAssignedToCurrentUser}); NSD duoc tich "Kiểm soát ĐGCL"
     * o KNDN (kiem soat moi doan) hoac co quyen AUDIT.PLAN_ENGAGEMENT.VIEW_ALL thi thay tat ca. */
    @Transactional(readOnly = true)
    public List<AuditEngagementResponse> listEngagements(CurrentUserPrincipal principal) {
        return actor(principal).canControl() ? engagementService.list() : engagementService.listAssignedToCurrentUser(principal);
    }

    @Transactional(readOnly = true)
    public Capability capability(CurrentUserPrincipal principal) {
        Actor actor = actor(principal);
        return new Capability(actor.canEvaluate(), actor.canControl());
    }

    // ===================== Man hinh 2: thanh vien doan + diem =====================

    @Transactional(readOnly = true)
    public List<SubjectRow> listSubjects(UUID engagementId, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        return listSubjects(engagementId);
    }

    private List<SubjectRow> listSubjects(UUID engagementId) {
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        UUID tenantId = TenantContext.getTenantId();
        Map<String, Map<DgclAppendix, AuditDgclEvaluation>> evaluations = new HashMap<>();
        for (AuditDgclEvaluation e : evaluationRepository.findByTenantIdAndEngagementId(tenantId, engagementId)) {
            evaluations.computeIfAbsent(e.getSubjectKey(), k -> new EnumMap<>(DgclAppendix.class)).put(e.getAppendix(), e);
        }
        return subjects(engagement).stream()
                .map(s -> toSubjectRow(engagement, s, evaluations.getOrDefault(s.key(), Map.of())))
                .toList();
    }

    // ===================== Man hinh 3: phieu PL01A/PL01B/PL01F =====================

    @Transactional(readOnly = true)
    public Sheet getSheet(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Optional<AuditDgclEvaluation> evaluation = findEvaluation(engagementId, subjectKey, appendix);
        return toSheet(engagement, subject, appendix, evaluation.orElse(null), actor(principal));
    }

    @Transactional
    public Sheet saveSheet(UUID engagementId, String subjectKey, DgclAppendix appendix, SaveRequest request, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Actor actor = actor(principal);
        requireEvaluator(actor);
        AuditDgclEvaluation evaluation = findEvaluation(engagementId, subjectKey, appendix)
                .orElseGet(() -> newEvaluation(engagementId, subject, appendix));
        requireEditable(evaluation);

        Map<String, Item> itemsByKey = catalog.items(appendix).stream().collect(Collectors.toMap(Item::key, Function.identity()));
        if (evaluation.getId() == null) {
            evaluation = evaluationRepository.save(evaluation);
        }
        UUID evaluationId = evaluation.getId();
        Map<String, AuditDgclEvaluationLine> existing = lineRepository.findByEvaluationId(evaluationId).stream()
                .collect(Collectors.toMap(AuditDgclEvaluationLine::getItemKey, Function.identity()));
        for (LineInput input : request.lines()) {
            Item item = itemsByKey.get(input.key());
            if (item == null || item.isHeader() || item.isFooter()) {
                continue;
            }
            if (input.compliant() && input.nonCompliant()) {
                throw new BusinessException("AUDIT_DGCL_BOTH_COMPLIANT", "Dong " + label(item) + ": khong the tich dong thoi Tuan thu va Khong tuan thu");
            }
            AuditDgclEvaluationLine line = existing.get(input.key());
            if (isEmpty(input)) {
                if (line != null) {
                    lineRepository.delete(line);
                }
                continue;
            }
            if (line == null) {
                line = new AuditDgclEvaluationLine();
                line.setTenantId(evaluation.getTenantId());
                line.setEvaluationId(evaluationId);
                line.setItemKey(input.key());
            }
            if (applyInput(line, input)) {
                line.setEvaluatorName(actor.name());
            }
            lineRepository.save(line);
        }
        lineRepository.flush();

        evaluation.setEvaluatorName(actor.name());
        recalculate(evaluation, subject);
        evaluationRepository.save(evaluation);
        if (appendix != DgclAppendix.PL01F) {
            // Diem PL01F da luu dung B1/B2 cua PL01A/PL01B: tinh lai de cot PL01F/xep loai man hinh ngoai khong bi cu (test 30.9).
            findEvaluation(engagementId, subjectKey, DgclAppendix.PL01F).ifPresent(f -> {
                recalculate(f, subject);
                evaluationRepository.save(f);
            });
        }
        auditLogService.record("AuditDgclEvaluation", evaluationId, AuditAction.UPDATE,
                "Danh gia chat luong " + appendix + " - " + subject.name() + " - CKT " + engagement.code());
        return toSheet(engagement, subject, appendix, evaluation, actor);
    }

    /** Nut 1 "Xác nhận hoàn thành ĐGCL": diem duoc day ra man hinh ngoai. */
    @Transactional
    public Sheet confirm(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Actor actor = actor(principal);
        requireEvaluator(actor);
        AuditDgclEvaluation evaluation = findEvaluation(engagementId, subjectKey, appendix)
                .orElseThrow(() -> new BusinessException("AUDIT_DGCL_NOT_EVALUATED", "Phu luc chua duoc danh gia, hay nhap va luu truoc khi xac nhan"));
        requireEditable(evaluation);
        if (appendix == DgclAppendix.PL01F) {
            for (DgclAppendix dependency : List.of(DgclAppendix.PL01A, DgclAppendix.PL01B)) {
                boolean confirmed = findEvaluation(engagementId, subjectKey, dependency).map(AuditDgclEvaluation::isConfirmed).orElse(false);
                if (!confirmed) {
                    throw new BusinessException("AUDIT_DGCL_DEPENDENCY_NOT_CONFIRMED",
                            "Can xac nhan hoan thanh " + dependency + " truoc khi xac nhan PL01F (diem B1/B2 lay tu " + dependency + ")");
                }
            }
        }
        recalculate(evaluation, subject);
        if (appendix != DgclAppendix.PL01F && evaluation.getScore() == null) {
            throw new BusinessException("AUDIT_DGCL_NOTHING_REQUIRED", "Chua tich dong 'Nội dung thực hiện' nao, khong tinh duoc diem");
        }
        evaluation.setConfirmed(true);
        evaluation.setConfirmedBy(actor.name());
        evaluation.setConfirmedAt(Instant.now());
        evaluationRepository.save(evaluation);
        auditLogService.record("AuditDgclEvaluation", evaluation.getId(), AuditAction.UPDATE,
                "Xac nhan hoan thanh DGCL " + appendix + " - " + subject.name() + " - CKT " + engagement.code());
        return toSheet(engagement, subject, appendix, evaluation, actor);
    }

    /** Nut 2 "Hủy xác nhận hoàn thành": diem o man hinh ngoai de trong. */
    @Transactional
    public Sheet unconfirm(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Actor actor = actor(principal);
        requireEvaluator(actor);
        AuditDgclEvaluation evaluation = confirmedEvaluationOrThrow(engagementId, subjectKey, appendix);
        requireNotControlled(evaluation);
        if (appendix != DgclAppendix.PL01F
                && findEvaluation(engagementId, subjectKey, DgclAppendix.PL01F).map(AuditDgclEvaluation::isConfirmed).orElse(false)) {
            throw new BusinessException("AUDIT_DGCL_DEPENDENT_CONFIRMED", "PL01F dang dung diem cua " + appendix + ", hay huy xac nhan PL01F truoc");
        }
        evaluation.setConfirmed(false);
        evaluation.setConfirmedBy(null);
        evaluation.setConfirmedAt(null);
        evaluationRepository.save(evaluation);
        auditLogService.record("AuditDgclEvaluation", evaluation.getId(), AuditAction.UPDATE,
                "Huy xac nhan DGCL " + appendix + " - " + subject.name() + " - CKT " + engagement.code());
        return toSheet(engagement, subject, appendix, evaluation, actor);
    }

    /** Nut 3 "Kiểm soát ĐGCL": khoa phieu, NSD danh gia khong sua/huy xac nhan duoc nua. */
    @Transactional
    public Sheet control(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Actor actor = actor(principal);
        requireController(actor);
        AuditDgclEvaluation evaluation = confirmedEvaluationOrThrow(engagementId, subjectKey, appendix);
        if (evaluation.isControlled()) {
            throw new BusinessException("AUDIT_DGCL_ALREADY_CONTROLLED", "Phu luc da duoc kiem soat");
        }
        evaluation.setControlled(true);
        evaluation.setControlledBy(actor.name());
        evaluation.setControlledAt(Instant.now());
        evaluationRepository.save(evaluation);
        auditLogService.record("AuditDgclEvaluation", evaluation.getId(), AuditAction.UPDATE,
                "Kiem soat DGCL " + appendix + " - " + subject.name() + " - CKT " + engagement.code());
        return toSheet(engagement, subject, appendix, evaluation, actor);
    }

    /** Nut 4 "Hủy kiểm soát ĐGCL": tra lai quyen danh gia. */
    @Transactional
    public Sheet uncontrol(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        Subject subject = subjectOrThrow(engagement, subjectKey);
        Actor actor = actor(principal);
        requireController(actor);
        AuditDgclEvaluation evaluation = findEvaluation(engagementId, subjectKey, appendix)
                .filter(AuditDgclEvaluation::isControlled)
                .orElseThrow(() -> new BusinessException("AUDIT_DGCL_NOT_CONTROLLED", "Phu luc chua duoc kiem soat"));
        evaluation.setControlled(false);
        evaluation.setControlledBy(null);
        evaluation.setControlledAt(null);
        evaluationRepository.save(evaluation);
        auditLogService.record("AuditDgclEvaluation", evaluation.getId(), AuditAction.UPDATE,
                "Huy kiem soat DGCL " + appendix + " - " + subject.name() + " - CKT " + engagement.code());
        return toSheet(engagement, subject, appendix, evaluation, actor);
    }

    // ===================== Nut "PL04B1" =====================

    @Transactional(readOnly = true)
    public byte[] exportPl04b1(UUID engagementId, CurrentUserPrincipal principal) {
        requireAccess(engagementId, principal);
        AuditEngagementResponse engagement = engagementService.get(engagementId);
        return DgclPl04b1Writer.write(engagement, listSubjects(engagementId));
    }

    // ===================== Nut "Xuất PL01A/PL01B/PL01F" (test 30.9) =====================

    /** Xuat dung phieu dang xem (ke ca gia tri tich san khi chua luu) vao mau FORM_PL01A/B/F. */
    @Transactional(readOnly = true)
    public byte[] exportSheet(UUID engagementId, String subjectKey, DgclAppendix appendix, CurrentUserPrincipal principal) {
        Sheet sheet = getSheet(engagementId, subjectKey, appendix, principal);
        return DgclAppendixWriter.write(engagementService.get(engagementId), sheet);
    }

    // ===================== Helpers =====================

    private List<Subject> subjects(AuditEngagementResponse engagement) {
        List<AuditEngagementGroupMemberResponse> members = teamService.listMembersByEngagement(engagement.id());
        Map<UUID, List<AuditEngagementGroupMemberResponse>> byEmployee = members.stream()
                .collect(Collectors.groupingBy(AuditEngagementGroupMemberResponse::employeeId, LinkedHashMap::new, Collectors.toList()));

        List<Subject> result = new ArrayList<>();
        UUID leadId = engagement.teamLeadEmployeeId();
        if (leadId != null && !byEmployee.containsKey(leadId)) {
            result.add(new Subject(leadId.toString(), leadId, engagement.teamLeadEmployeeCode(), engagement.teamLeadEmployeeName(), null, ROLE_TEAM_LEAD));
        }
        byEmployee.forEach((employeeId, rows) -> {
            AuditEngagementGroupMemberResponse first = rows.get(0);
            String role = employeeId.equals(leadId) ? ROLE_TEAM_LEAD : ROLE_MEMBER;
            result.add(new Subject(employeeId.toString(), employeeId, first.employeeCode(), first.employeeName(), joinSegments(rows.stream()), role));
        });
        // Truong doan len dau (PL4B1 muc I.1), cac thanh vien giu thu tu nhom.
        result.sort((a, b) -> Boolean.compare(!ROLE_TEAM_LEAD.equals(a.role()), !ROLE_TEAM_LEAD.equals(b.role())));
        result.add(new Subject(TEAM_SUBJECT, null, null, TEAM_NAME, joinSegments(members.stream()), null));
        return result;
    }

    private String joinSegments(Stream<AuditEngagementGroupMemberResponse> members) {
        Set<String> codes = new LinkedHashSet<>();
        members.forEach(m -> Stream.of(m.businessSegment1Code(), m.businessSegment2Code(), m.businessSegment3Code())
                .filter(Objects::nonNull).forEach(codes::add));
        return codes.isEmpty() ? null : String.join(",", codes.stream().sorted().toList());
    }

    private Subject subjectOrThrow(AuditEngagementResponse engagement, String subjectKey) {
        return subjects(engagement).stream()
                .filter(s -> s.key().equals(subjectKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AUDIT_DGCL_SUBJECT_NOT_FOUND", "Khong tim thay thanh vien trong doan kiem toan", HttpStatus.NOT_FOUND));
    }

    private SubjectRow toSubjectRow(AuditEngagementResponse engagement, Subject subject, Map<DgclAppendix, AuditDgclEvaluation> evaluations) {
        AuditDgclEvaluation a = evaluations.get(DgclAppendix.PL01A);
        AuditDgclEvaluation b = evaluations.get(DgclAppendix.PL01B);
        AuditDgclEvaluation f = evaluations.get(DgclAppendix.PL01F);
        int confirmed = (int) evaluations.values().stream().filter(AuditDgclEvaluation::isConfirmed).count();
        int controlled = (int) evaluations.values().stream().filter(AuditDgclEvaluation::isControlled).count();
        String evaluators = distinctJoin(evaluations.values().stream().map(AuditDgclEvaluation::getEvaluatorName));
        String controllers = distinctJoin(evaluations.values().stream().filter(AuditDgclEvaluation::isControlled).map(AuditDgclEvaluation::getControlledBy));
        // test 30.9 muc 1: diem lay tu phieu da Lưu ben trong (khong doi den khi xac nhan), co confirmed de man hinh phan biet.
        return new SubjectRow(subject.key(), subject.team(), engagement.code(), subject.employeeId(), subject.employeeCode(), subject.name(),
                subject.segmentCodes(), subject.role(), savedScore(a), savedScore(b), savedScore(f),
                f == null ? null : f.getBonusPoints(), f == null ? null : f.getPenaltyPoints(), f == null ? null : f.getClassification(),
                isConfirmed(a), isConfirmed(b), isConfirmed(f), confirmed, controlled, evaluators, controllers);
    }

    private BigDecimal savedScore(AuditDgclEvaluation evaluation) {
        return evaluation == null ? null : evaluation.getScore();
    }

    private boolean isConfirmed(AuditDgclEvaluation evaluation) {
        return evaluation != null && evaluation.isConfirmed();
    }

    private String distinctJoin(Stream<String> values) {
        String joined = values.filter(Objects::nonNull).distinct().collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private Sheet toSheet(AuditEngagementResponse engagement, Subject subject, DgclAppendix appendix, AuditDgclEvaluation evaluation, Actor actor) {
        List<Item> items = catalog.items(appendix);
        Map<String, AuditDgclEvaluationLine> stored = evaluation == null || evaluation.getId() == null ? Map.of()
                : lineRepository.findByEvaluationId(evaluation.getId()).stream()
                .collect(Collectors.toMap(AuditDgclEvaluationLine::getItemKey, Function.identity()));
        boolean exists = evaluation != null && evaluation.getId() != null;
        // Phieu chua luu lan nao: tu tich san NDTH + Tuân thủ cac tieu chi cot "tick" (test 29.9) de NSD ra soat lai.
        Map<String, LineValues> values = exists ? toValues(stored) : defaultValues(items, subject);

        Summary summary;
        Map<String, Calc> calc = Map.of();
        if (appendix == DgclAppendix.PL01F) {
            QualityResult q = quality(engagement.id(), subject.key(), values);
            calc = q.calcByKey();
            summary = new Summary(null, null, null, q.ratio(), q.grandTotal(), q.weightedMax(), q.bonusPoints(), q.penaltyPoints(),
                    classification(subject, q));
        } else {
            ComplianceResult c = DgclScoring.compliance(items, values);
            summary = new Summary(c.requiredCount(), c.compliantCount(), c.nonCompliantCount(), c.ratio(), c.score(), null, null, null, null);
        }

        List<Line> lines = new ArrayList<>(items.size());
        for (Item item : items) {
            AuditDgclEvaluationLine line = stored.get(item.key());
            LineValues v = values.getOrDefault(item.key(), LineValues.EMPTY);
            Calc c = calc.get(item.key());
            lines.add(new Line(item.key(), item.stt(), item.content(), item.isHeader(), item.segment(), item.kind(), item.rate(), item.isTick(),
                    v.required(), v.compliant(), v.nonCompliant(), v.checked(), line == null ? null : line.getViolationCount(), null,
                    line == null ? null : line.getDetail(), line == null ? null : line.getDocument(), line == null ? null : line.getNote(),
                    line == null ? null : line.getEvaluatorName(),
                    c == null ? null : c.max(), c == null ? null : c.deduction(), c == null ? null : c.points(), c == null ? null : c.ratio(),
                    attachmentEntityId(engagement.id(), subject.key(), appendix, item.key())));
        }

        boolean confirmed = evaluation != null && evaluation.isConfirmed();
        boolean controlled = evaluation != null && evaluation.isControlled();
        return new Sheet(engagement.id(), engagement.code(), subject.key(), subject.team(), subject.name(), subject.segmentCodes(), appendix,
                lines, summary, exists, evaluation == null ? null : evaluation.getEvaluatorName(),
                confirmed, evaluation == null ? null : evaluation.getConfirmedBy(), evaluation == null ? null : evaluation.getConfirmedAt(),
                controlled, evaluation == null ? null : evaluation.getControlledBy(), evaluation == null ? null : evaluation.getControlledAt(),
                actor.canEvaluate() && !confirmed && !controlled,
                actor.canEvaluate() && exists && !confirmed && !controlled,
                actor.canEvaluate() && confirmed && !controlled,
                actor.canControl() && confirmed && !controlled,
                actor.canControl() && controlled);
    }

    /** Gia tri tich san cua phieu chua luu: moi dong co "tick" = NDTH + Tuân thủ. PL01B chi tich san dong dung chung (CHUNG)
     * va dong thuoc mang nghiep vu cua doi tuong (vd lam GA, DP chi tich san tieu chi GA, DP; dong "Cuộc kiểm toán" = mang
     * cua ca doan) - cac mang khac van hien de xem nhung de trong. */
    private Map<String, LineValues> defaultValues(List<Item> items, Subject subject) {
        Set<String> segments = subject.segmentCodes() == null ? Set.of()
                : Stream.of(subject.segmentCodes().split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
        Map<String, LineValues> values = new HashMap<>();
        for (Item item : items) {
            if (!item.isTick()) {
                continue;
            }
            String segment = item.segment() == null ? null : item.segment().trim().toUpperCase();
            boolean applies = segment == null || Item.COMMON_SEGMENT.equals(segment) || segments.contains(segment);
            if (applies) {
                values.put(item.key(), new LineValues(true, true, false, false, null, null));
            }
        }
        return values;
    }

    /** Tinh lai diem luu tren phieu tu cac dong da luu. */
    private void recalculate(AuditDgclEvaluation evaluation, Subject subject) {
        Map<String, LineValues> values = toValues(lineRepository.findByEvaluationId(evaluation.getId()).stream()
                .collect(Collectors.toMap(AuditDgclEvaluationLine::getItemKey, Function.identity())));
        if (evaluation.getAppendix() == DgclAppendix.PL01F) {
            QualityResult q = quality(evaluation.getEngagementId(), evaluation.getSubjectKey(), values);
            evaluation.setScore(decimal(q.grandTotal()));
            evaluation.setBonusPoints(decimal(q.bonusPoints()));
            evaluation.setPenaltyPoints(decimal(q.penaltyPoints()));
            evaluation.setClassification(classification(subject, q));
        } else {
            ComplianceResult c = DgclScoring.compliance(catalog.items(evaluation.getAppendix()), values);
            evaluation.setScore(c.score() == null ? null : decimal(c.score()));
        }
    }

    private QualityResult quality(UUID engagementId, String subjectKey, Map<String, LineValues> values) {
        Double ratioA = findEvaluation(engagementId, subjectKey, DgclAppendix.PL01A).map(this::ratioOf).orElse(null);
        Double ratioB = findEvaluation(engagementId, subjectKey, DgclAppendix.PL01B).map(this::ratioOf).orElse(null);
        return DgclScoring.quality(catalog.items(DgclAppendix.PL01F), values, ratioA, ratioB);
    }

    private Double ratioOf(AuditDgclEvaluation evaluation) {
        return evaluation.getScore() == null ? null : evaluation.getScore().doubleValue() / 100;
    }

    private String classification(Subject subject, QualityResult q) {
        return subject.team() ? q.teamClassification()
                : DgclScoring.memberClassification(q.ratio() * 100, q.bonusPoints(), q.penaltyPoints());
    }

    private Map<String, LineValues> toValues(Map<String, AuditDgclEvaluationLine> lines) {
        Map<String, LineValues> values = new HashMap<>();
        lines.forEach((key, l) -> values.put(key, new LineValues(l.isRequired(), l.isCompliant(), l.isNonCompliant(), l.isChecked(),
                l.getViolationCount(), l.getMaxScore() == null ? null : l.getMaxScore().doubleValue())));
        return values;
    }

    /** Ghi input vao dong, tra ve true neu co thay doi (de cap nhat "User đánh giá" cua dong). Tich Tuan thu/Khong tuan thu
     * tu dong tich NDTH (dong khong thuoc noi dung thuc hien thi khong co khai niem tuan thu). */
    private boolean applyInput(AuditDgclEvaluationLine line, LineInput input) {
        boolean required = input.required() || input.compliant() || input.nonCompliant();
        boolean changed = line.isRequired() != required || line.isCompliant() != input.compliant() || line.isNonCompliant() != input.nonCompliant()
                || line.isChecked() != input.checked() || !Objects.equals(line.getViolationCount(), input.violationCount())
                || line.getMaxScore() != null || !Objects.equals(line.getDetail(), blankToNull(input.detail()))
                || !Objects.equals(line.getDocument(), blankToNull(input.document())) || !Objects.equals(line.getNote(), blankToNull(input.note()));
        line.setRequired(required);
        line.setCompliant(input.compliant());
        line.setNonCompliant(input.nonCompliant());
        line.setChecked(input.checked());
        line.setViolationCount(input.violationCount());
        // test 29.9: diem toi da I/II/III PL01F co dinh 100, khong nhap nua.
        line.setMaxScore(null);
        line.setDetail(blankToNull(input.detail()));
        line.setDocument(blankToNull(input.document()));
        line.setNote(blankToNull(input.note()));
        return changed;
    }

    private boolean isEmpty(LineInput input) {
        return !input.required() && !input.compliant() && !input.nonCompliant() && !input.checked() && input.violationCount() == null
                && blankToNull(input.detail()) == null && blankToNull(input.document()) == null
                && blankToNull(input.note()) == null;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String label(Item item) {
        return item.stt() == null || item.stt().isBlank() ? item.key() : item.stt();
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    static UUID attachmentEntityId(UUID engagementId, String subjectKey, DgclAppendix appendix, String itemKey) {
        return UUID.nameUUIDFromBytes(("DGCL:" + engagementId + ":" + subjectKey + ":" + appendix + ":" + itemKey).getBytes(StandardCharsets.UTF_8));
    }

    private Optional<AuditDgclEvaluation> findEvaluation(UUID engagementId, String subjectKey, DgclAppendix appendix) {
        return evaluationRepository.findByTenantIdAndEngagementIdAndSubjectKeyAndAppendix(TenantContext.getTenantId(), engagementId, subjectKey, appendix);
    }

    private AuditDgclEvaluation confirmedEvaluationOrThrow(UUID engagementId, String subjectKey, DgclAppendix appendix) {
        return findEvaluation(engagementId, subjectKey, appendix)
                .filter(AuditDgclEvaluation::isConfirmed)
                .orElseThrow(() -> new BusinessException("AUDIT_DGCL_NOT_CONFIRMED", "Phu luc chua duoc xac nhan hoan thanh DGCL"));
    }

    private AuditDgclEvaluation newEvaluation(UUID engagementId, Subject subject, DgclAppendix appendix) {
        AuditDgclEvaluation evaluation = new AuditDgclEvaluation();
        evaluation.setTenantId(TenantContext.getTenantId());
        evaluation.setEngagementId(engagementId);
        evaluation.setSubjectKey(subject.key());
        evaluation.setEmployeeId(subject.employeeId());
        evaluation.setAppendix(appendix);
        return evaluation;
    }

    private void requireNotControlled(AuditDgclEvaluation evaluation) {
        if (evaluation.isControlled()) {
            throw new BusinessException("AUDIT_DGCL_CONTROLLED", "Phu luc da duoc kiem soat, khong the danh gia tiep", HttpStatus.FORBIDDEN);
        }
    }

    private void requireEditable(AuditDgclEvaluation evaluation) {
        requireNotControlled(evaluation);
        if (evaluation.isConfirmed()) {
            throw new BusinessException("AUDIT_DGCL_CONFIRMED", "Phu luc da xac nhan hoan thanh, hay huy xac nhan truoc khi sua");
        }
    }

    private void requireAccess(UUID engagementId, CurrentUserPrincipal principal) {
        boolean allowed = listEngagements(principal).stream().anyMatch(e -> e.id().equals(engagementId));
        if (!allowed) {
            throw new BusinessException("AUDIT_DGCL_NOT_TEAM_MEMBER",
                    "Ban khong thuoc doan cua cuoc kiem toan nay nen khong duoc vao danh gia chat luong", HttpStatus.FORBIDDEN);
        }
    }

    private void requireEvaluator(Actor actor) {
        if (!actor.canEvaluate()) {
            throw new BusinessException("AUDIT_DGCL_NOT_EVALUATOR",
                    "Ban chua duoc tich 'Thuc hien DGCL' o man hinh Khai bao kha nang dam nhan (KNDN)", HttpStatus.FORBIDDEN);
        }
    }

    private void requireController(Actor actor) {
        if (!actor.canControl()) {
            throw new BusinessException("AUDIT_DGCL_NOT_CONTROLLER",
                    "Ban chua duoc tich 'Kiem soat DGCL' o man hinh Khai bao kha nang dam nhan (KNDN)", HttpStatus.FORBIDDEN);
        }
    }

    private Actor actor(CurrentUserPrincipal principal) {
        if (principal == null || principal.employeeCode() == null) {
            return new Actor(principal == null ? null : principal.username(), false, false);
        }
        UUID tenantId = TenantContext.getTenantId();
        Optional<Employee> employee = employeeRepository.findByTenantIdAndEmployeeCode(tenantId, principal.employeeCode());
        if (employee.isEmpty()) {
            return new Actor(principal.username(), false, false);
        }
        Optional<AuditEmployeeCapability> capability = capabilityRepository.findByTenantIdAndEmployeeId(tenantId, employee.get().getId());
        return new Actor(employee.get().getFullName(),
                capability.map(AuditEmployeeCapability::isDgclCapable).orElse(false),
                capability.map(AuditEmployeeCapability::isDgclControlCapable).orElse(false));
    }
}
