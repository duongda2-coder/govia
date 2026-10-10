package com.govia.audit.agent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.service.AgentText;
import com.govia.audit.controlpoint.controller.AuditControlPointController;
import com.govia.audit.controlpointqt.controller.AuditControlPointQtController;
import com.govia.audit.exceptionmapping.controller.AuditExceptionMappingController;
import com.govia.audit.exceptionmappingqt.controller.AuditExceptionMappingQtController;
import com.govia.audit.exceptiontype.controller.AuditExceptionTypeController;
import com.govia.audit.exceptiontypeqt.controller.AuditExceptionTypeQtController;
import com.govia.audit.planengagement.controller.AuditEngagementController;
import com.govia.audit.planengagement.controller.AuditWorkAssignmentController;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.ttss.controller.AuditTtssController;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.processstep.controller.AuditProcessStepDetailController;
import com.govia.audit.processstepqt.controller.AuditProcessStepDetailQtController;
import com.govia.audit.workitem.controller.AuditWorkItemController;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitemqt.controller.AuditWorkItemQtController;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Tool CHI DOC cua cac agent G2 (A3 Tac nghiep KT, A4 Phat hien & Kien nghi, A7 Danh muc). Moi method
 * chi goi lai method cua controller nghiep vu SAN CO (khong goi repository, khong viet lai logic) nen
 * van di qua dung @PreAuthorize va phan quyen theo dong cua man hinh tuong ung - vd TTSS van chi tra
 * dong ma nguoi dung duoc xem (AuditTtssService.scopeByVisibility). Ket qua duoc rut gon truong (bo id
 * noi bo, cat noi dung dai) de vua context cua model.
 */
@Component
public class AgentWorkToolsService {

    private static final int MAX_ENGAGEMENTS = 30;
    private static final int MAX_TTSS = 50;
    private static final int MAX_WORK_ITEMS = 60;
    private static final int MAX_CATALOG_HITS = 20;
    private static final int EXCERPT = 300;

    /** Danh muc tra cuu duoc qua search_catalog / find_catalog_duplicates. */
    public record CatalogSource(String key, String label, Supplier<List<?>> loader) {
    }

    private final AuditEngagementController engagementController;
    private final AuditWorkAssignmentController workAssignmentController;
    private final AuditTtssController ttssController;
    private final AuditRecommendationService recommendationService;
    private final ObjectMapper objectMapper;
    private final Map<String, CatalogSource> catalogs = new LinkedHashMap<>();

    public AgentWorkToolsService(AuditEngagementController engagementController,
                                 AuditWorkAssignmentController workAssignmentController,
                                 AuditTtssController ttssController,
                                 AuditRecommendationService recommendationService,
                                 AuditControlPointController controlPointController,
                                 AuditControlPointQtController controlPointQtController,
                                 AuditWorkItemController workItemController,
                                 AuditWorkItemQtController workItemQtController,
                                 AuditExceptionTypeController exceptionTypeController,
                                 AuditExceptionTypeQtController exceptionTypeQtController,
                                 AuditExceptionMappingController exceptionMappingController,
                                 AuditExceptionMappingQtController exceptionMappingQtController,
                                 AuditProcessStepDetailController processStepController,
                                 AuditProcessStepDetailQtController processStepQtController,
                                 ObjectMapper objectMapper) {
        this.engagementController = engagementController;
        this.workAssignmentController = workAssignmentController;
        this.ttssController = ttssController;
        this.recommendationService = recommendationService;
        this.objectMapper = objectMapper;
        register("control_point", "Danh mục Điểm kiểm soát (chi nhánh)", () -> controlPointController.list().data());
        register("control_point_qt", "Danh mục Điểm kiểm soát quy trình", () -> controlPointQtController.list().data());
        register("work_item", "Danh mục Công việc kiểm toán", () -> workItemController.list().data());
        register("work_item_qt", "Danh mục Bảng mã công việc quy trình", () -> workItemQtController.list().data());
        register("exception_type", "Danh mục Loại ngoại lệ", () -> exceptionTypeController.list().data());
        register("exception_type_qt", "Danh mục Loại ngoại lệ quy trình", () -> exceptionTypeQtController.list().data());
        register("exception_mapping", "Danh mục Mapping ngoại lệ", () -> exceptionMappingController.list().data());
        register("exception_mapping_qt", "Danh mục Mapping ngoại lệ quy trình", () -> exceptionMappingQtController.list().data());
        register("process_step", "Danh mục Bước quy trình chi tiết", () -> processStepController.list().data());
        register("process_step_qt", "Danh mục Bước quy trình chi tiết (quy trình)", () -> processStepQtController.list().data());
    }

