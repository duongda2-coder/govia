package com.govia.identity.dto;

import java.util.List;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String username,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String status,
        List<String> roleCodes
) {
}
