package com.govia.audit.documentlibrary.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditDocumentLibraryResponse(
        UUID id,
        String documentNumber,
        String documentName,
        LocalDate issueDate,
        LocalDate effectiveDate,
        UUID issuerPositionId,
        String issuerPositionName,
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
