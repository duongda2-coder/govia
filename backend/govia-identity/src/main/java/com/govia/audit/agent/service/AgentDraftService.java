package com.govia.audit.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.dto.RecommendationDraftRequest;
import com.govia.audit.agent.dto.RecommendationDraftResponse;
import com.govia.audit.agent.dto.RewriteRequest;
import com.govia.audit.agent.dto.RewriteResponse;
import com.govia.audit.agent.llm.ChatMessage;
import com.govia.audit.agent.llm.ChatResult;
import com.govia.audit.agent.llm.LlmProvider;
import com.govia.audit.agent.llm.ToolCallRequest;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.dto.ReminderDraftRequest;
import com.govia.audit.agent.dto.ReminderDraftResponse;
import com.govia.audit.agent.tools.AgentPlanTdkpToolsService;
import com.govia.audit.agent.tools.AgentWorkToolsService;
import com.govia.audit.agent.tools.ToolExecutionResult;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.ttss.controller.AuditTtssController;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Soan nhap (muc tu dong M2) cua A4 Phat hien & Kien nghi. KHONG ghi gi vao du lieu nghiep vu: chi tra
 * ban nhap ve giao dien, nguoi dung sua roi tu bam Them/Luu qua API san co (validate san co).
 *
 * <p>Khac luong chat: day la 1 lan goi model (khong vong lap tool) - he thong tu lay du lieu TTSS qua
 * controller san co (dung phan quyen theo dong) roi dua thang vao prompt, model chi viet. Nhanh hon va
 * on dinh hon voi model nho. Moi ban nhap duoc kiem tra lai: ma phat hien phai nam trong du lieu dua
 * vao, so lieu (>= 3 chu so) phai xuat hien trong du lieu goc - khong thi danh dau grounded=false.
 */
@Service
public class AgentDraftService {

