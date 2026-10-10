package com.govia.audit.agent.tools;

import com.govia.audit.dgcl.AuditDgclController;
import com.govia.audit.dgcl.AuditDgclDto.Line;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.dgcl.DgclAppendix;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool CHI DOC cua A6 Chat luong (G4) tren phan he "Đánh giá chất lượng" (DGCL). Giong cac tool G2/G3: chi goi
 * lai method cua AuditDgclController san co - giu nguyen @PreAuthorize AUDIT.DGCL.VIEW va kiem tra "chi thanh vien
 * doan / NSD kiem soat DGCL" trong service. Khong luu, khong xac nhan, khong kiem soat phieu nao.
 */
@Component
public class AgentQualityToolsService {

    private static final int MAX_ENGAGEMENTS = 30;
    private static final int MAX_LINES = 60;
    private static final int EXCERPT = 220;
    /** Chenh diem trung binh cua 1 nguoi cham so voi trung binh chung tu muc nay tro len thi canh bao. */
    static final double VARIANCE_ALERT_POINTS = 10.0;

    private final AuditDgclController dgclController;
    private final AgentWorkToolsService workTools;

    public AgentQualityToolsService(AuditDgclController dgclController, AgentWorkToolsService workTools) {
        this.dgclController = dgclController;
        this.workTools = workTools;
    }

    /** Cuoc kiem toan nguoi dung xem duoc tren man DGCL (thanh vien doan, hoac NSD "Kiểm soát ĐGCL" thay tat ca). */
    public List<AuditEngagementResponse> engagements(CurrentUserPrincipal principal) {
        return dgclController.listEngagements(principal).data();
    }

