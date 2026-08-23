package com.govia.identity.workflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record StartProcessRequest(
        @NotBlank String processDefinitionKey,
        String businessKey,
        Map<String, Object> variables
) {
}
