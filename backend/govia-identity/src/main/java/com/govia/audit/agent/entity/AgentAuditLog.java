package com.govia.audit.agent.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * 1 dong nhat ky cho AI Agent - hoac 1 lan goi tool (event_type=TOOL_CALL), hoac cau tra loi cuoi
 * cua 1 luot (FINAL_ANSWER), hoac 1 loi (ERROR). Khong tai dung AuditLog dung chung vi cot "detail"
 * cua bang do gioi han 4000 ky tu - khong du cho response cua mot so tool (vd get_risk_history).
 * TUYET DOI KHONG ghi Authorization header/token/password vao bat ky cot nao o day.
 */
@Getter
@Setter
@Entity
@Table(name = "agent_tool_call_log")
public class AgentAuditLog extends BaseEntity {

    public enum EventType { TOOL_CALL, FINAL_ANSWER, ERROR }

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    private UUID conversationId;

    @Column(name = "turn_seq", nullable = false)
    private int turnSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "user_question", length = 4000)
    private String userQuestion;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Column(name = "tool_arguments", length = 4000)
    private String toolArguments;

    @Column(name = "tool_status", length = 20)
    private String toolStatus;

    @Column(name = "response_summary", length = 4000)
    private String responseSummary;

    @Column(name = "latency_ms")
    private Long latencyMs;
}
