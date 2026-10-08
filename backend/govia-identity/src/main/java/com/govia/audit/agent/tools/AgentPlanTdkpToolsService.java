package com.govia.audit.agent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.service.AgentText;
import com.govia.audit.khkt.khnsnam.controller.AuditKhnsNamController;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.khkt.th.controller.AuditKhktThConfirmedController;
import com.govia.audit.khkt.th.controller.AuditKhktThController;
import com.govia.audit.khkt.th.dto.AuditKhktThRowResponse;
import com.govia.audit.khkt.thang.controller.AuditKhktThangController;
import com.govia.audit.khkt.thang.dto.AuditKhktThangRowResponse;
import com.govia.audit.tdkp.branch.AuditTdkpBranchController;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoAllController;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoKhController;
import com.govia.audit.tdkp.resolution.AuditTdkpResolutionController;
import com.govia.audit.tdkp.unitrec.AuditTdkpUnitRecommendationController;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Tool CHI DOC cua cac agent G3: A2 Ke hoach (KHKT TH/TH2, KHKT thang, KHNS phan bo) va A5 Theo doi khac
 * phuc (5 danh sach kien nghi/nghi quyet TDKP). Giong AgentWorkToolsService: chi goi lai method cua
 * controller nghiep vu san co nen giu nguyen @PreAuthorize; khong ghi gi.
 */
@Component
public class AgentPlanTdkpToolsService {

    private static final int EXCERPT = 300;
    private static final int MAX_ITEMS = 50;
    private static final int DEFAULT_DUE_SOON_DAYS = 30;

    /** 1 dong kien nghi/nghi quyet TDKP da chuan hoa ve cung 1 dang - nguon nao cung vay. */
    public record TdkpItem(String source, UUID id, String code, String unit, String content, LocalDate deadline,
                           String status, String statusLabel, String deadlineState, String note) {

        public boolean done() {
            return "DONE".equals(status);
        }

        /** So ngay qua han (duong) hoac con lai (am) tinh den hom nay; null neu khong co han. */
        public Long daysOverdue() {
            return deadline == null ? null : ChronoUnit.DAYS.between(deadline, LocalDate.now());
        }
    }

    /** 1 danh sach TDKP tra cuu duoc - loader goi controller (bi chan neu thieu quyen man hinh do). */
    public record TdkpSource(String key, String label, Supplier<List<TdkpItem>> loader) {
    }

    private final Map<String, TdkpSource> tdkpSources = new LinkedHashMap<>();
    private final AuditKhktThController thController;
    private final AuditKhktThConfirmedController thConfirmedController;
    private final AuditKhktThangController thangController;
    private final AuditKhnsNamController khnsNamController;
    private final ObjectMapper objectMapper;

    public AgentPlanTdkpToolsService(AuditTdkpCeoAllController ceoAllController, AuditTdkpCeoKhController ceoKhController,
                                     AuditTdkpBranchController branchController, AuditTdkpResolutionController resolutionController,
                                     AuditTdkpUnitRecommendationController unitRecommendationController,
                                     AuditKhktThController thController, AuditKhktThConfirmedController thConfirmedController,
                                     AuditKhktThangController thangController, AuditKhnsNamController khnsNamController,
                                     ObjectMapper objectMapper) {
        this.thController = thController;
        this.thConfirmedController = thConfirmedController;
        this.thangController = thangController;
        this.khnsNamController = khnsNamController;
        this.objectMapper = objectMapper;
        tdkpSources.put("CEO_ALL", new TdkpSource("CEO_ALL", "Kiến nghị của KTNB đối với HĐTV, TGĐ", () -> ceoAllController.list().data().stream()
                .map(r -> new TdkpItem("CEO_ALL", r.id(), r.reportNumber(), r.executingUnitName(), r.content(), r.deadline(),
                        name(r.status()), r.statusLabel(), r.deadlineState(), r.evaluation()))
                .toList()));
        tdkpSources.put("CEO_KH", new TdkpSource("CEO_KH", "Kiến nghị HĐTV, TGĐ - Khối/phòng thực hiện", () -> ceoKhController.list().data().stream()
                .map(r -> new TdkpItem("CEO_KH", r.id(), r.reportNumber(), r.executingUnitName(), r.content(), r.deadline(),
                        name(r.status()), r.statusLabel(), r.deadlineState(), r.evaluation()))
                .toList()));
        tdkpSources.put("BRANCH", new TdkpSource("BRANCH", "Kiến nghị chi nhánh", () -> branchController.listRecommendations().data().stream()
                .map(r -> new TdkpItem("BRANCH", r.id(), r.managementCode(), r.branchName(), r.content(), r.deadline(),
                        name(r.status()), r.statusLabel(), r.deadlineState(),
                        r.defectCount() > 0 ? "Sai sót đã chỉnh sửa " + r.defectDoneCount() + "/" + r.defectCount() : r.evaluation()))
                .toList()));
        tdkpSources.put("RESOLUTION", new TdkpSource("RESOLUTION", "Nghị quyết", () -> resolutionController.list().data().stream()
                .map(r -> new TdkpItem("RESOLUTION", r.id(), r.code(), r.unitName(), r.summary() != null ? r.summary() : r.content(),
                        r.completionDeadline(), name(r.progressStatus()), r.progressStatusLabel(), r.resolutionState(), r.implementation()))
                .toList()));
        tdkpSources.put("UNIT", new TdkpSource("UNIT", "Kiến nghị của KTNB đối với đơn vị", () -> unitRecommendationController.list().data().stream()
                .map(r -> new TdkpItem("UNIT", r.id(), r.code(), r.unitName(), r.content(), r.deadline(),
                        name(r.status()), r.statusLabel(), r.deadlineState(), r.implementation()))
                .toList()));
    }

