package com.govia.audit.documentlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AuditDocumentLibraryRequest(
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Size(max = 500) String documentName,
        LocalDate issueDate,
        LocalDate effectiveDate,
        UUID issuerPositionId,
        String businessActivity,
        String topic,
        String replacedDocument,
        String amendedDocument,
        String legalBasis,
        boolean expired,
        LocalDate expiryDate,
        String content
) {
}
