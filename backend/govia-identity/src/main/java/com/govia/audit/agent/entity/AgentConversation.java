package com.govia.audit.agent.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 1 cuoc tro chuyen cua 1 nguoi dung voi AI Agent. Bang rieng cua module agent (tien to agent_),
 * khong lien ket khoa ngoai toi bang nghiep vu nao - xoa/tat AI khong anh huong du lieu nghiep vu.
 * conversationKey = conversationId do frontend sinh (cung la conversation_id trong agent_message va
 * agent_tool_call_log); id cua BaseEntity van do Hibernate tu sinh nhu moi entity khac.
 */
@Getter
@Setter
@Entity
@Table(name = "agent_conversation")
public class AgentConversation extends BaseEntity {

    @Column(name = "conversation_key", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID conversationKey;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "last_agent_code", length = 10)
    private String lastAgentCode;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "archived", nullable = false)
    private boolean archived;
}
