package com.govia.audit.agent.service;

import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.dto.AgentSuggestionResponse;
import com.govia.audit.agent.entity.AgentSuggestion;
import com.govia.audit.agent.entity.AgentSuggestion.Severity;
import com.govia.audit.agent.repository.AgentSuggestionRepository;
import com.govia.audit.agent.tools.AgentPlanTdkpToolsService;
import com.govia.audit.agent.tools.AgentPlanTdkpToolsService.TdkpItem;
import com.govia.audit.agent.tools.AgentQualityToolsService;
import com.govia.audit.agent.tools.AgentWorkToolsService;
import com.govia.audit.dgcl.AuditDgclController;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.entity.AuditEngagementStatus;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.workflow.controller.WorkflowTaskController;
import com.govia.identity.workflow.dto.TaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * "Gợi ý AI" (G4, muc M3 cua phuong an): moi sang ngay lam viec (AgentSuggestionScheduler) hoac khi nguoi dung bam
 * "Làm mới", he thong ra soat du lieu NGUOI DUNG DUOC XEM - goi lai dung controller/tool chi-doc cua A0/A2/A4/A5/A6
 * bang quyen cua chinh nguoi do - va ghi cac viec can chu y vao bang agent_suggestion.
 *
 * <p>Nguyen tac: KHONG goi model (ket qua xac dinh, chay duoc ca khi may GPU tat, khong "ao giac" so lieu), KHONG tao
 * task Flowable, KHONG gui thu/thong bao, KHONG doi du lieu nghiep vu. Nguoi dung tu mo man hinh lien quan va xu ly
 * nhu hien nay. Moi kiem tra doc lap: thieu quyen hay loi o 1 kiem tra thi bo qua kiem tra do.
 */