    private void register(String key, String label, Supplier<List<?>> loader) {
        catalogs.put(key, new CatalogSource(key, label, loader));
    }

    /** Dong cua 1 danh muc (da doi sang Map) - dung cho kiem tra file truoc import (G4); quyen VIEW van do controller chan. */
    public List<Map<String, Object>> catalogRows(String catalogKey) {
        return rows(catalog(catalogKey));
    }

    public String catalogLabel(String catalogKey) {
        return catalog(catalogKey).label();
    }

    public List<String> catalogKeys() {
        return List.copyOf(catalogs.keySet());
    }

    // ------------------------------------------------------------------ dot kiem toan (A3/A4)

    /** list_my_engagements - cac CKT nguoi dung duoc phan cong (hoac tat ca neu co quyen VIEW_ALL). */
    public List<Map<String, Object>> listMyEngagements(CurrentUserPrincipal principal, String search, Integer limit) {
        List<String> words = words(search);
        int max = limit == null || limit <= 0 ? MAX_ENGAGEMENTS : Math.min(limit, MAX_ENGAGEMENTS);
        return assignedEngagements(principal).stream()
                .filter(e -> words.isEmpty() || matchesAll(e.code() + " " + e.name() + " " + e.auditObjectUnitCode() + " " + e.auditObjectUnitName(), words))
                .limit(max)
                .map(this::slimEngagement)
                .toList();
    }

