package com.govia.identity.notification;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * "Hop thu di" cho thong bao workflow: moi lan {@link LoggingWorkflowNotificationService} xu ly 1
 * {@link TaskAssignedNotification} (khi chua bat gui mail that - govia.notification.email.enabled=
 * false) deu ghi lai 1 dong o day thay vi lam mat thong bao - khi bat SMTP that len sau nay co the
 * doc lai bang nay de gui bu (khong bat buoc trong pham vi dot nay, chi de KHONG MAT du lieu).
 */
@Getter
@Setter
@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox extends BaseEntity {

    @Column(name = "recipient_user_id", length = 100)
    private String recipientUserId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "sent", nullable = false)
    private boolean sent = false;

    @Column(name = "sent_at")
    private Instant sentAt;
}
