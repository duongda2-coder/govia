package com.govia.identity.dto;

import java.util.UUID;

public record OrganizationUnitResponse(
        UUID id,
        String code,
        String name,
        String type,
        String levelCode,
        UUID parentId,
        UUID managerEmployeeId,
        String managerEmployeeName,
        boolean active
) {
}
