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
 * 1 tin nhan trong agent_conversation. Cot van ban dung VARCHAR(4000) (khong CLOB) theo quy uoc chung
 * cua du an de cot vat ly giong nhau tren Postgres/H2/Oracle - noi dung dai hon duoc rut gon khi luu.
 */
@Getter
@Setter
@Entity
@Table(name = "agent_message")
public class AgentMessage extends BaseEntity {

    public enum Role { USER, ASSISTANT }

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    private UUID conversationId;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Column(name = "agent_code", length = 10)
    private String agentCode;

    /** JSON cua AgentChatResponse (chi role ASSISTANT) - de mo lai hoi thoai van hien du the Dữ kiện/Phân tích/... */
    @Column(name = "response_json", length = 4000)
    private String responseJson;

    @Column(name = "page_label", length = 300)
    private String pageLabel;
}
