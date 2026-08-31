package com.govia.audit.agent.service;

import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.audit.agent.tools.ToolExecutionResult;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Ghi nhat ky cho AI Agent - moi lan goi tool va moi cau tra loi cuoi. KHONG BAO GIO nhan/ghi
 * Authorization header, token, password vao day - cac method o day chi nhan du lieu nghiep vu
 * (cau hoi, ten tool, tham so, tom tat ket qua).
 */
@Service
public class AgentAuditLogService {

    private static final int MAX_LENGTH = 4000;

    private final AgentAuditLogRepository repository;

    public AgentAuditLogService(AgentAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logToolCall(UUID userId, UUID conversationId, int turnSeq, String userQuestion,
                             String toolName, Map<String, Object> arguments, ToolExecutionResult result, long latencyMs) {
        AgentAuditLog log = base(userId, conversationId, turnSeq, AgentAuditLog.EventType.TOOL_CALL, userQuestion);
        log.setToolName(toolName);
        log.setToolArguments(truncate(String.valueOf(arguments)));
        log.setToolStatus(result.status().name());
        log.setResponseSummary(truncate(result.status() == ToolExecutionResult.Status.SUCCESS ? result.body() : result.message()));
        log.setLatencyMs(latencyMs);
        repository.save(log);
    }

    @Transactional
    public void logFinalAnswer(UUID userId, UUID conversationId, int turnSeq, String userQuestion,
                                String answer, long latencyMs) {
        AgentAuditLog log = base(userId, conversationId, turnSeq, AgentAuditLog.EventType.FINAL_ANSWER, userQuestion);
        log.setResponseSummary(truncate(answer));
        log.setLatencyMs(latencyMs);
        repository.save(log);
    }

    @Transactional
    public void logError(UUID userId, UUID conversationId, int turnSeq, String userQuestion, String errorMessage) {
        AgentAuditLog log = base(userId, conversationId, turnSeq, AgentAuditLog.EventType.ERROR, userQuestion);
        log.setResponseSummary(truncate(errorMessage));
        repository.save(log);
    }

    private AgentAuditLog base(UUID userId, UUID conversationId, int turnSeq, AgentAuditLog.EventType type, String userQuestion) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setUserId(userId);
        log.setConversationId(conversationId);
        log.setTurnSeq(turnSeq);
        log.setEventType(type);
        log.setUserQuestion(truncate(userQuestion));
        return log;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }
}
