package com.govia.identity.workflow.dto;

import java.time.Instant;

public record TaskSummary(
        String id,
        String name,
        String processInstanceId,
        String processDefinitionId,
        String businessKey,
        String assignee,
        String owner,
        /** null (chua uy quyen lan nao), "PENDING" (dang uy quyen, cho nguoi duoc uy quyen xu ly),
         * "RESOLVED" (nguoi duoc uy quyen da xong, dang cho owner duyet lai). */
        String delegationState,
        String parentTaskId,
        Instant createTime,
        Instant dueDate
) {
}
