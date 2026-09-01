package com.govia.audit.branchstaff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuditBranchStaffRequest(
        @NotBlank @Size(max = 10) String branchCode,
        @NotBlank @Size(max = 100) String staffName,
        @Size(max = 100) String position,
        @Min(1) @Max(5) Integer priority,
        @Size(max = 500) String note,
        boolean active
) {
}
