package com.govia.audit.planengagement.supervisionteam.dto;

import java.util.UUID;

public record AuditSupervisionTeamMemberResponse(
        UUID id,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String username
) {
}
