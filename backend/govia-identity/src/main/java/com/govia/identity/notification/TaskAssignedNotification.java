package com.govia.identity.notification;

/**
 * 1 su kien "co task phe duyet/xu ly moi duoc giao cho ai do" - phat sinh boi
 * {@code com.govia.identity.workflow.delegate.WorkflowTaskCreatedNotifier} (Flowable TaskListener
 * event="create") gan vao BAT KY userTask nao cua BAT KY quy trinh BPMN nao, khong rieng 1
 * workflow cu the. {@link WorkflowNotificationService} chi lo phan "gui di dau/nhu the nao".
 */
public record TaskAssignedNotification(
        String taskId,
        String taskName,
        String assigneeUserId,
        String processDefinitionKey,
        String businessKey,
        String tenantId
) {
}
