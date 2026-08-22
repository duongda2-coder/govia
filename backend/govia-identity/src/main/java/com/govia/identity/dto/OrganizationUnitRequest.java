package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OrganizationUnitRequest(
        @NotBlank String code,
        @NotBlank String name,
        String type,
        String levelCode,
        UUID parentId,
        UUID managerEmployeeId
) {
}