    public List<String> tdkpSourceKeys() {
        return List.copyOf(tdkpSources.keySet());
    }

    // ------------------------------------------------------------------ A5 theo doi khac phuc

    /** get_tdkp_overview - tong hop 5 danh sach TDKP; danh sach nao nguoi dung khong co quyen thi ghi ro,
     * khong lam hong ca tool (moi danh sach la 1 man hinh voi quyen rieng). */
    public Map<String, Object> getTdkpOverview(Integer dueWithinDays) {
        int soon = dueWithinDays == null || dueWithinDays <= 0 ? DEFAULT_DUE_SOON_DAYS : dueWithinDays;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asOf", LocalDate.now());
        result.put("dueSoonWindowDays", soon);
        List<Map<String, Object>> sources = new ArrayList<>();
        List<String> noPermission = new ArrayList<>();
        for (TdkpSource source : tdkpSources.values()) {
            List<TdkpItem> items;
            try {
                items = source.loader().get();
            } catch (AccessDeniedException e) {
                noPermission.add(source.label());
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", source.key());
            m.put("label", source.label());
            m.put("total", items.size());
            m.put("countByStatus", items.stream().collect(Collectors.groupingBy(i -> i.statusLabel() == null ? "Chưa cập nhật" : i.statusLabel(),
                    LinkedHashMap::new, Collectors.counting())));
            m.put("overdueNotDone", items.stream().filter(i -> overdue(i, 0)).count());
            m.put("dueSoonNotDone", items.stream().filter(i -> dueSoon(i, soon)).count());
            m.put("topUnitsOverdue", items.stream().filter(i -> overdue(i, 0))
                    .collect(Collectors.groupingBy(i -> i.unit() == null ? "-" : i.unit(), Collectors.counting()))
                    .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                    .map(e -> e.getKey() + ": " + e.getValue()).toList());
            sources.add(m);
        }
        result.put("sources", sources);
        result.put("noPermissionFor", noPermission);
        return result;
    }

    /** list_tdkp_items - kien nghi/nghi quyet cua 1 danh sach, loc theo tinh trang han va don vi. */
    public Map<String, Object> listTdkpItems(String sourceKey, String state, String unit, Integer dueWithinDays, Integer limit) {
        TdkpSource source = source(sourceKey);
        int soon = dueWithinDays == null || dueWithinDays <= 0 ? DEFAULT_DUE_SOON_DAYS : dueWithinDays;
        String s = state == null ? "OPEN" : state.trim().toUpperCase(Locale.ROOT);
        if (!List.of("OVERDUE", "DUE_SOON", "OPEN", "ALL").contains(s)) {
            throw new IllegalArgumentException("state phai la OVERDUE, DUE_SOON, OPEN hoac ALL");
        }
        List<String> unitWords = AgentText.tokens(unit).stream().filter(w -> w.length() >= 2).toList();
        List<TdkpItem> items = source.loader().get().stream()
                .filter(i -> switch (s) {
                    case "OVERDUE" -> overdue(i, 0);
                    case "DUE_SOON" -> dueSoon(i, soon);
                    case "OPEN" -> !i.done();
                    default -> true;
                })
                .filter(i -> unitWords.isEmpty() || unitWords.stream().allMatch(w -> AgentText.containsPhrase(AgentText.normalize(i.unit()), w)))
                .sorted(Comparator.comparing(TdkpItem::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int max = limit == null || limit <= 0 ? 30 : Math.min(limit, MAX_ITEMS);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", source.label());
        result.put("state", s);
        result.put("total", items.size());
        result.put("items", items.stream().limit(max).map(AgentPlanTdkpToolsService::slim).toList());
        result.put("truncated", items.size() > max);
        return result;
    }

    /** Lay dong theo id (cho soan thu don doc); ids rong = moi dong chua xong da qua han hoac sap den han. */
    public List<TdkpItem> itemsForReminder(String sourceKey, Collection<UUID> ids, int dueWithinDays) {
        List<TdkpItem> items = source(sourceKey).loader().get();
        if (ids != null && !ids.isEmpty()) {
            Set<UUID> wanted = new HashSet<>(ids);
            return items.stream().filter(i -> wanted.contains(i.id())).toList();
        }
        return items.stream().filter(i -> overdue(i, 0) || dueSoon(i, dueWithinDays)).toList();
    }

    public String sourceLabel(String sourceKey) {
        return source(sourceKey).label();
    }

    // ------------------------------------------------------------------ A2 ke hoach

    /** get_plan_overview - KHKT tong hop (TH) va TH2 da xac nhan cua 1 nam: so doi tuong, phan bo theo xep
     * loai rui ro, so da phe duyet, va so doi tuong theo thang trong KHKT thang. */
    public Map<String, Object> getPlanOverview(Integer year) {
        List<AuditKhktThRowResponse> th = thController.list(year).data();
        List<AuditKhktThRowResponse> th2 = thConfirmedController.list(year).data();
        List<AuditKhktThangRowResponse> thang = thangController.list(year).data();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("thCandidates", th.size());
        result.put("th2Confirmed", th2.size());
        result.put("th2Approved", th2.stream().filter(r -> "APPROVED".equals(name(r.approvalStatus()))).count());
        result.put("th2ByRank", th2.stream().collect(Collectors.groupingBy(r -> r.rankLabel() == null ? "-" : r.rankLabel(), TreeMap::new, Collectors.counting())));
        result.put("th2ByAuditOrSupervision", th2.stream().collect(Collectors.groupingBy(r -> r.khktgsAfterAdjustment() == null ? "-" : name(r.khktgsAfterAdjustment()),
                TreeMap::new, Collectors.counting())));
        result.put("planAdjusted", th2.stream().filter(r -> r.planAdjustment() != null).count());
        result.put("monthlyObjectCounts", IntStream.rangeClosed(1, 12).boxed()
                .collect(Collectors.toMap(m -> "T" + m, m -> thang.stream().filter(r -> month(r, m)).count(), (a, b) -> a, LinkedHashMap::new)));
        result.put("objectsWithoutMonth", thang.stream().filter(r -> IntStream.rangeClosed(1, 12).noneMatch(m -> month(r, m)))
                .map(AuditKhktThangRowResponse::auditObjectCode).limit(20).toList());
        return result;
    }

    /** suggest_plan_candidates - doi tuong diem rui ro cao o TH nhung CHUA nam trong TH2 da phe duyet. Chi
     * la goi y de nguoi lap ke hoach xem xet - quyet dinh van theo quy trinh KHKT hien co. */
    public Map<String, Object> suggestPlanCandidates(Integer year, Integer limit) {
        List<AuditKhktThRowResponse> th = thController.list(year).data();
        Set<String> approved = thConfirmedController.list(year).data().stream()
                .filter(r -> "APPROVED".equals(name(r.approvalStatus())))
                .map(r -> r.auditObjectCode().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        int max = limit == null || limit <= 0 ? 10 : Math.min(limit, 30);
        List<Map<String, Object>> candidates = th.stream()
                .filter(r -> r.auditObjectCode() != null && !approved.contains(r.auditObjectCode().toUpperCase(Locale.ROOT)))
                .sorted(Comparator.comparing(AuditKhktThRowResponse::riskScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(max)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("auditObjectCode", r.auditObjectCode());
                    m.put("auditObjectName", r.auditObjectName());
                    m.put("riskScore", r.riskScore());
                    m.put("rankLabel", r.rankLabel());
                    m.put("selection1", r.selection1());
                    m.put("selection2", r.selection2());
                    m.put("selection3", r.selection3());
                    m.put("bpReviewResult", excerpt(r.bpReviewResult()));
                    m.put("expertOpinion", excerpt(r.expertOpinion()));
                    m.put("proposingDepartments", r.proposingDepartmentCodes());
                    return m;
                }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("approvedInTh2", approved.size());
        result.put("highestRiskNotApproved", candidates);
        return result;
    }

    /** get_monthly_staffing - can doi KHKT thang voi phan bo can bo KHNS: thang/doi tuong chua co nguoi,
     * doi tuong chua co truong doan, so can bo chua phan bo moi thang. */
    public Map<String, Object> getMonthlyStaffing(Integer year) {
        List<AuditKhktThangRowResponse> thang = thangController.list(year).data();
        List<AuditKhnsPbRowResponse> allocation = khnsNamController.allocationRows(year).data();
        List<Map<String, Object>> employees = khnsNamController.list(year, false, false).data().stream()
                .map(r -> objectMapper.convertValue(r, new TypeReference<Map<String, Object>>() {
                })).toList();

        List<Map<String, Object>> months = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int month = m;
            List<String> planned = thang.stream().filter(r -> month(r, month)).map(AuditKhktThangRowResponse::auditObjectCode).toList();
            Map<String, Long> staffByObject = allocation.stream()
                    .filter(a -> a.months() != null && a.months().contains(month))
                    .collect(Collectors.groupingBy(AuditKhnsPbRowResponse::auditObjectCode, Collectors.counting()));
            List<String> unstaffed = planned.stream().filter(code -> !staffByObject.containsKey(code)).toList();
            unstaffed.forEach(code -> gaps.add("T" + month + ": " + code));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("plannedObjects", planned.size());
            row.put("staffedObjects", planned.size() - unstaffed.size());
            row.put("assignedStaff", staffByObject.values().stream().mapToLong(Long::longValue).sum());
            row.put("staffPerObject", staffByObject);
            row.put("unassignedEmployees", employees.stream().filter(e -> e.get("month" + month + "AuditObjectCode") == null).count());
            months.add(row);
        }
        Set<String> withLead = allocation.stream().filter(a -> a.roleInTeam() != null && "TEAM_LEAD".equals(a.roleInTeam().name()))
                .map(AuditKhnsPbRowResponse::auditObjectCode).collect(Collectors.toSet());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("totalEmployees", employees.size());
        result.put("months", months);
        result.put("plannedButUnstaffed", gaps.stream().limit(40).toList());
        result.put("objectsWithoutTeamLead", allocation.stream().map(AuditKhnsPbRowResponse::auditObjectCode).filter(Objects::nonNull)
                .distinct().filter(code -> !withLead.contains(code)).limit(30).toList());
        return result;
    }

    // ------------------------------------------------------------------ helpers

    private TdkpSource source(String key) {
        TdkpSource source = key == null ? null : tdkpSources.get(key.trim().toUpperCase(Locale.ROOT));
        if (source == null) {
            throw new BusinessException("AGENT_TDKP_SOURCE_INVALID", "source phai la 1 trong: " + String.join(", ", tdkpSources.keySet()),
                    HttpStatus.BAD_REQUEST);
        }
        return source;
    }

    private static boolean overdue(TdkpItem i, int graceDays) {
        return !i.done() && i.deadline() != null && i.deadline().plusDays(graceDays).isBefore(LocalDate.now());
    }

    private static boolean dueSoon(TdkpItem i, int days) {
        LocalDate today = LocalDate.now();
        return !i.done() && i.deadline() != null && !i.deadline().isBefore(today) && !i.deadline().isAfter(today.plusDays(days));
    }

    private static Map<String, Object> slim(TdkpItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.id());
        m.put("code", i.code());
        m.put("unit", i.unit());
        m.put("content", excerpt(i.content()));
        m.put("deadline", i.deadline());
        m.put("daysOverdue", i.daysOverdue());
        m.put("status", i.statusLabel());
        m.put("note", excerpt(i.note()));
        return m;
    }

    private static boolean month(AuditKhktThangRowResponse r, int m) {
        return switch (m) {
            case 1 -> r.month1();
            case 2 -> r.month2();
            case 3 -> r.month3();
            case 4 -> r.month4();
            case 5 -> r.month5();
            case 6 -> r.month6();
            case 7 -> r.month7();
            case 8 -> r.month8();
            case 9 -> r.month9();
            case 10 -> r.month10();
            case 11 -> r.month11();
            default -> r.month12();
        };
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > EXCERPT ? text.substring(0, EXCERPT) + "..." : text;
    }
}
