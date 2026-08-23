package com.govia.identity.workflow.dto;

import java.time.Instant;

public record ProcessInstanceSummary(
        String id,
        String processDefinitionKey,
        String businessKey,
        Instant startTime,
        Instant endTime
) {
}
