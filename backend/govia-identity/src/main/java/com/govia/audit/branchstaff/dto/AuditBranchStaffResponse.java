package com.govia.audit.branchstaff.dto;

import java.util.UUID;

public record AuditBranchStaffResponse(
        UUID id,
        String branchCode,
        String staffName,
        String position,
        Integer priority,
        String note,
        boolean active
) {
}
