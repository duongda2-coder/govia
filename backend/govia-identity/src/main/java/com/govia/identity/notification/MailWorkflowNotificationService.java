package com.govia.identity.notification;

import com.govia.identity.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cai dat GUI MAIL THAT cua {@link WorkflowNotificationService} - chi duoc dung khi
 * govia.notification.email.enabled=true VA da dien spring.mail.host/username/password thuc su
 * trong application.yml (bien moi truong GOVIA_SMTP_*). Cho toi khi do, bean nay KHONG duoc tao
 * (xem @ConditionalOnProperty) - {@link LoggingWorkflowNotificationService} dam nhiem thay.
 */
@Service
@ConditionalOnProperty(name = "govia.notification.email.enabled", havingValue = "true")
public class MailWorkflowNotificationService implements WorkflowNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailWorkflowNotificationService.class);

    private final JavaMailSender mailSender;
    private final UserAccountRepository userAccountRepository;
    private final String fromAddress;

    public MailWorkflowNotificationService(JavaMailSender mailSender, UserAccountRepository userAccountRepository,
                                            @Value("${govia.notification.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.userAccountRepository = userAccountRepository;
        this.fromAddress = fromAddress;
    }

    @Override
    public void notifyTaskAssigned(TaskAssignedNotification notification) {
        String recipientEmail = userAccountRepository.findById(UUID.fromString(notification.assigneeUserId()))
                .map(account -> account.getEmail())
                .orElse(null);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Khong gui duoc mail cho userId={} (task={}): tai khoan chua co email", notification.assigneeUserId(), notification.taskId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("[GOVIA] Có công việc chờ xử lý: " + notification.taskName());
        message.setText("Bạn được giao xử lý task \"" + notification.taskName() + "\" (mã tham chiếu: "
                + notification.businessKey() + ") trong quy trình " + notification.processDefinitionKey()
                + ". Vui lòng vào GOVIA > Việc của tôi để xử lý.");
        mailSender.send(message);
    }
}
