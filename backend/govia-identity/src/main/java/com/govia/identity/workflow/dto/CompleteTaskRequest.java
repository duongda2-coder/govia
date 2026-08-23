package com.govia.identity.workflow.dto;

import java.util.Map;

public record CompleteTaskRequest(Map<String, Object> variables) {
}