    private static final Logger log = LoggerFactory.getLogger(AgentDraftService.class);
    private static final int MAX_FINDING_GROUPS = 12;
    private static final int MAX_CONTENT = 2000;
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,]{2,}");
    private static final String DRAFT_TOOL = "submit_recommendation_drafts";
    private static final String REWRITE_TOOL = "submit_rewrite";
    private static final String REMINDER_TOOL = "submit_reminder_template";
    private static final java.time.format.DateTimeFormatter VN_DATE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LlmProvider llmProvider;
    private final AgentWorkToolsService workTools;
    private final AgentPlanTdkpToolsService planTdkpTools;
    private final AuditTtssController ttssController;
    private final AgentAuditLogService auditLogService;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;

    public AgentDraftService(LlmProvider llmProvider, AgentWorkToolsService workTools, AgentPlanTdkpToolsService planTdkpTools,
                             AuditTtssController ttssController,
                             AgentAuditLogService auditLogService, AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.workTools = workTools;
        this.planTdkpTools = planTdkpTools;
        this.ttssController = ttssController;
        this.auditLogService = auditLogService;
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
    }

    public RecommendationDraftResponse draftRecommendations(RecommendationDraftRequest request, CurrentUserPrincipal principal) {
        requireEnabled();
        long start = System.currentTimeMillis();
        AuditEngagementResponse engagement = workTools.resolveEngagement(principal, request.engagementId().toString());
        List<AuditTtssRecordResponse> all = ttssController.list(engagement.id(), principal).data().stream()
                .filter(r -> request.businessSegmentId() == null || request.businessSegmentId().equals(r.businessSegmentId()))
                .toList();
        if (all.isEmpty()) {
            throw new BusinessException("AGENT_DRAFT_NO_TTSS",
                    "Chua co TTSS nao (trong pham vi ban duoc xem) de AI soan kien nghi cho nghiep vu nay", HttpStatus.BAD_REQUEST);
        }
        // Uu tien TTSS chua gan kien nghi truong doan - do la phan con thieu kien nghi
        List<AuditTtssRecordResponse> pending = all.stream().filter(r -> r.teamRecommendations() == null || r.teamRecommendations().isEmpty()).toList();
        List<AuditTtssRecordResponse> source = pending.isEmpty() ? all : pending;
        String segmentCode = request.businessSegmentId() == null ? null : all.get(0).businessSegmentCode();

        Map<String, List<AuditTtssRecordResponse>> byFinding = source.stream()
                .collect(Collectors.groupingBy(r -> r.findingCode() == null ? "-" : r.findingCode(), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> findings = byFinding.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, List<AuditTtssRecordResponse>> e) -> e.getValue().stream().anyMatch(AuditTtssRecordResponse::material)).reversed()
                        .thenComparing(e -> -e.getValue().size()))
                .limit(MAX_FINDING_GROUPS)
                .map(e -> findingGroup(e.getKey(), e.getValue()))
                .toList();
        Set<String> knownCodes = findings.stream().map(f -> String.valueOf(f.get("findingCode"))).collect(Collectors.toSet());

        List<AuditRecommendationResponse> existing = workTools.safeRecommendations(engagement.id());
        List<RecommendationDraftResponse.SimilarRecommendation> similar = similarRecommendations(principal, engagement, findings);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("engagement", engagement.code() + " - " + engagement.name());
        input.put("auditObjectUnit", engagement.auditObjectUnitName());
        input.put("businessSegment", segmentCode);
        input.put("findings", findings);
        input.put("existingRecommendations", existing.stream().map(r -> r.code() + ": " + r.content()).toList());
        input.put("similarRecommendationsFromOtherAudits", similar.stream().map(RecommendationDraftResponse.SimilarRecommendation::content).toList());
        String inputJson = toJson(input);

        String system = """
                Ban la tro ly soan thao kiem toan noi bo ngan hang (A4). Nhiem vu: tu cac nhom ton tai sai sot \
                (TTSS) trong du lieu, soan BAN NHAP kien nghi bang tieng Viet trang trong, ngan gon (toi da 600 ky tu \
                moi kien nghi), theo mau: "Đề nghị <đơn vị/bộ phận> <việc cần làm cụ thể> đối với <phạm vi TTSS>; \
                <biện pháp phòng ngừa tái diễn>."
                QUY TAC:
                1. Moi kien nghi phai dua tren it nhat 1 nhom TTSS, ghi ma phat hien (findingCode) vao findingCodes - \
                   CHI dung ma co trong du lieu.
                2. KHONG bia so tien, ty le, ten nguoi, ten khach hang, so van ban khong co trong du lieu.
                3. Khong lap lai kien nghi da co (existingRecommendations). Nhom cac TTSS cung ban chat vao 1 kien nghi.
                4. Uu tien TTSS trong yeu (material). Toi da 5 kien nghi.
                5. "rationale": 1 cau giai thich vi sao de xuat, dua tren du lieu.
                6. Tham khao cach viet o similarRecommendationsFromOtherAudits neu phu hop, nhung khong chep nguyen.
                PHAI tra loi bang cach goi tool "%s".
                """.formatted(DRAFT_TOOL);
        String user = "Du lieu (JSON):\n" + inputJson
                + (request.instruction() == null || request.instruction().isBlank() ? "" : "\nYeu cau them cua nguoi dung: " + request.instruction());

        Map<String, Object> args = callForTool(system, user, draftToolSpec(), DRAFT_TOOL, principal, "AI soan kien nghi " + engagement.code());
        String sourceText = inputJson;
        List<RecommendationDraftResponse.Draft> drafts = new ArrayList<>();
        for (Map<String, Object> d : mapList(args.get("drafts"))) {
            String content = trim(String.valueOf(d.getOrDefault("content", "")));
            if (content.isBlank()) {
                continue;
            }
            List<String> codes = stringList(d.get("findingCodes"));
            List<String> validCodes = codes.stream().filter(knownCodes::contains).toList();
            boolean grounded = !validCodes.isEmpty() && validCodes.size() == codes.size() && numbersFrom(content, sourceText);
            drafts.add(new RecommendationDraftResponse.Draft(content, validCodes, stringOrNull(d.get("rationale")), grounded));
        }
        if (drafts.isEmpty() && args.get("_freeText") instanceof String freeText && !freeText.isBlank()) {
            drafts.add(new RecommendationDraftResponse.Draft(trim(freeText), List.of(), null, false));
        }

        RecommendationDraftResponse response = new RecommendationDraftResponse(engagement.code(), segmentCode, source.size(),
                drafts, similar, llmProvider.modelId());
        auditLogService.logToolCall(principal.userId(), UUID.randomUUID(), 0, "AI soan kien nghi " + engagement.code(),
                "draft_recommendations", Map.of("engagement", engagement.code(), "businessSegment", String.valueOf(segmentCode),
                        "sourceTtss", source.size()),
                ToolExecutionResult.success(toJson(drafts)), System.currentTimeMillis() - start);
        return response;
    }

    public RewriteResponse rewrite(RewriteRequest request, CurrentUserPrincipal principal) {
        requireEnabled();
        long start = System.currentTimeMillis();
        String kind = switch (request.purpose()) {
            case RECOMMENDATION -> "kien nghi kiem toan";
            case FINDING -> "mo ta phat hien/ton tai sai sot";
            case SUMMARY -> "tom tat bao cao kiem toan";
        };
        String system = """
                Ban la bien tap vien van ban kiem toan noi bo ngan hang. Viet lai doan %s duoi day cho ro rang, trang \
                trong, dung chinh ta tieng Viet, cau ngan gon, mach lac.
                QUY TAC: GIU NGUYEN moi su kien, so lieu, ten, ma, moc thoi gian; KHONG them thong tin moi; KHONG bo y \
                chinh. "changes": liet ke ngan gon cac thay doi chinh (toi da 5 dong).
                PHAI tra loi bang cach goi tool "%s".
                """.formatted(kind, REWRITE_TOOL);
        Map<String, Object> args = callForTool(system, request.text(), rewriteToolSpec(), REWRITE_TOOL, principal, "AI chuan hoa cau chu");
        String text = args.get("text") instanceof String s && !s.isBlank() ? s.trim()
                : args.get("_freeText") instanceof String f ? f.trim() : "";
        if (text.isBlank()) {
            throw new BusinessException("AGENT_REWRITE_EMPTY", "AI chua viet lai duoc doan van nay, vui long thu lai", HttpStatus.BAD_GATEWAY);
        }
        text = trim(text);
        RewriteResponse response = new RewriteResponse(text, stringList(args.get("changes")), numbersFrom(text, request.text()), llmProvider.modelId());
        auditLogService.logToolCall(principal.userId(), UUID.randomUUID(), 0, "AI chuan hoa cau chu (" + request.purpose() + ")",
                "rewrite_text", Map.of("purpose", request.purpose().name(), "length", request.text().length()),
                ToolExecutionResult.success(text), System.currentTimeMillis() - start);
        return response;
    }

    /**
     * "AI soạn thư đôn đốc" (A5, G3): 1 thu cho moi don vi co kien nghi qua han/sap den han. Danh sach kien
     * nghi trong thu do he thong dung tu du lieu that; AI chi viet tieu de + loi mo dau + loi ket (dung chung
     * cho moi don vi, cho trong {don_vi}). Khong gui thu, khong doi hien trang.
     */
    public ReminderDraftResponse draftReminder(ReminderDraftRequest request, CurrentUserPrincipal principal) {
        if (!agentProperties.isAgentEnabled(AgentProfileRegistry.REMEDIATION)) {
            throw new BusinessException("AGENT_DISABLED", "Tro ly AI theo doi khac phuc dang duoc tat theo cau hinh", HttpStatus.SERVICE_UNAVAILABLE);
        }
        long start = System.currentTimeMillis();
        int days = request.dueWithinDays() == null || request.dueWithinDays() <= 0 ? 30 : request.dueWithinDays();
        String sourceLabel = planTdkpTools.sourceLabel(request.source());
        List<AgentPlanTdkpToolsService.TdkpItem> items = planTdkpTools.itemsForReminder(request.source(), request.itemIds(), days).stream()
                .filter(i -> !i.done())
                .toList();
        if (items.isEmpty()) {
            throw new BusinessException("AGENT_REMINDER_NO_ITEMS",
                    "Khong co kien nghi nao chua hoan thanh, qua han hoac sap den han de soan thu don doc", HttpStatus.BAD_REQUEST);
        }
        Map<String, List<AgentPlanTdkpToolsService.TdkpItem>> byUnit = items.stream()
                .collect(Collectors.groupingBy(i -> i.unit() == null || i.unit().isBlank() ? "(Chưa xác định đơn vị)" : i.unit(),
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("danhSach", sourceLabel);
        input.put("soDonVi", byUnit.size());
        input.put("soKienNghi", items.size());
        input.put("soQuaHan", items.stream().filter(i -> i.daysOverdue() != null && i.daysOverdue() > 0).count());
        input.put("viDu", items.stream().limit(5).map(i -> i.code() + ": " + (i.content() == null ? "" : i.content().length() > 150 ? i.content().substring(0, 150) : i.content())).toList());
        String inputJson = toJson(input);
        String system = """
                Ban la can bo Kiem toan noi bo ngan hang soan cong van DON DOC thuc hien kien nghi. Viet tieng Viet trang \
                trong, lich su, ngan gon. Chi viet: "subject" (tieu de, toi da 120 ky tu), "opening" (1 doan mo dau: can cu \
                theo doi khac phuc, de nghi don vi khan truong thuc hien cac kien nghi o danh sach ben duoi) va "closing" \
                (1 doan ket: de nghi bao cao ket qua/ly do cham tre va gui bang chung khac phuc, cam on). Dung dung chuoi \
                {don_vi} o cho can ten don vi. KHONG liet ke kien nghi, KHONG viet so lieu, ngay thang, ten nguoi - he \
                thong tu chen danh sach chinh xac. PHAI tra loi bang cach goi tool "%s".
                """.formatted(REMINDER_TOOL);
        Map<String, Object> args = callForTool(system, "Thong tin (JSON):\n" + inputJson, reminderToolSpec(), REMINDER_TOOL, principal,
                "AI soan thu don doc " + request.source());
        String subject = stringOrNull(args.get("subject"));
        String opening = stringOrNull(args.get("opening"));
        String closing = stringOrNull(args.get("closing"));
        boolean grounded = subject != null && opening != null && closing != null
                && numbersFrom(subject + " " + opening + " " + closing, inputJson);
        if (subject == null || opening == null || closing == null) {
            // Model khong tra dung cau truc - dung mau co dinh de nguoi dung van co ban nhap dung so lieu
            subject = "Đôn đốc thực hiện kiến nghị của Kiểm toán nội bộ";
            opening = "Căn cứ kết quả theo dõi khắc phục, Kiểm toán nội bộ đề nghị {don_vi} khẩn trương thực hiện các kiến nghị dưới đây:";
            closing = "Đề nghị {don_vi} báo cáo kết quả thực hiện (hoặc lý do chậm trễ) kèm bằng chứng khắc phục về Kiểm toán nội bộ. Trân trọng cảm ơn.";
        }

        List<ReminderDraftResponse.Letter> letters = new ArrayList<>();
        for (Map.Entry<String, List<AgentPlanTdkpToolsService.TdkpItem>> e : byUnit.entrySet()) {
            String unit = e.getKey();
            List<ReminderDraftResponse.Item> letterItems = e.getValue().stream()
                    .sorted(Comparator.comparing(AgentPlanTdkpToolsService.TdkpItem::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(i -> new ReminderDraftResponse.Item(i.code(), i.content(), i.deadline(), i.daysOverdue(), i.statusLabel()))
                    .toList();
            StringBuilder body = new StringBuilder("Kính gửi: ").append(unit).append("\n\n")
                    .append(opening.replace("{don_vi}", unit)).append("\n\n");
            int n = 0;
            for (ReminderDraftResponse.Item item : letterItems) {
                body.append(++n).append(". ").append(item.code() == null ? "" : "[" + item.code() + "] ")
                        .append(item.content() == null ? "" : item.content().length() > 400 ? item.content().substring(0, 400) + "..." : item.content())
                        .append("\n   Thời hạn: ").append(item.deadline() == null ? "chưa xác định" : item.deadline().format(VN_DATE))
                        .append(daysText(item.daysOverdue()))
                        .append(" - Hiện trạng: ").append(item.status() == null ? "chưa cập nhật" : item.status()).append('\n');
            }
            body.append('\n').append(closing.replace("{don_vi}", unit));
            letters.add(new ReminderDraftResponse.Letter(unit, subject.replace("{don_vi}", unit), body.toString(), letterItems));
        }
        ReminderDraftResponse response = new ReminderDraftResponse(request.source(), sourceLabel, letters, grounded, llmProvider.modelId());
        auditLogService.logToolCall(principal.userId(), UUID.randomUUID(), 0, "AI soan thu don doc " + request.source(),
                "draft_reminder", Map.of("source", request.source(), "items", items.size(), "units", byUnit.size()),
                ToolExecutionResult.success(toJson(Map.of("subject", subject, "opening", opening, "closing", closing))),
                System.currentTimeMillis() - start);
        return response;
    }

    private static String daysText(Long daysOverdue) {
        if (daysOverdue == null) {
            return "";
        }
        if (daysOverdue > 0) {
            return " (quá hạn " + daysOverdue + " ngày)";
        }
        return daysOverdue == 0 ? " (đến hạn hôm nay)" : " (còn " + (-daysOverdue) + " ngày)";
    }

    private static ToolSpec reminderToolSpec() {
        return new ToolSpec(REMINDER_TOOL, "Nop mau thu don doc (tieu de, mo dau, ket) - he thong tu chen danh sach kien nghi",
                Map.of("type", "object", "properties", Map.of(
                                "subject", Map.of("type", "string", "description", "Tieu de cong van"),
                                "opening", Map.of("type", "string", "description", "Doan mo dau, dung {don_vi}"),
                                "closing", Map.of("type", "string", "description", "Doan ket, dung {don_vi}")),
                        "required", List.of("subject", "opening", "closing")));
    }

    // ------------------------------------------------------------------ helpers

    private void requireEnabled() {
        if (!agentProperties.isAgentEnabled(AgentProfileRegistry.FINDING)) {
            throw new BusinessException("AGENT_DISABLED", "Tro ly AI soan thao dang duoc tat theo cau hinh he thong", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** 1 lan goi model, bat buoc tra qua 1 tool co cau truc; neu model tra text tu do thi nhac 1 lan, van
     * khong duoc thi tra text do o khoa "_freeText" de nguoi goi tu xu ly (danh dau chua kiem chung). */
    private Map<String, Object> callForTool(String system, String user, ToolSpec tool, String toolName,
                                            CurrentUserPrincipal principal, String logQuestion) {
        List<ChatMessage> messages = new ArrayList<>(List.of(ChatMessage.system(system), ChatMessage.user(user)));
        String freeText = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            ChatResult result;
            try {
                result = llmProvider.chat(messages, List.of(tool));
            } catch (RuntimeException e) {
                log.warn("LLM khong phan hoi khi soan nhap ({}): {}", llmProvider.modelId(), e.getMessage());
                auditLogService.logError(principal.userId(), UUID.randomUUID(), 0, logQuestion, "LLM_UNAVAILABLE: " + e.getMessage());
                throw new BusinessException("AGENT_LLM_UNAVAILABLE",
                        "Tro ly AI tam thoi chua phan hoi duoc, vui long thu lai sau. Ban van nhap tay binh thuong duoc.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            if (result.hasToolCalls()) {
                for (ToolCallRequest call : result.toolCalls()) {
                    if (toolName.equals(call.name())) {
                        return call.arguments() == null ? Map.of() : call.arguments();
                    }
                }
            }
            freeText = result.finalMessage() == null ? null : result.finalMessage().content();
            messages.add(ChatMessage.assistant(freeText));
            messages.add(ChatMessage.user("Hay goi tool '" + toolName + "' voi dung noi dung do thay vi viet text tu do."));
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("_freeText", freeText);
        return fallback;
    }

    private Map<String, Object> findingGroup(String code, List<AuditTtssRecordResponse> records) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("findingCode", code);
        m.put("findingName", records.get(0).findingName());
        m.put("count", records.size());
        m.put("material", records.stream().filter(AuditTtssRecordResponse::material).count());
        m.put("processSteps", records.stream().map(AuditTtssRecordResponse::processStepSummaryName).filter(Objects::nonNull).distinct().limit(3).toList());
        m.put("totalAmount", records.stream().map(AuditTtssRecordResponse::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("examples", records.stream().map(AuditTtssRecordResponse::ttssContent).filter(Objects::nonNull)
                .map(t -> t.length() > 250 ? t.substring(0, 250) + "..." : t).distinct().limit(3).toList());
        m.put("uploaderSuggestions", records.stream().map(AuditTtssRecordResponse::uploaderRecommendationName).filter(Objects::nonNull).distinct().limit(3).toList());
        return m;
    }

    private List<RecommendationDraftResponse.SimilarRecommendation> similarRecommendations(CurrentUserPrincipal principal,
                                                                                          AuditEngagementResponse engagement,
                                                                                          List<Map<String, Object>> findings) {
        Set<String> seen = new LinkedHashSet<>();
        List<RecommendationDraftResponse.SimilarRecommendation> result = new ArrayList<>();
        for (Map<String, Object> f : findings.stream().limit(3).toList()) {
            Object name = f.get("findingName");
            if (name == null || String.valueOf(name).isBlank()) {
                continue;
            }
            try {
                Map<String, Object> hits = workTools.searchSimilarFindings(principal, String.valueOf(name), engagement.code(), 3);
                for (Map<String, Object> r : mapList(hits.get("recommendations"))) {
                    String content = String.valueOf(r.get("content"));
                    if (seen.add(content)) {
                        result.add(new RecommendationDraftResponse.SimilarRecommendation(String.valueOf(r.get("engagementCode")),
                                String.valueOf(r.get("code")), content));
                    }
                }
            } catch (RuntimeException e) {
                // tham khao thi co thi tot, khong co van soan duoc
            }
        }
        return result.stream().limit(5).toList();
    }

    /** Moi so (>= 3 chu so) trong ban nhap phai xuat hien trong du lieu goc - chan so lieu tu bia. */
    private static boolean numbersFrom(String text, String source) {
        String normalizedSource = source.replaceAll("[.,\\s]", "");
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String digits = m.group().replaceAll("[.,]", "");
            if (!normalizedSource.contains(digits)) {
                return false;
            }
        }
        return true;
    }

    private static ToolSpec draftToolSpec() {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("type", "object");
        draft.put("properties", Map.of(
                "content", Map.of("type", "string", "description", "Noi dung kien nghi (ban nhap)"),
                "findingCodes", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Ma phat hien lam can cu"),
                "rationale", Map.of("type", "string", "description", "1 cau giai thich can cu")));
        draft.put("required", List.of("content", "findingCodes"));
        return new ToolSpec(DRAFT_TOOL, "Nop danh sach ban nhap kien nghi",
                Map.of("type", "object", "properties", Map.of("drafts", Map.of("type", "array", "items", draft)), "required", List.of("drafts")));
    }

    private static ToolSpec rewriteToolSpec() {
        return new ToolSpec(REWRITE_TOOL, "Nop doan van da viet lai",
                Map.of("type", "object", "properties", Map.of(
                                "text", Map.of("type", "string", "description", "Doan van da viet lai"),
                                "changes", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Cac thay doi chinh")),
                        "required", List.of("text")));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(i -> i instanceof Map<?, ?>).map(i -> (Map<String, Object>) i).toList();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private static String stringOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static String trim(String text) {
        return text.length() > MAX_CONTENT ? text.substring(0, MAX_CONTENT) : text;
    }
}