    /** get_engagement_work_items - cong viec da phan cong cua 1 CKT (ca 3 giai doan neu khong chon). */
    public Map<String, Object> getEngagementWorkItems(CurrentUserPrincipal principal, String engagementRef, String phase, String status) {
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        List<AuditWorkPhase> phases = phase == null || phase.isBlank() ? List.of(AuditWorkPhase.values())
                : List.of(AuditWorkPhase.valueOf(phase.trim().toUpperCase(Locale.ROOT)));
        List<AuditWorkManagementItemResponse> items = new ArrayList<>();
        for (AuditWorkPhase p : phases) {
            items.addAll(workAssignmentController.list(engagement.id(), p, null, principal).data());
        }
        List<AuditWorkManagementItemResponse> filtered = items.stream()
                .filter(i -> status == null || status.isBlank() || (i.status() != null && i.status().name().equalsIgnoreCase(status.trim())))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", engagement.code());
        result.put("total", filtered.size());
        result.put("countByStatus", countBy(filtered, i -> i.status() == null ? "UNKNOWN" : i.status().name()));
        result.put("countByPhase", countBy(filtered, i -> i.phase() == null ? "UNKNOWN" : i.phase().name()));
        result.put("items", filtered.stream().limit(MAX_WORK_ITEMS).map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("phase", i.phase());
            m.put("workItemCode", i.workItemCode());
            m.put("workItemName", i.workItemName());
            m.put("businessSegment", i.businessSegmentCode());
            m.put("employee", i.employeeName());
            m.put("status", i.status());
            m.put("approvalStatus", i.approvalStatus());
            m.put("groupCode", i.groupCode());
            return m;
        }).toList());
        result.put("truncated", filtered.size() > MAX_WORK_ITEMS);
        return result;
    }

    // ------------------------------------------------------------------ TTSS & kien nghi (A4)

    /** get_ttss_summary - so lieu tong hop TTSS cua 1 CKT trong pham vi nguoi dung duoc xem. */
    public Map<String, Object> getTtssSummary(CurrentUserPrincipal principal, String engagementRef) {
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        List<AuditTtssRecordResponse> records = ttssController.list(engagement.id(), principal).data();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", engagement.code());
        result.put("engagementName", engagement.name());
        result.put("total", records.size());
        result.put("material", records.stream().filter(AuditTtssRecordResponse::material).count());
        result.put("withoutTeamRecommendation", records.stream().filter(r -> r.teamRecommendations() == null || r.teamRecommendations().isEmpty()).count());
        result.put("totalAmount", records.stream().map(AuditTtssRecordResponse::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("countByBusinessSegment", countBy(records, r -> r.businessSegmentCode() == null ? "-" : r.businessSegmentCode()));
        result.put("countByApprovalStatus", countBy(records, r -> r.recommendationApprovalStatus() == null ? "CHUA_GUI_DUYET" : r.recommendationApprovalStatus().name()));
        Map<String, List<AuditTtssRecordResponse>> byFinding = records.stream()
                .collect(Collectors.groupingBy(r -> r.findingCode() == null ? "-" : r.findingCode(), LinkedHashMap::new, Collectors.toList()));
        result.put("topFindings", byFinding.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<AuditTtssRecordResponse>> e) -> e.getValue().size()).reversed())
                .limit(15)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("findingCode", e.getKey());
                    m.put("findingName", e.getValue().get(0).findingName());
                    m.put("count", e.getValue().size());
                    m.put("material", e.getValue().stream().filter(AuditTtssRecordResponse::material).count());
                    m.put("totalAmount", e.getValue().stream().map(AuditTtssRecordResponse::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
                    return m;
                }).toList());
        return result;
    }

    /** get_ttss_records - dong TTSS (rut gon) cua 1 CKT, loc tuy chon. */
    public Map<String, Object> getTtssRecords(CurrentUserPrincipal principal, String engagementRef, String businessSegmentCode,
                                              String findingCode, Boolean materialOnly, Boolean withoutRecommendationOnly, Integer limit) {
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        List<AuditTtssRecordResponse> records = filterTtss(ttssController.list(engagement.id(), principal).data(),
                businessSegmentCode, findingCode, materialOnly, withoutRecommendationOnly);
        int max = limit == null || limit <= 0 ? 30 : Math.min(limit, MAX_TTSS);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engagementCode", engagement.code());
        result.put("total", records.size());
        result.put("records", records.stream().limit(max).map(AgentWorkToolsService::slimTtss).toList());
        result.put("truncated", records.size() > max);
        return result;
    }

    /** list_engagement_recommendations - danh muc "Luu ma kien nghi" cua 1 CKT. */
    public List<Map<String, Object>> listEngagementRecommendations(CurrentUserPrincipal principal, String engagementRef) {
        AuditEngagementResponse engagement = resolveEngagement(principal, engagementRef);
        return readRecommendations(engagement.id()).stream().map(AgentWorkToolsService::slimRecommendation).toList();
    }

    /** search_similar_findings - tim TTSS va kien nghi co noi dung giong nhau o cac CKT nguoi dung duoc
     * xem (phan quyen theo dong cua tung CKT van ap dung) - dung de tham khao cach viet kien nghi cu. */
    public Map<String, Object> searchSimilarFindings(CurrentUserPrincipal principal, String query, String excludeEngagementRef, Integer limit) {
        List<String> words = words(query);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("query qua ngan de tim kiem");
        }
        int max = limit == null || limit <= 0 ? 8 : Math.min(limit, 15);
        String exclude = excludeEngagementRef == null ? null : excludeEngagementRef.trim();
        List<Map<String, Object>> ttssHits = new ArrayList<>();
        List<Map<String, Object>> recommendationHits = new ArrayList<>();
        for (AuditEngagementResponse e : assignedEngagements(principal).stream().limit(MAX_ENGAGEMENTS).toList()) {
            if (exclude != null && (exclude.equalsIgnoreCase(e.code()) || exclude.equals(String.valueOf(e.id())))) {
                continue;
            }
            for (AuditTtssRecordResponse r : safeTtss(e.id(), principal)) {
                double score = score(r.findingName() + " " + r.ttssContent(), words);
                if (score > 0) {
                    Map<String, Object> m = slimTtss(r);
                    m.put("engagementCode", e.code());
                    m.put("score", score);
                    ttssHits.add(m);
                }
            }
            for (AuditRecommendationResponse rec : safeRecommendations(e.id())) {
                double score = score(rec.content(), words);
                if (score > 0) {
                    Map<String, Object> m = slimRecommendation(rec);
                    m.put("engagementCode", e.code());
                    m.put("score", score);
                    recommendationHits.add(m);
                }
            }
        }
        Comparator<Map<String, Object>> byScore = Comparator.comparingDouble(m -> -((Double) m.get("score")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ttss", ttssHits.stream().sorted(byScore).limit(max).toList());
        result.put("recommendations", recommendationHits.stream().sorted(byScore).limit(max).toList());
        return result;
    }

    // ------------------------------------------------------------------ danh muc (A3/A7)

    /** search_catalog - tim dong trong 1 danh muc theo tu khoa (+ nghiep vu tuy chon). */
    public Map<String, Object> searchCatalog(String catalogKey, String query, String businessSegmentCode, Integer limit) {
        CatalogSource source = catalog(catalogKey);
        List<String> words = words(query);
        int max = limit == null || limit <= 0 ? 10 : Math.min(limit, MAX_CATALOG_HITS);
        List<Map<String, Object>> rows = rows(source).stream()
                .filter(r -> businessSegmentCode == null || businessSegmentCode.isBlank()
                        || businessSegmentCode.equalsIgnoreCase(String.valueOf(r.get("businessSegmentCode"))))
                .toList();
        List<Map<String, Object>> hits = rows.stream()
                .map(r -> Map.entry(r, words.isEmpty() ? 1.0 : score(textOf(r), words)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Map<String, Object>, Double>comparingByValue().reversed())
                .limit(max)
                .map(Map.Entry::getKey)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("catalog", source.label());
        result.put("totalRows", rows.size());
        result.put("hits", hits);
        return result;
    }

    /** find_catalog_duplicates - nhom cac dong co TEN gan nhu trung nhau (khong dau, khong phan biet
     * hoa thuong) trong cung nghiep vu/nam/bo cong viec - goi y ra soat, khong tu xoa/sua gi. */
    public Map<String, Object> findCatalogDuplicates(String catalogKey, Integer limit) {
        CatalogSource source = catalog(catalogKey);
        int max = limit == null || limit <= 0 ? 20 : Math.min(limit, 50);
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> r : rows(source)) {
            String name = firstText(r, "name", "exceptionTypeName", "processStepSummaryName");
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = String.join("|", String.valueOf(r.get("businessSegmentCode")), String.valueOf(r.get("applicableYear")),
                    String.valueOf(r.get("workSetCode")), String.valueOf(r.get("phase")), AgentText.normalize(name));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> duplicates = groups.values().stream()
                .filter(g -> g.size() > 1)
                .limit(max)
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("businessSegmentCode", g.get(0).get("businessSegmentCode"));
                    m.put("name", firstText(g.get(0), "name", "exceptionTypeName", "processStepSummaryName"));
                    m.put("codes", g.stream().map(r -> r.get("code")).toList());
                    m.put("count", g.size());
                    return m;
                }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("catalog", source.label());
        result.put("totalRows", groups.values().stream().mapToInt(List::size).sum());
        result.put("duplicateGroups", duplicates);
        return result;
    }

    // ------------------------------------------------------------------ dung chung (ca AgentDraftService)

    /** Tim CKT theo id hoac ma (khong phan biet hoa thuong) TRONG danh sach nguoi dung duoc phan cong. */
    public AuditEngagementResponse resolveEngagement(CurrentUserPrincipal principal, String engagementRef) {
        if (engagementRef == null || engagementRef.isBlank()) {
            throw new IllegalArgumentException("Thieu ma cuoc kiem toan (engagement)");
        }
        String ref = engagementRef.trim();
        return assignedEngagements(principal).stream()
                .filter(e -> ref.equalsIgnoreCase(e.code()) || ref.equals(String.valueOf(e.id())))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AGENT_ENGAGEMENT_NOT_FOUND",
                        "Khong tim thay cuoc kiem toan '" + ref + "' trong danh sach ban duoc phan cong", HttpStatus.NOT_FOUND));
    }

    public List<AuditTtssRecordResponse> filterTtss(List<AuditTtssRecordResponse> records, String businessSegmentCode, String findingCode,
                                                    Boolean materialOnly, Boolean withoutRecommendationOnly) {
        return records.stream()
                .filter(r -> businessSegmentCode == null || businessSegmentCode.isBlank() || businessSegmentCode.equalsIgnoreCase(r.businessSegmentCode()))
                .filter(r -> findingCode == null || findingCode.isBlank() || findingCode.equalsIgnoreCase(r.findingCode()))
                .filter(r -> !Boolean.TRUE.equals(materialOnly) || r.material())
                .filter(r -> !Boolean.TRUE.equals(withoutRecommendationOnly) || r.teamRecommendations() == null || r.teamRecommendations().isEmpty())
                .toList();
    }

    public static Map<String, Object> slimTtss(AuditTtssRecordResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("findingCode", r.findingCode());
        m.put("findingName", r.findingName());
        m.put("ttssContent", excerpt(r.ttssContent()));
        m.put("businessSegment", r.businessSegmentCode());
        m.put("workItemCode", r.workItemCode());
        m.put("processStep", r.processStepSummaryName());
        m.put("material", r.material());
        m.put("amount", r.amount());
        m.put("customerName", r.customerName());
        m.put("exceptionDate", r.exceptionDate());
        m.put("uploaderRecommendation", r.uploaderRecommendationName());
        m.put("teamRecommendationCodes", r.teamRecommendations() == null ? List.of()
                : r.teamRecommendations().stream().map(AuditTtssRecordResponse.TeamRecommendation::code).toList());
        return m;
    }

    public static Map<String, Object> slimRecommendation(AuditRecommendationResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", r.code());
        m.put("businessSegment", r.businessSegmentCode());
        m.put("content", r.content());
        return m;
    }

    public List<AuditEngagementResponse> assignedEngagements(CurrentUserPrincipal principal) {
        return engagementController.listAssigned(principal).data();
    }

    public List<AuditRecommendationResponse> safeRecommendations(UUID engagementId) {
        try {
            return readRecommendations(engagementId);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * Doc "Lưu mã kiến nghị" cua 1 CKT KHONG GHI GI. Khong goi AuditRecommendationController.list vi ham do (cua man
     * hinh) tu tao dong kien nghi mac dinh khi CKT chua co dong nao - AI doc thi khong duoc lam phat sinh du lieu. Dung
     * listByEngagementIds (transaction chi doc, khong seed) va kiem tra dung quyen cua endpoint man hinh (AUDIT.TTSS.VIEW).
     */
    public List<AuditRecommendationResponse> readRecommendations(UUID engagementId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = auth != null && auth.getAuthorities().stream().anyMatch(a -> "PERM_AUDIT.TTSS.VIEW".equals(a.getAuthority()));
        if (!allowed) {
            throw new AccessDeniedException("Thieu quyen AUDIT.TTSS.VIEW");
        }
        return recommendationService.listByEngagementIds(List.of(engagementId));
    }

    private List<AuditTtssRecordResponse> safeTtss(UUID engagementId, CurrentUserPrincipal principal) {
        try {
            return ttssController.list(engagementId, principal).data();
        } catch (BusinessException e) {
            return List.of();
        }
    }

    private Map<String, Object> slimEngagement(AuditEngagementResponse e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", e.code());
        m.put("name", e.name());
        m.put("auditObjectUnit", e.auditObjectUnitCode() + " - " + e.auditObjectUnitName());
        m.put("year", e.year());
        m.put("expectedMonth", e.expectedMonth());
        m.put("status", e.status());
        m.put("teamLead", e.teamLeadEmployeeName());
        m.put("fieldworkStart", e.fieldworkStartDate());
        m.put("fieldworkEnd", e.fieldworkEndDate());
        m.put("processAudit", e.processEngagementId() != null);
        return m;
    }

    private CatalogSource catalog(String key) {
        CatalogSource source = key == null ? null : catalogs.get(key.trim().toLowerCase(Locale.ROOT));
        if (source == null) {
            throw new IllegalArgumentException("catalog phai la 1 trong: " + String.join(", ", catalogs.keySet()));
        }
        return source;
    }

    /** Doi dong danh muc thanh Map, bo cac cot id noi bo (UUID) cho gon. */
    private List<Map<String, Object>> rows(CatalogSource source) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : source.loader().get()) {
            Map<String, Object> m = objectMapper.convertValue(item, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            m.keySet().removeIf(k -> k.equals("id") || k.endsWith("Id"));
            rows.add(m);
        }
        return rows;
    }

    private static String textOf(Map<String, Object> row) {
        return row.values().stream().filter(v -> v instanceof String).map(String::valueOf).collect(Collectors.joining(" "));
    }

    private static String firstText(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            Object v = row.get(k);
            if (v instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static List<String> words(String text) {
        return AgentText.tokens(text).stream().filter(w -> w.length() >= 2).distinct().toList();
    }

    private static boolean matchesAll(String text, List<String> words) {
        String normalized = AgentText.normalize(text);
        return words.stream().allMatch(w -> AgentText.containsPhrase(normalized, w));
    }

    /** Ti le tu khoa xuat hien trong text (0..1) - du dung cho tra cuu noi bo, khong can embedding. */
    private static double score(String text, List<String> words) {
        String normalized = AgentText.normalize(text);
        long hit = words.stream().filter(w -> AgentText.containsPhrase(normalized, w)).count();
        double ratio = (double) hit / words.size();
        return ratio >= 0.5 ? Math.round(ratio * 100) / 100.0 : 0;
    }

    private static <T> Map<String, Long> countBy(List<T> items, java.util.function.Function<T, String> key) {
        return items.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.counting()));
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > EXCERPT ? text.substring(0, EXCERPT) + "..." : text;
    }
}
