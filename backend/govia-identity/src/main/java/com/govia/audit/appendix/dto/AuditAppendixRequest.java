package com.govia.audit.appendix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditAppendixRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 100) String sampleType,
        @NotBlank @Size(max = 100) String appendixCode,
        @Size(max = 100) String note,
        boolean active
) {
}
