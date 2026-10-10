package com.govia.audit.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.entity.AgentMessage;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.audit.agent.repository.AgentMessageRepository;
import com.govia.audit.agent.repository.AgentSuggestionRepository;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thong ke su dung va chat luong Tro ly AI (G4) - do cac KPI cua phuong an tu 3 bang rieng cua module agent
 * (agent_tool_call_log, agent_message, agent_suggestion). Chi doc; khong cham bang nghiep vu nao.
 * KPI muc tieu theo phuong an: grounded >= 80%, 90% luot tra loi < 15 giay, 0 su co du lieu ngoai quyen.
 */
@Service
public class AgentKpiService {

    static final double GROUNDED_TARGET = 0.80;
    static final long LATENCY_TARGET_MS = 15_000;
    static final double LATENCY_TARGET_SHARE = 0.90;
    private static final List<String> DRAFT_TOOLS = List.of("draft_recommendations", "rewrite_text", "draft_reminder", "draft_dgcl_score",
            "check_import_file");

    private final AgentAuditLogRepository auditLogRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentSuggestionRepository suggestionRepository;
    private final ObjectMapper objectMapper;

    public AgentKpiService(AgentAuditLogRepository auditLogRepository, AgentMessageRepository messageRepository,
                           AgentSuggestionRepository suggestionRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.messageRepository = messageRepository;
        this.suggestionRepository = suggestionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> kpi(Integer days) {
        int period = days == null || days <= 0 ? 30 : Math.min(days, 365);
        Instant from = Instant.now().minus(period, ChronoUnit.DAYS);
        UUID tenantId = TenantContext.getTenantId();
        List<AgentAuditLog> logs = auditLogRepository.findByTenantIdAndCreatedAtAfter(tenantId, from);
        List<AgentMessage> answers = messageRepository.findByTenantIdAndCreatedAtAfter(tenantId, from).stream()
                .filter(m -> m.getRole() == AgentMessage.Role.ASSISTANT).toList();

        List<Long> latencies = logs.stream().filter(l -> l.getEventType() == AgentAuditLog.EventType.FINAL_ANSWER)
                .map(AgentAuditLog::getLatencyMs).filter(Objects::nonNull).sorted().toList();
        long groundedYes = 0;
        long groundedKnown = 0;
        Map<String, Long> byAgent = new TreeMap<>();
        Map<String, long[]> groundedByAgent = new TreeMap<>();
        for (AgentMessage m : answers) {
            String agent = m.getAgentCode() == null ? "-" : m.getAgentCode();
            byAgent.merge(agent, 1L, Long::sum);
            Boolean grounded = grounded(m.getResponseJson());
            if (grounded != null) {
                groundedKnown++;
                long[] g = groundedByAgent.computeIfAbsent(agent, k -> new long[2]);
                g[1]++;
                if (grounded) {
                    groundedYes++;
                    g[0]++;
                }
            }
        }
        long withinTarget = latencies.stream().filter(l -> l < LATENCY_TARGET_MS).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodDays", period);
        result.put("from", from);
        result.put("questions", answers.size());
        result.put("activeUsers", logs.stream().map(AgentAuditLog::getUserId).distinct().count());
        result.put("answersByAgent", byAgent);
        result.put("groundedRate", groundedKnown == 0 ? null : ratio(groundedYes, groundedKnown));
        result.put("groundedRateByAgent", groundedByAgent.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ratio(e.getValue()[0], e.getValue()[1]), (a, b) -> a, TreeMap::new)));
        result.put("latencyP50Ms", percentile(latencies, 0.5));
        result.put("latencyP90Ms", percentile(latencies, 0.9));
        result.put("answersUnder15sRate", latencies.isEmpty() ? null : ratio(withinTarget, latencies.size()));
        result.put("llmErrors", logs.stream().filter(l -> l.getEventType() == AgentAuditLog.EventType.ERROR).count());
        result.put("toolCalls", logs.stream().filter(l -> l.getEventType() == AgentAuditLog.EventType.TOOL_CALL && !DRAFT_TOOLS.contains(l.getToolName())).count());
        result.put("toolCallsDenied", logs.stream().filter(l -> "FORBIDDEN".equals(l.getToolStatus())).count());
        Map<String, Long> drafts = new LinkedHashMap<>();
        for (String tool : DRAFT_TOOLS) {
            drafts.put(tool, logs.stream().filter(l -> tool.equals(l.getToolName())).count());
        }
        result.put("drafts", drafts);
        result.put("suggestionsGenerated", suggestionRepository.countByTenantIdAndCreatedAtAfter(tenantId, from));

        List<Map<String, Object>> targets = new ArrayList<>();
        targets.add(target("Tỷ lệ câu trả lời có căn cứ (grounded)", "≥ 80%", "PERCENT", result.get("groundedRate"),
                groundedKnown == 0 ? null : ratio(groundedYes, groundedKnown) >= GROUNDED_TARGET));
        targets.add(target("Câu trả lời dưới 15 giây", "≥ 90% lượt", "PERCENT", result.get("answersUnder15sRate"),
                latencies.isEmpty() ? null : ratio(withinTarget, latencies.size()) >= LATENCY_TARGET_SHARE));
        targets.add(target("Lượt tra cứu bị chặn vì không đủ quyền", "Chỉ để theo dõi - hệ thống đã chặn đúng", "COUNT", result.get("toolCallsDenied"), null));
        result.put("targets", targets);
        return result;
    }

    private Boolean grounded(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json).path("metadata").path("grounded");
            return node.isBoolean() ? node.asBoolean() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> target(String name, String goal, String unit, Object value, Boolean met) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("goal", goal);
        m.put("unit", unit);
        m.put("value", value);
        m.put("met", met);
        return m;
    }

    private static double ratio(long a, long b) {
        return b == 0 ? 0 : Math.round(10000.0 * a / b) / 10000.0;
    }

    private static Long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return null;
        }
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
