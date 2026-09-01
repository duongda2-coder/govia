package com.govia.audit.cmntd9.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AuditCmNtd9Request(
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 20) String postingUser,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 20) String idNumber,
        @Size(max = 20) String customerType,
        @Size(max = 120) String transactionContent,
        @Size(max = 50) String sampleReason,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
