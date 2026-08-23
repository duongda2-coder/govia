package com.govia.identity.workflow.dto;

import java.time.Instant;

/** 1 hoat dong (task/gateway/event...) da chay qua trong lich su cua 1 process instance. */
public record ActivityHistoryEntry(
        String activityId,
        String activityName,
        String activityType,
        String assignee,
        Instant startTime,
        Instant endTime
) {
}
