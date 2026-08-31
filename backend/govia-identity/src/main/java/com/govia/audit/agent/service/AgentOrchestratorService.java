package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.AgentChatResponse;
import com.govia.audit.agent.dto.AgentMetadata;
import com.govia.audit.agent.dto.EvidenceRef;
import com.govia.audit.agent.llm.ChatMessage;
import com.govia.audit.agent.llm.ChatResult;
import com.govia.audit.agent.llm.LlmProvider;
import com.govia.audit.agent.llm.ToolCallRequest;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.tools.AuditToolDefinition;
import com.govia.audit.agent.tools.AuditToolExecutor;
import com.govia.audit.agent.tools.AuditToolRegistry;
import com.govia.audit.agent.tools.ToolExecutionResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agent Core - vong lap multi-step: hoi LLM, thuc thi tool model yeu cau, hoi lai, cho toi khi model
 * goi tool dac biet "submit_final_answer" hoac vuot qua {@link #MAX_TOOL_ROUNDS}. Chi phu thuoc
 * {@link LlmProvider} (khong bao gio goi thang Ollama/OpenAI SDK o day) va goi Audit Tools qua
 * {@link AuditToolExecutor} (di qua HTTP that, mang theo quyen cua user - xem class do).
 */
@Service
public class AgentOrchestratorService {

    private static final int MAX_TOOL_ROUNDS = 5;

    private final LlmProvider llmProvider;
    private final AuditToolExecutor toolExecutor;
    private final ConversationStore conversationStore;
    private final AgentAuditLogService auditLogService;
    private final List<ToolSpec> allToolSpecs;

    public AgentOrchestratorService(LlmProvider llmProvider, AuditToolRegistry toolRegistry,
                                     AuditToolExecutor toolExecutor, ConversationStore conversationStore,
                                     AgentAuditLogService auditLogService) {
        this.llmProvider = llmProvider;
        this.toolExecutor = toolExecutor;
        this.conversationStore = conversationStore;
        this.auditLogService = auditLogService;
        this.allToolSpecs = toolRegistry.allDefinitions().stream().map(AuditToolDefinition::toToolSpec).toList();
    }

    public AgentChatResponse chat(UUID conversationId, String userMessage, UUID userId) {
        long turnStart = System.currentTimeMillis();
        int turnSeq = conversationStore.nextTurnSeq(conversationId);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt()));
        messages.addAll(conversationStore.history(conversationId));
        ChatMessage userChatMessage = ChatMessage.user(userMessage);
        messages.add(userChatMessage);

        Set<String> toolsUsedThisTurn = new HashSet<>();
        Set<String> forbiddenThisTurn = new HashSet<>();
        Map<String, StringBuilder> rawResponsesByTool = new HashMap<>();

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatResult result = llmProvider.chat(messages, allToolSpecs);

            if (!result.hasToolCalls()) {
                String freeText = result.finalMessage() != null ? result.finalMessage().content() : null;
                if (round < MAX_TOOL_ROUNDS - 1) {
                    // Model tra text tu do thay vi goi submit_final_answer - nhac lai 1 lan thay vi
                    // chap nhan ngay, model 7B doi luc bo qua yeu cau goi tool o buoc cuoi cung.
                    messages.add(ChatMessage.assistant(freeText));
                    messages.add(ChatMessage.user(
                            "Ban vua tra loi bang text tu do. Hay goi tool 'submit_final_answer' voi dung "
                                    + "noi dung do (answer/facts/analysis/recommendations/evidence) thay vi viet text truc tiep."));
                    continue;
                }
                // Het luot nhac ma model van khong goi tool - dung text tho lam cau tra loi cuoi,
                // van tot hon la bat loi/loop vo han.
                AgentChatResponse fallback = fallbackResponse(freeText, toolsUsedThisTurn);
                persistAndLog(userId, conversationId, turnSeq, userMessage, userChatMessage, fallback, turnStart);
                return fallback;
            }

            ToolCallRequest finalCall = result.toolCalls().stream()
                    .filter(c -> AuditToolRegistry.FINAL_ANSWER_TOOL_NAME.equals(c.name()))
                    .findFirst().orElse(null);
            if (finalCall != null) {
                AgentChatResponse response = buildFinalAnswer(finalCall.arguments(), toolsUsedThisTurn, rawResponsesByTool);
                persistAndLog(userId, conversationId, turnSeq, userMessage, userChatMessage, response, turnStart);
                return response;
            }

            for (ToolCallRequest call : result.toolCalls()) {
                long start = System.currentTimeMillis();
                ToolExecutionResult execResult;
                if (forbiddenThisTurn.contains(call.name())) {
                    execResult = ToolExecutionResult.forbidden("Da bi tu choi quyen truoc do trong luot nay - khong thu lai");
                } else {
                    execResult = toolExecutor.execute(call.name(), call.arguments());
                }
                long latency = System.currentTimeMillis() - start;
                auditLogService.logToolCall(userId, conversationId, turnSeq, userMessage, call.name(), call.arguments(), execResult, latency);

                if (execResult.status() == ToolExecutionResult.Status.FORBIDDEN) {
                    forbiddenThisTurn.add(call.name());
                } else if (execResult.status() == ToolExecutionResult.Status.SUCCESS) {
                    toolsUsedThisTurn.add(call.name());
                    rawResponsesByTool.computeIfAbsent(call.name(), k -> new StringBuilder()).append(execResult.body()).append('\n');
                }

                messages.add(assistantToolCallMessage(call));
                messages.add(ChatMessage.toolResult(call.name(), toolResultContent(execResult)));
            }
        }

        AgentChatResponse capped = cappedResponse(toolsUsedThisTurn, rawResponsesByTool);
        persistAndLog(userId, conversationId, turnSeq, userMessage, userChatMessage, capped, turnStart);
        return capped;
    }

    private AgentChatResponse buildFinalAnswer(Map<String, Object> args, Set<String> toolsUsedThisTurn,
                                                Map<String, StringBuilder> rawResponsesByTool) {
        String answer = String.valueOf(args.getOrDefault("answer", ""));
        List<String> facts = stringList(args.get("facts"));
        List<String> analysis = stringList(args.get("analysis"));
        List<String> recommendations = stringList(args.get("recommendations"));
        List<Map<String, Object>> rawEvidence = mapList(args.get("evidence"));

        boolean grounded = true;
        List<EvidenceRef> evidence = new ArrayList<>();
        for (Map<String, Object> e : rawEvidence) {
            String tool = String.valueOf(e.get("tool"));
            if (!toolsUsedThisTurn.contains(tool)) {
                grounded = false;
                continue;
            }
            Map<String, Object> keyData = asMap(e.get("keyData"));
            String rawForTool = rawResponsesByTool.getOrDefault(tool, new StringBuilder()).toString();
            boolean allNumbersVerified = keyData.values().stream()
                    .filter(v -> v instanceof Number)
                    .allMatch(v -> rawForTool.contains(String.valueOf(v)));
            if (!allNumbersVerified) {
                grounded = false;
                continue;
            }
            evidence.add(new EvidenceRef(tool, asMap(e.get("args")), keyData));
        }

        return new AgentChatResponse(answer, facts, analysis, recommendations, evidence,
                new AgentMetadata(llmProvider.modelId(), Instant.now(), List.copyOf(toolsUsedThisTurn), false, grounded));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add((Map<String, Object>) m);
            }
        }
        return result;
    }

    private AgentChatResponse fallbackResponse(String rawText, Set<String> toolsUsedThisTurn) {
        String answer = (rawText == null || rawText.isBlank())
                ? "Hien chua co du lieu trong he thong de ket luan."
                : rawText;
        return new AgentChatResponse(answer, List.of(), List.of(), List.of(), List.of(),
                new AgentMetadata(llmProvider.modelId(), Instant.now(), List.copyOf(toolsUsedThisTurn), false, false));
    }

    private AgentChatResponse cappedResponse(Set<String> toolsUsedThisTurn, Map<String, StringBuilder> rawResponsesByTool) {
        List<String> facts = rawResponsesByTool.entrySet().stream()
                .map(e -> e.getKey() + " tra ve: " + trim(e.getValue().toString(), 300))
                .toList();
        String answer = toolsUsedThisTurn.isEmpty()
                ? "Hien chua co du lieu trong he thong de ket luan."
                : "Da dat gioi han so buoc xu ly (" + MAX_TOOL_ROUNDS + " vong goi tool). Duoi day la du lieu da thu thap duoc, chua du de ket luan day du.";
        return new AgentChatResponse(answer, facts, List.of(), List.of(), List.of(),
                new AgentMetadata(llmProvider.modelId(), Instant.now(), List.copyOf(toolsUsedThisTurn), true, !facts.isEmpty()));
    }

    private void persistAndLog(UUID userId, UUID conversationId, int turnSeq, String userMessage,
                                ChatMessage userChatMessage, AgentChatResponse response, long turnStart) {
        conversationStore.append(conversationId, turnSeq, userChatMessage, ChatMessage.assistant(response.answer()));
        auditLogService.logFinalAnswer(userId, conversationId, turnSeq, userMessage, response.answer(),
                System.currentTimeMillis() - turnStart);
    }

    private ChatMessage assistantToolCallMessage(ToolCallRequest call) {
        return new ChatMessage("assistant", null, null, List.of(call));
    }

    private String toolResultContent(ToolExecutionResult result) {
        return switch (result.status()) {
            case SUCCESS -> result.body();
            case FORBIDDEN -> "{\"error\":\"FORBIDDEN\",\"message\":\"" + result.message() + "\"}";
            case ERROR -> "{\"error\":\"TOOL_ERROR\",\"message\":\"" + result.message() + "\"}";
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private String trim(String value, int max) {
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }

    private String systemPrompt() {
        return """
                Ban la Audit AI Assistant, ho tro kiem toan vien phan tich rui ro chi nhanh trong he \
                thong GOVIA. Ban CHI duoc tra loi dua tren du lieu lay duoc tu cac tool duoc cung cap \
                (goi la Audit Tools) - day la nguon du lieu that DUY NHAT, khong duoc dung kien thuc \
                ngoai de khang dinh 1 su that ve du lieu noi bo.

                QUY TAC BAT BUOC:
                1. Diem rui ro (risk score) va xep loai (rank/risk level) tu tool la nguon chuan (source \
                   of truth) - KHONG tu tinh lai, KHONG doan, KHONG lam tron/sua doi.
                2. Neu tool tra ve null hoac mang rong: phai noi ro "Hien chua co du lieu trong he thong \
                   de ket luan" cho phan do - KHONG duoc suy dien thanh 1 gia tri, KHONG bien mang rong \
                   thanh "khong co van de gi".
                3. KHONG bia so lieu, KHONG bia ten chi nhanh, KHONG bia audit finding, KHONG bia evidence.
                4. Neu 1 tool tra loi khong co quyen (FORBIDDEN): noi ro voi nguoi dung la khong lay duoc \
                   du lieu do do khong du quyen - KHONG thu goi tool khac de lay du lieu tuong tu thay the.
                5. Cau hoi ngoai pham vi du lieu kiem toan/rui ro chi nhanh: tra loi ngan gon la ban chi \
                   ho tro cac cau hoi ve du lieu chấm điểm rủi ro/audit finding, khong goi tool nao ca.
                6. Voi cau hoi can nhieu buoc (vd "tai sao chi nhanh X co rui ro cao"): goi lan luot \
                   nhieu tool can thiet (vd get_branch_risk roi get_risk_breakdown) truoc khi tra loi.
                7. Khi da du du lieu, PHAI ket thuc bang cach goi tool "submit_final_answer" - KHONG duoc \
                   tra loi bang text tu do. "facts" chi ghi du lieu lay truc tiep tu tool. "analysis" la \
                   nhan dinh dua tren facts. "recommendations" la de xuat cua ban, phai noi ro day la de \
                   xuat chu khong phai su that da xac nhan. "evidence" liet ke moi tool DA THUC SU goi. \
                   "answer" viet cho nguoi dung cuoi doc - KHONG nhac ten tool/tham so ky thuat trong \
                   "answer", nhung thong tin do chi thuoc ve "evidence".
                8. Khong hien thi qua trinh suy nghi noi bo cho nguoi dung, chi dua ra ket luan va can cu.
                """;
    }
}
