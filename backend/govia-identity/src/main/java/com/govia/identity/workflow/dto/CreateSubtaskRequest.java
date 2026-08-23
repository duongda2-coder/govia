package com.govia.identity.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSubtaskRequest(@NotBlank String name) {
}
