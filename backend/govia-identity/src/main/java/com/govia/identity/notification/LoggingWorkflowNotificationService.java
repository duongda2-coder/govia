package com.govia.identity.notification;

import com.govia.identity.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cai dat MAC DINH cua {@link WorkflowNotificationService}: CHUA gui mail that (chua co SMTP nao
 * duoc cau hinh - xem ghi chu tai application.yml govia.notification.email), chi log + luu lai 1
 * dong trong bang notification_outbox de KHONG MAT thong bao - san sang gui bu khi bat
 * govia.notification.email.enabled=true (luc do {@link MailWorkflowNotificationService} duoc dung
 * thay the, khong can sua code goi).
 */
@Service
@ConditionalOnProperty(name = "govia.notification.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingWorkflowNotificationService implements WorkflowNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingWorkflowNotificationService.class);

    private final UserAccountRepository userAccountRepository;
    private final NotificationOutboxRepository outboxRepository;

    public LoggingWorkflowNotificationService(UserAccountRepository userAccountRepository, NotificationOutboxRepository outboxRepository) {
        this.userAccountRepository = userAccountRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void notifyTaskAssigned(TaskAssignedNotification notification) {
        String recipientEmail = userAccountRepository.findById(UUID.fromString(notification.assigneeUserId()))
                .map(account -> account.getEmail())
                .orElse(null);

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setTenantId(UUID.fromString(notification.tenantId()));
        outbox.setRecipientUserId(notification.assigneeUserId());
        outbox.setRecipientEmail(recipientEmail);
        outbox.setSubject(buildSubject(notification));
        outbox.setBody(buildBody(notification));
        outbox.setSent(false);
        outboxRepository.save(outbox);

        log.info("[notification-outbox] Task '{}' (id={}, process={}, businessKey={}) da duoc giao "
                        + "cho userId={} (email={}) - GUI MAIL THAT dang TAT, xem NotificationOutbox de gui bu sau",
                notification.taskName(), notification.taskId(), notification.processDefinitionKey(),
                notification.businessKey(), notification.assigneeUserId(), recipientEmail);
    }

    private String buildSubject(TaskAssignedNotification notification) {
        return "[GOVIA] Có công việc chờ xử lý: " + notification.taskName();
    }

    private String buildBody(TaskAssignedNotification notification) {
        return "Bạn được giao xử lý task \"" + notification.taskName() + "\" (mã tham chiếu: "
                + notification.businessKey() + ") trong quy trình " + notification.processDefinitionKey()
                + ". Vui lòng vào GOVIA > Việc của tôi để xử lý.";
    }
}
