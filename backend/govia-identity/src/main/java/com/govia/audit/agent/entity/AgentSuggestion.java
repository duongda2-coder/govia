package com.govia.audit.agent.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 1 dong "Gợi ý AI" (G4, muc M3) cho 1 nguoi dung - do job theo lich (hoac nguoi dung bam "Làm mới") sinh ra tu du
 * lieu ho duoc xem. Bang rieng cua module agent, khong khoa ngoai toi bang nghiep vu; chi la thong tin nhac viec,
 * khong phai task Flowable, khong gui di dau ca.
 */
@Getter
@Setter
@Entity
@Table(name = "agent_suggestion")
public class AgentSuggestion extends BaseEntity {

    public enum Severity { INFO, WARN, HIGH }

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "agent_code", nullable = false, length = 10)
    private String agentCode;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "severity", nullable = false, length = 10)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Severity severity;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "detail", length = 4000)
    private String detail;

    @Column(name = "link_path", length = 300)
    private String linkPath;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "dismissed", nullable = false)
    private boolean dismissed;
}
