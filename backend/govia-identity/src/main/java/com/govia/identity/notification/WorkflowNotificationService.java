package com.govia.identity.notification;

/**
 * Cong dung chung de "bao cho ai do biet co task workflow moi" - moi quy trinh BPMN nao gan
 * {@code workflowTaskCreatedNotifier} (TaskListener event="create") vao userTask deu di qua day,
 * khong can biet kenh gui thuc su la gi (log, email, sau nay co the them SMS/Teams...).
 * Xem 2 cai dat: {@link LoggingWorkflowNotificationService} (mac dinh, chua gui mail that) va
 * {@link MailWorkflowNotificationService} (bat bang notification.email.enabled=true khi da co
 * SMTP that).
 */
public interface WorkflowNotificationService {

    void notifyTaskAssigned(TaskAssignedNotification notification);
}
