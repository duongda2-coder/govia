package com.govia.identity.maildemo;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Lich su 1 lan gui mail tu man hinh "Demo gui email" (ca thanh cong lan that bai). */
@Getter
@Setter
@Entity
@Table(name = "email_demo_log")
public class EmailDemoLog extends BaseEntity {

    @Column(name = "to_address", nullable = false, length = 1000)
    private String toAddress;

    @Column(name = "cc_address", length = 1000)
    private String ccAddress;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body", length = 10000)
    private String body;

    @Column(name = "html", nullable = false)
    private boolean html;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    /** Token ngau nhien gan vao link nut Phe duyet/Tu choi trong mail - null neu mail khong kem nut. */
    @Column(name = "action_token", length = 64, unique = true)
    private String actionToken;

    /** PENDING (da gui, chua bam) / APPROVED / REJECTED - lan bam DAU TIEN duoc ghi nhan. */
    @Column(name = "action_status", length = 20)
    private String actionStatus;

    @Column(name = "action_at")
    private Instant actionAt;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl;
}