    public AuditEngagementResponse resolveEngagement(CurrentUserPrincipal principal, String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("Thieu ma cuoc kiem toan (engagement)");
        }
        String r = ref.trim();
        return engagements(principal).stream()
                .filter(e -> r.equalsIgnoreCase(e.code()) || r.equals(String.valueOf(e.id())))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AGENT_ENGAGEMENT_NOT_FOUND",
                        "Khong tim thay cuoc kiem toan '" + r + "' trong danh sach Danh gia chat luong ban duoc xem", HttpStatus.NOT_FOUND));
    }

    public List<SubjectRow> subjects(CurrentUserPrincipal principal, AuditEngagementResponse engagement) {
        return dgclController.listSubjects(engagement.id(), principal).data();
    }

    public Sheet sheet(CurrentUserPrincipal principal, AuditEngagementResponse engagement, String subjectKey, DgclAppendix appendix) {
        return dgclController.getSheet(engagement.id(), subjectKey, appendix, principal).data();
    }

    /** Thanh vien theo subjectKey, ma NV hoac ten (khong dau, khong phan biet hoa thuong); "TEAM"/"doan" = dong ca doan. */
    public SubjectRow resolveSubject(List<SubjectRow> subjects, String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("Thieu thanh vien (subject): ma NV, ten hoac TEAM cho ca doan");
        }
        String r = ref.trim();
        String n = com.govia.audit.agent.service.AgentText.normalize(r);
        return subjects.stream()
                .filter(s -> r.equalsIgnoreCase(s.subjectKey())
                        || (s.team() && (n.equals("team") || n.contains("doan") || n.contains("cuoc kiem toan")))
                        || r.equalsIgnoreCase(s.employeeCode())
                        || (s.employeeName() != null && com.govia.audit.agent.service.AgentText.normalize(s.employeeName()).equals(n)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AGENT_DGCL_SUBJECT_NOT_FOUND",
                        "Khong tim thay thanh vien '" + r + "' trong doan; dung ma NV, ho ten hoac TEAM", HttpStatus.NOT_FOUND));
    }

    // ------------------------------------------------------------------ get_dgcl_overview

    /** Khong co engagement: danh sach CKT xem duoc + so phieu da cham/xac nhan; co engagement: tung thanh vien. */
    public Map<String, Object> getOverview(CurrentUserPrincipal principal, String engagementRef) {
        if (engagementRef == null || engagementRef.isBlank()) {
            List<AuditEngagementResponse> list = engagements(principal);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalEngagements", list.size());
            result.put("engagements", list.stream().limit(MAX_ENGAGEMENTS).map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", e.code());
                m.put("name", e.name());
                m.put("unit", e.auditObjectUnitName());
                m.put("year", e.year());
                try {
                    List<SubjectRow> subjects = subjects(principal, e);
                    m.put("subjects", subjects.size());
                    m.put("pl01fConfirmed", subjects.stream().filter(SubjectRow::pl01fConfirmed).count());
                    m.put("notEvaluated", subjects.stream().filter(s -> s.pl01fScore() == null).count());
                } catch (RuntimeException ex) {
                    m.put("subjects", null);
                }
                return m;
            }).toList());
            result.put("truncated", list.size() > MAX_ENGAGEMENTS);
            return result;
        }
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        List<SubjectRow> subjects = subjects(principal, engagement);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", engagement.code());
        result.put("engagementName", engagement.name());
        result.put("unit", engagement.auditObjectUnitName());
        result.put("subjects", subjects.stream().map(AgentQualityToolsService::slimSubject).toList());
        result.put("notSavedPl01a", names(subjects, s -> s.pl01aScore() == null));
        result.put("notSavedPl01b", names(subjects, s -> s.pl01bScore() == null));
        result.put("notSavedPl01f", names(subjects, s -> s.pl01fScore() == null));
        result.put("savedButNotConfirmed", subjects.stream().filter(s -> (s.pl01aScore() != null && !s.pl01aConfirmed())
                || (s.pl01bScore() != null && !s.pl01bConfirmed()) || (s.pl01fScore() != null && !s.pl01fConfirmed()))
                .map(AgentQualityToolsService::subjectLabel).toList());
        result.put("confirmedNotControlled", subjects.stream().filter(s -> s.confirmedCount() > s.controlledCount())
                .map(AgentQualityToolsService::subjectLabel).toList());
        return result;
    }

    // ------------------------------------------------------------------ get_dgcl_sheet

    public Map<String, Object> getSheet(CurrentUserPrincipal principal, String engagementRef, String subjectRef, String appendixRef,
                                        Boolean issuesOnly) {
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        SubjectRow subject = resolveSubject(subjects(principal, engagement), subjectRef);
        DgclAppendix appendix = appendix(appendixRef);
        Sheet sheet = sheet(principal, engagement, subject.subjectKey(), appendix);
        boolean onlyIssues = !Boolean.FALSE.equals(issuesOnly);
        List<Line> scorable = applicableLines(sheet);
        List<Line> nonCompliant = scorable.stream().filter(l -> l.nonCompliant() || (l.violationCount() != null && l.violationCount() > 0)).toList();
        List<Line> unscored = scorable.stream().filter(l -> appendix != DgclAppendix.PL01F && !l.compliant() && !l.nonCompliant()).toList();
        List<Line> shown = onlyIssues ? concat(nonCompliant, unscored) : scorable;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", engagement.code());
        result.put("subject", subjectLabel(subject));
        result.put("appendix", appendix.name());
        result.put("saved", sheet.saved());
        result.put("confirmed", sheet.confirmed());
        result.put("confirmedBy", sheet.confirmedBy());
        result.put("controlled", sheet.controlled());
        result.put("controlledBy", sheet.controlledBy());
        result.put("evaluator", sheet.evaluatorName());
        if (sheet.summary() != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("requiredCount", sheet.summary().requiredCount());
            s.put("compliantCount", sheet.summary().compliantCount());
            s.put("nonCompliantCount", sheet.summary().nonCompliantCount());
            s.put("score", round(sheet.summary().score()));
            s.put("bonusPoints", round(sheet.summary().bonusPoints()));
            s.put("penaltyPoints", round(sheet.summary().penaltyPoints()));
            s.put("classification", sheet.summary().classification());
            result.put("summary", s);
        }
        result.put("applicableCriteria", scorable.size());
        result.put("nonCompliantCount", nonCompliant.size());
        result.put("unscoredCount", unscored.size());
        result.put("lines", shown.stream().limit(MAX_LINES).map(AgentQualityToolsService::slimLine).toList());
        result.put("truncated", shown.size() > MAX_LINES);
        return result;
    }

    // ------------------------------------------------------------------ get_engagement_dossier

    /** Ho so doan lam can cu cham: moc thoi gian, tien do cong viec 3 giai doan, TTSS, kien nghi. Phan nao nguoi
     * dung khong co quyen/khong thuoc phan cong thi ghi ro, khong lam hong ca tool. */
    public Map<String, Object> getDossier(CurrentUserPrincipal principal, String engagementRef) {
        AuditEngagementResponse e = resolveEngagement(principal, engagementRef);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", e.code());
        result.put("engagementName", e.name());
        result.put("unit", e.auditObjectUnitName());
        result.put("year", e.year());
        result.put("status", e.status());
        result.put("teamLead", e.teamLeadEmployeeName());
        result.put("decisionNumber", e.decisionNumber());
        result.put("decisionDate", e.decisionDate());
        Map<String, Object> timeline = new LinkedHashMap<>();
        timeline.put("planning", e.planningStartDate() + " -> " + e.planningEndDate());
        timeline.put("fieldwork", e.fieldworkStartDate() + " -> " + e.fieldworkEndDate());
        timeline.put("report", e.reportStartDate() + " -> " + e.reportEndDate());
        result.put("timeline", timeline);
        result.put("objective", excerpt(e.objective()));
        result.put("scope", excerpt(e.scope()));
        List<String> notAvailable = new ArrayList<>();
        try {
            Map<String, Object> work = workTools.getEngagementWorkItems(principal, e.code(), null, null);
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("total", work.get("total"));
            w.put("countByStatus", work.get("countByStatus"));
            w.put("countByPhase", work.get("countByPhase"));
            result.put("workItems", w);
        } catch (AccessDeniedException | BusinessException ex) {
            notAvailable.add("Tiến độ công việc CBKT/THKT/DCKT");
        }
        try {
            Map<String, Object> ttss = workTools.getTtssSummary(principal, e.code());
            Map<String, Object> t = new LinkedHashMap<>();
            for (String k : List.of("total", "material", "withoutTeamRecommendation", "countByApprovalStatus")) {
                t.put(k, ttss.get(k));
            }
            result.put("ttss", t);
        } catch (AccessDeniedException | BusinessException ex) {
            notAvailable.add("TTSS");
        }
        result.put("recommendations", workTools.safeRecommendations(e.id()).size());
        result.put("notAvailable", notAvailable);
        return result;
    }

    // ------------------------------------------------------------------ get_dgcl_evaluator_variance

    /** Diem da luu theo NGUOI CHAM tren moi CKT xem duoc (loc theo nam): so phieu, trung binh, min/max moi phu luc;
     * nguoi cham co trung binh lech >= 10 diem so voi trung binh chung thi danh dau de nguoi kiem soat xem lai. */
    public Map<String, Object> getEvaluatorVariance(CurrentUserPrincipal principal, Integer year) {
        record Score(String evaluator, DgclAppendix appendix, double value, String engagement) {
        }
        List<Score> scores = new ArrayList<>();
        List<AuditEngagementResponse> list = engagements(principal).stream()
                .filter(e -> year == null || Objects.equals(year, e.year()))
                .limit(MAX_ENGAGEMENTS).toList();
        for (AuditEngagementResponse e : list) {
            List<SubjectRow> subjects;
            try {
                subjects = subjects(principal, e);
            } catch (RuntimeException ex) {
                continue;
            }
            for (SubjectRow s : subjects) {
                if (s.team()) {
                    continue;
                }
                List<String> evaluators = s.evaluatorNames() == null || s.evaluatorNames().isBlank() ? List.of("(Chưa rõ người chấm)")
                        : Arrays.stream(s.evaluatorNames().split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
                for (String ev : evaluators) {
                    add(scores, ev, DgclAppendix.PL01A, s.pl01aScore(), e.code(), Score::new);
                    add(scores, ev, DgclAppendix.PL01B, s.pl01bScore(), e.code(), Score::new);
                    add(scores, ev, DgclAppendix.PL01F, s.pl01fScore(), e.code(), Score::new);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("engagementsScanned", list.size());
        result.put("alertThresholdPoints", VARIANCE_ALERT_POINTS);
        List<Map<String, Object>> byAppendix = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        for (DgclAppendix a : DgclAppendix.values()) {
            List<Score> ofA = scores.stream().filter(s -> s.appendix() == a).toList();
            if (ofA.isEmpty()) {
                continue;
            }
            double overall = ofA.stream().mapToDouble(Score::value).average().orElse(0);
            Map<String, List<Score>> byEvaluator = ofA.stream().collect(Collectors.groupingBy(Score::evaluator, LinkedHashMap::new, Collectors.toList()));
            List<Map<String, Object>> evaluators = byEvaluator.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(en -> {
                        double avg = en.getValue().stream().mapToDouble(Score::value).average().orElse(0);
                        double diff = avg - overall;
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("evaluator", en.getKey());
                        m.put("sheets", en.getValue().size());
                        m.put("average", round(avg));
                        m.put("min", round(en.getValue().stream().mapToDouble(Score::value).min().orElse(0)));
                        m.put("max", round(en.getValue().stream().mapToDouble(Score::value).max().orElse(0)));
                        m.put("diffFromOverall", round(diff));
                        if (Math.abs(diff) >= VARIANCE_ALERT_POINTS && byEvaluator.size() > 1) {
                            alerts.add(a.name() + ": " + en.getKey() + " chấm trung bình " + round(avg) + " (chung " + round(overall) + ", lệch "
                                    + round(diff) + " điểm, " + en.getValue().size() + " phiếu)");
                        }
                        return m;
                    }).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appendix", a.name());
            m.put("sheets", ofA.size());
            m.put("overallAverage", round(overall));
            m.put("evaluators", evaluators);
            byAppendix.add(m);
        }
        result.put("byAppendix", byAppendix);
        result.put("alerts", alerts);
        return result;
    }

    // ------------------------------------------------------------------ dung chung (ca AgentQualityDraftService)

    /** Tieu chi can cham cua 1 phieu: bo dong tieu de/tong hop; PL01A/B chi lay dong "tick" (dung bo tieu chi dang
     * hien san tren phieu cho thanh vien do) hoac dong da cham; PL01F lay dong tru diem/cong diem. */
    public static List<Line> applicableLines(Sheet sheet) {
        Set<String> segments = sheet.segmentCodes() == null ? Set.of() : Arrays.stream(sheet.segmentCodes().split("[,;\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        if (sheet.appendix() == DgclAppendix.PL01F) {
            return sheet.lines().stream().filter(l -> PL01F_INPUT_KINDS.contains(l.kind())).toList();
        }
        return sheet.lines().stream()
                .filter(l -> !l.header() && l.kind() == null) // kind != null = dong tong hop IV-VI / gian doan
                .filter(l -> l.tick() || l.required() || l.compliant() || l.nonCompliant())
                .filter(l -> sheet.appendix() != DgclAppendix.PL01B || sheet.team() || segments.isEmpty() || l.segment() == null
                        || "CHUNG".equalsIgnoreCase(l.segment()) || segments.contains(l.segment().toUpperCase(Locale.ROOT))
                        || l.compliant() || l.nonCompliant())
                .toList();
    }

    /** Dong PL01F nguoi cham nhap (so loi / tich): tru diem chat luong, diem cong, diem tru. */
    private static final Set<String> PL01F_INPUT_KINDS = Set.of("DEDUCTION", "BONUS", "PENALTY");

    public static DgclAppendix appendix(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("Thieu phu luc (appendix): PL01A, PL01B hoac PL01F");
        }
        try {
            return DgclAppendix.valueOf(ref.trim().toUpperCase(Locale.ROOT).replace(" ", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("appendix phai la PL01A, PL01B hoac PL01F");
        }
    }

    public static String subjectLabel(SubjectRow s) {
        if (s.team()) {
            return "Cả đoàn (" + s.engagementCode() + ")";
        }
        return (s.role() == null ? "" : s.role() + " ") + s.employeeName() + (s.employeeCode() == null ? "" : " - " + s.employeeCode());
    }

    public static Map<String, Object> slimLine(Line l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", l.key());
        m.put("stt", l.stt());
        m.put("content", excerpt(l.content()));
        if (l.segment() != null) {
            m.put("segment", l.segment());
        }
        m.put("result", l.nonCompliant() ? "KHONG_TUAN_THU" : l.compliant() ? "TUAN_THU" : l.checked() ? "DA_TICH" : "CHUA_CHAM");
        if (l.violationCount() != null && l.violationCount() > 0) {
            m.put("violationCount", l.violationCount());
        }
        if (l.detail() != null && !l.detail().isBlank()) {
            m.put("detail", excerpt(l.detail()));
        }
        if (l.evaluatorName() != null) {
            m.put("evaluator", l.evaluatorName());
        }
        return m;
    }

    private static Map<String, Object> slimSubject(SubjectRow s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subject", subjectLabel(s));
        m.put("subjectKey", s.subjectKey());
        m.put("segments", s.segmentCodes());
        m.put("pl01a", score(s.pl01aScore(), s.pl01aConfirmed()));
        m.put("pl01b", score(s.pl01bScore(), s.pl01bConfirmed()));
        m.put("pl01f", score(s.pl01fScore(), s.pl01fConfirmed()));
        m.put("bonusPoints", s.bonusPoints());
        m.put("penaltyPoints", s.penaltyPoints());
        m.put("classification", s.classification());
        m.put("controlled", s.controlledCount() + "/" + s.confirmedCount());
        m.put("evaluators", s.evaluatorNames());
        m.put("controllers", s.controllerNames());
        return m;
    }

    private static String score(BigDecimal value, boolean confirmed) {
        if (value == null) {
            return "chưa chấm";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + (confirmed ? " (đã xác nhận)" : " (chưa xác nhận)");
    }

    private static List<String> names(List<SubjectRow> subjects, java.util.function.Predicate<SubjectRow> filter) {
        return subjects.stream().filter(filter).map(AgentQualityToolsService::subjectLabel).toList();
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> r = new ArrayList<>(a);
        b.stream().filter(x -> !r.contains(x)).forEach(r::add);
        return r;
    }

    private interface ScoreFactory<S> {
        S create(String evaluator, DgclAppendix appendix, double value, String engagement);
    }

    private static <S> void add(List<S> target, String evaluator, DgclAppendix appendix, BigDecimal value, String engagement, ScoreFactory<S> factory) {
        if (value != null) {
            target.add(factory.create(evaluator, appendix, value.doubleValue(), engagement));
        }
    }

    public static Double round(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() > EXCERPT ? t.substring(0, EXCERPT) + "..." : t;
    }

}