@Service
public class AgentSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AgentSuggestionService.class);
    private static final int MAX_ENGAGEMENTS = 20;
    private static final int MAX_LIST = 8;

    /** Ma kiem tra -> man hinh lien quan. */
    static final String MY_TASKS = "MY_TASKS";
    static final String TDKP_OVERDUE = "TDKP_OVERDUE";
    static final String TDKP_DUE_SOON = "TDKP_DUE_SOON";
    static final String TTSS_MATERIAL_NO_RECOMMENDATION = "TTSS_MATERIAL_NO_RECOMMENDATION";
    static final String PLAN_HIGH_RISK_NOT_APPROVED = "PLAN_HIGH_RISK_NOT_APPROVED";
    static final String PLAN_UNSTAFFED = "PLAN_UNSTAFFED";
    static final String DGCL_PENDING = "DGCL_PENDING";

    private static final Map<String, String> TDKP_PATHS = Map.of(
            "CEO_ALL", "/audit/tdkp/ceo-all", "CEO_KH", "/audit/tdkp/ceo-kh", "BRANCH", "/audit/tdkp/branch",
            "RESOLUTION", "/audit/tdkp/resolution", "UNIT", "/audit/tdkp/unit-recommendation");

    private final AgentSuggestionRepository repository;
    private final AgentProfileRegistry registry;
    private final AgentProperties agentProperties;
    private final WorkflowTaskController workflowTaskController;
    private final AgentPlanTdkpToolsService planTdkpTools;
    private final AgentWorkToolsService workTools;
    private final AgentQualityToolsService qualityTools;
    private final AuditDgclController dgclController;
    private final TransactionTemplate transactionTemplate;

    public AgentSuggestionService(AgentSuggestionRepository repository, AgentProfileRegistry registry, AgentProperties agentProperties,
                                  WorkflowTaskController workflowTaskController, AgentPlanTdkpToolsService planTdkpTools,
                                  AgentWorkToolsService workTools, AgentQualityToolsService qualityTools,
                                  AuditDgclController dgclController, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.registry = registry;
        this.agentProperties = agentProperties;
        this.workflowTaskController = workflowTaskController;
        this.planTdkpTools = planTdkpTools;
        this.workTools = workTools;
        this.qualityTools = qualityTools;
        this.dgclController = dgclController;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 1 goi y chua luu. */
    record Draft(String agentCode, String category, Severity severity, String title, String detail, String linkPath, int count) {
    }

    // ------------------------------------------------------------------ API cho nguoi dung

    public List<AgentSuggestionResponse> list(UUID userId) {
        requireEnabled();
        return repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(TenantContext.getTenantId(), userId).stream()
                .filter(s -> !s.isDismissed())
                .sorted(Comparator.comparing((AgentSuggestion s) -> s.getRunDate()).reversed()
                        .thenComparing(s -> s.getSeverity().ordinal(), Comparator.reverseOrder()))
                .map(AgentSuggestionService::toResponse)
                .toList();
    }

    /** "Làm mới" - chay ngay cac kiem tra cho chinh nguoi dung dang dang nhap (SecurityContext cua request). */
    public List<AgentSuggestionResponse> refresh(CurrentUserPrincipal principal) {
        requireEnabled();
        generate(principal);
        return list(principal.userId());
    }

    public void dismiss(UUID id, UUID userId) {
        transactionTemplate.executeWithoutResult(status -> {
            AgentSuggestion s = repository.findByIdAndTenantIdAndUserId(id, TenantContext.getTenantId(), userId)
                    .orElseThrow(() -> new BusinessException("AGENT_SUGGESTION_NOT_FOUND", "Khong tim thay goi y", HttpStatus.NOT_FOUND));
            s.setDismissed(true);
            repository.save(s);
        });
    }

    public void markAllRead(UUID userId) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now();
            List<AgentSuggestion> unread = repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(TenantContext.getTenantId(), userId).stream()
                    .filter(s -> s.getReadAt() == null).toList();
            unread.forEach(s -> s.setReadAt(now));
            repository.saveAll(unread);
        });
    }

    // ------------------------------------------------------------------ sinh goi y

    /**
     * Chay moi kiem tra bang quyen cua principal (SecurityContext + TenantContext PHAI da duoc dat dung nguoi nay) roi
     * thay the goi y cu: xoa goi y cua cac ngay truoc + goi y hom nay chua bi an, giu loai nguoi dung da "Ẩn" hom nay.
     * Phan doc du lieu chay NGOAI transaction (loi/thieu quyen o 1 kiem tra khong lam hong lan ghi).
     */
    public int generate(CurrentUserPrincipal principal) {
        if (!agentProperties.isEnabled()) {
            return 0;
        }
        List<Draft> drafts = new ArrayList<>();
        AgentProperties.Schedule cfg = agentProperties.getSchedule();
        run("A0", () -> myTasks(principal, cfg.getStaleTaskDays()), drafts);
        run("A5", () -> tdkp(cfg.getDueSoonDays()), drafts);
        run("A4", () -> materialWithoutRecommendation(principal), drafts);
        run("A2", this::plan, drafts);
        run("A6", () -> dgcl(principal), drafts);
        persist(principal.userId(), drafts);
        return drafts.size();
    }

    private void run(String agentCode, Supplier<List<Draft>> check, List<Draft> target) {
        if (!registry.isEnabled(agentCode)) {
            return;
        }
        try {
            target.addAll(check.get());
        } catch (AccessDeniedException e) {
            // nguoi dung khong co quyen man hinh nguon - khong co goi y loai nay, dung thiet ke
        } catch (RuntimeException e) {
            log.debug("Kiem tra goi y AI {} bi bo qua: {}", agentCode, e.getMessage());
        }
    }

    private void persist(UUID userId, List<Draft> drafts) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();
        transactionTemplate.executeWithoutResult(status -> {
            List<AgentSuggestion> existing = repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
            Set<String> dismissedToday = existing.stream().filter(s -> s.isDismissed() && today.equals(s.getRunDate()))
                    .map(AgentSuggestion::getCategory).collect(Collectors.toSet());
            Map<String, Instant> readToday = existing.stream().filter(s -> today.equals(s.getRunDate()) && s.getReadAt() != null)
                    .collect(Collectors.toMap(s -> s.getCategory() + "|" + s.getTitle(), AgentSuggestion::getReadAt, (a, b) -> a));
            repository.deleteAll(existing.stream().filter(s -> !today.equals(s.getRunDate()) || !s.isDismissed()).toList());
            repository.flush();
            for (Draft d : drafts) {
                if (dismissedToday.contains(d.category())) {
                    continue;
                }
                AgentSuggestion s = new AgentSuggestion();
                s.setTenantId(tenantId);
                s.setUserId(userId);
                s.setAgentCode(d.agentCode());
                s.setCategory(d.category());
                s.setSeverity(d.severity());
                s.setTitle(cut(d.title(), 300));
                s.setDetail(cut(d.detail(), 4000));
                s.setLinkPath(d.linkPath());
                s.setItemCount(d.count());
                s.setRunDate(today);
                s.setReadAt(readToday.get(d.category() + "|" + s.getTitle()));
                repository.save(s);
            }
        });
    }

    // ------------------------------------------------------------------ cac kiem tra

    /** A0 - viec dang cho trong quy trinh phe duyet, uu tien viec da cho lau. */
    private List<Draft> myTasks(CurrentUserPrincipal principal, int staleDays) {
        List<TaskSummary> tasks = workflowTaskController.myTasks(principal).data();
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Instant staleBefore = Instant.now().minus(staleDays, ChronoUnit.DAYS);
        List<TaskSummary> stale = tasks.stream().filter(t -> t.createTime() != null && t.createTime().isBefore(staleBefore)).toList();
        String detail = tasks.stream().sorted(Comparator.comparing(TaskSummary::createTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_LIST)
                .map(t -> "• " + t.name() + (t.businessKey() == null ? "" : " [" + t.businessKey() + "]")
                        + (t.createTime() == null ? "" : " - chờ " + ChronoUnit.DAYS.between(t.createTime(), Instant.now()) + " ngày"))
                .collect(Collectors.joining("\n"));
        String title = stale.isEmpty() ? "Có " + tasks.size() + " việc đang chờ anh/chị xử lý"
                : "Có " + tasks.size() + " việc đang chờ xử lý, " + stale.size() + " việc đã chờ trên " + staleDays + " ngày";
        return List.of(new Draft("A0", MY_TASKS, stale.isEmpty() ? Severity.INFO : Severity.WARN, title, detail, "/workflow/tasks", tasks.size()));
    }

    /** A5 - kien nghi/nghi quyet qua han va sap den han tren tung danh sach TDKP nguoi dung duoc xem. */
    private List<Draft> tdkp(int dueSoonDays) {
        List<Draft> result = new ArrayList<>();
        for (String source : planTdkpTools.tdkpSourceKeys()) {
            List<TdkpItem> items;
            try {
                items = planTdkpTools.itemsForReminder(source, null, dueSoonDays).stream().filter(i -> !i.done()).toList();
            } catch (AccessDeniedException e) {
                continue;
            }
            String label = planTdkpTools.sourceLabel(source);
            List<TdkpItem> overdue = items.stream().filter(i -> i.daysOverdue() != null && i.daysOverdue() > 0)
                    .sorted(Comparator.comparing(TdkpItem::daysOverdue).reversed()).toList();
            List<TdkpItem> soon = items.stream().filter(i -> i.daysOverdue() != null && i.daysOverdue() <= 0)
                    .sorted(Comparator.comparing(TdkpItem::deadline)).toList();
            if (!overdue.isEmpty()) {
                result.add(new Draft("A5", TDKP_OVERDUE + ":" + source, Severity.HIGH,
                        label + ": " + overdue.size() + " kiến nghị đã quá hạn chưa hoàn thành",
                        overdue.stream().limit(MAX_LIST).map(i -> "• " + code(i.code()) + unit(i.unit()) + " - quá hạn " + i.daysOverdue() + " ngày")
                                .collect(Collectors.joining("\n")) + more(overdue.size())
                                + "\nCó thể soạn thư đôn đốc ở tab Soạn nháp → Thư đôn đốc.",
                        TDKP_PATHS.get(source), overdue.size()));
            }
            if (!soon.isEmpty()) {
                result.add(new Draft("A5", TDKP_DUE_SOON + ":" + source, Severity.WARN,
                        label + ": " + soon.size() + " kiến nghị đến hạn trong " + dueSoonDays + " ngày tới",
                        soon.stream().limit(MAX_LIST).map(i -> "• " + code(i.code()) + unit(i.unit()) + " - hạn " + i.deadline())
                                .collect(Collectors.joining("\n")) + more(soon.size()),
                        TDKP_PATHS.get(source), soon.size()));
            }
        }
        return result;
    }

    /** A4 - TTSS trong yeu chua gan kien nghi truong doan o cac CKT dang thuc hien ma nguoi dung tham gia. */
    private List<Draft> materialWithoutRecommendation(CurrentUserPrincipal principal) {
        int year = LocalDate.now().getYear();
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (AuditEngagementResponse e : activeEngagements(workTools.assignedEngagements(principal), year)) {
            Map<String, Object> r;
            try {
                r = workTools.getTtssRecords(principal, e.code(), null, null, true, true, 1);
            } catch (AccessDeniedException | BusinessException ex) {
                continue;
            }
            int n = ((Number) r.getOrDefault("total", 0)).intValue();
            if (n > 0) {
                total += n;
                lines.add("• " + e.code() + (e.auditObjectUnitName() == null ? "" : " - " + e.auditObjectUnitName()) + ": " + n + " TTSS");
            }
        }
        if (total == 0) {
            return List.of();
        }
        return List.of(new Draft("A4", TTSS_MATERIAL_NO_RECOMMENDATION, Severity.WARN,
                total + " TTSS trọng yếu chưa được gắn kiến nghị trưởng đoàn",
                String.join("\n", lines.stream().limit(MAX_LIST).toList()) + more(lines.size())
                        + "\nCó thể nhờ AI soạn nháp ở tab Soạn nháp → Kiến nghị từ TTSS.",
                "/audit/plan/execution/work-management/ttss", total));
    }

    /** A2 - doi tuong rui ro cao chua vao TH2 da duyet; doi tuong ke hoach thang nay/thang sau chua co nguoi. */
    @SuppressWarnings("unchecked")
    private List<Draft> plan() {
        int year = LocalDate.now().getYear();
        List<Draft> result = new ArrayList<>();
        try {
            Map<String, Object> c = planTdkpTools.suggestPlanCandidates(year, 5);
            List<Map<String, Object>> top = (List<Map<String, Object>>) c.getOrDefault("highestRiskNotApproved", List.of());
            List<Map<String, Object>> withScore = top.stream().filter(m -> m.get("riskScore") != null).toList();
            if (!withScore.isEmpty()) {
                result.add(new Draft("A2", PLAN_HIGH_RISK_NOT_APPROVED, Severity.INFO,
                        withScore.size() + " đối tượng điểm rủi ro cao nhất năm " + year + " chưa có trong TH2 đã phê duyệt",
                        withScore.stream().map(m -> "• " + m.get("auditObjectCode") + " - " + Objects.toString(m.get("auditObjectName"), "")
                                + " (điểm " + m.get("riskScore") + (m.get("rankLabel") == null ? "" : ", " + m.get("rankLabel")) + ")")
                                .collect(Collectors.joining("\n")) + "\nChỉ là gợi ý xem xét - chọn/duyệt vẫn theo màn KHKT.",
                        "/audit/plan/khkt-th", withScore.size()));
            }
        } catch (AccessDeniedException e) {
            // khong co quyen KHKT TH
        }
        try {
            Map<String, Object> s = planTdkpTools.getMonthlyStaffing(year);
            int month = LocalDate.now().getMonthValue();
            Set<String> watch = Set.of("T" + month + ":", "T" + (month % 12 + 1) + ":");
            List<String> gaps = ((List<String>) s.getOrDefault("plannedButUnstaffed", List.of())).stream()
                    .filter(g -> watch.stream().anyMatch(g::startsWith)).toList();
            if (!gaps.isEmpty()) {
                result.add(new Draft("A2", PLAN_UNSTAFFED, Severity.WARN,
                        gaps.size() + " đối tượng trong KHKT tháng này/tháng sau chưa được phân bổ cán bộ",
                        gaps.stream().limit(MAX_LIST).map(g -> "• " + g).collect(Collectors.joining("\n")) + more(gaps.size()),
                        "/audit/plan/khns-pb", gaps.size()));
            }
        } catch (AccessDeniedException e) {
            // khong co quyen KHKT thang/KHNS
        }
        return result;
    }

    /** A6 - phieu DGCL da luu chua xac nhan / thanh vien chua cham PL01F, chi voi nguoi duoc "Thực hiện ĐGCL". */
    private List<Draft> dgcl(CurrentUserPrincipal principal) {
        if (!dgclController.capability(principal).data().canEvaluate()) {
            return List.of();
        }
        int year = LocalDate.now().getYear();
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (AuditEngagementResponse e : activeEngagements(qualityTools.engagements(principal), year)) {
            List<SubjectRow> subjects;
            try {
                subjects = qualityTools.subjects(principal, e);
            } catch (AccessDeniedException | BusinessException ex) {
                continue;
            }
            long notScored = subjects.stream().filter(s -> s.pl01fScore() == null).count();
            long unconfirmed = subjects.stream().filter(s -> (s.pl01aScore() != null && !s.pl01aConfirmed())
                    || (s.pl01bScore() != null && !s.pl01bConfirmed()) || (s.pl01fScore() != null && !s.pl01fConfirmed())).count();
            // Chi nhac khi doan da bat dau cham (co it nhat 1 phieu) - tranh nhac moi CKT moi mo
            boolean started = subjects.stream().anyMatch(s -> s.pl01aScore() != null || s.pl01bScore() != null || s.pl01fScore() != null);
            if (started && (notScored > 0 || unconfirmed > 0)) {
                total += (int) (notScored + unconfirmed);
                lines.add("• " + e.code() + ": " + notScored + " chưa chấm PL01F, " + unconfirmed + " có phiếu đã lưu chưa xác nhận");
            }
        }
        if (lines.isEmpty()) {
            return List.of();
        }
        return List.of(new Draft("A6", DGCL_PENDING, Severity.INFO, lines.size() + " cuộc kiểm toán còn phiếu ĐGCL chưa chấm/chưa xác nhận",
                String.join("\n", lines.stream().limit(MAX_LIST).toList()) + more(lines.size()), "/audit/dgcl", total));
    }

    // ------------------------------------------------------------------ helpers

    /** CKT nam nay/nam truoc, chua huy, toi da 20 - giu job nhe khi nguoi dung thay rat nhieu CKT. */
    private static List<AuditEngagementResponse> activeEngagements(List<AuditEngagementResponse> list, int year) {
        return list.stream()
                .filter(e -> e.year() == null || e.year() >= year - 1)
                .filter(e -> e.status() != AuditEngagementStatus.CANCELLED)
                .sorted(Comparator.comparing(AuditEngagementResponse::year, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_ENGAGEMENTS).toList();
    }

    private void requireEnabled() {
        if (!agentProperties.isEnabled()) {
            throw new BusinessException("AGENT_DISABLED", "Tro ly AI dang duoc tat theo cau hinh he thong", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static AgentSuggestionResponse toResponse(AgentSuggestion s) {
        return new AgentSuggestionResponse(s.getId(), s.getAgentCode(), s.getCategory(), s.getSeverity().name(), s.getTitle(), s.getDetail(),
                s.getLinkPath(), s.getItemCount(), s.getRunDate(), s.getCreatedAt(), s.getReadAt() != null);
    }

    private static String code(String code) {
        return code == null ? "(không mã)" : code;
    }

    private static String unit(String unit) {
        return unit == null || unit.isBlank() ? "" : " - " + unit;
    }

    private static String more(int size) {
        return size > MAX_LIST ? "\n… và " + (size - MAX_LIST) + " mục khác" : "";
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
