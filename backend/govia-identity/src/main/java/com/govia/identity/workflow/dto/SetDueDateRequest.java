package com.govia.identity.workflow.dto;

import java.time.Instant;

public record SetDueDateRequest(Instant dueDate) {
}
