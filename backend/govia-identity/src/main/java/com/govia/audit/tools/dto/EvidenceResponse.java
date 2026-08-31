package com.govia.audit.tools.dto;

import java.util.UUID;

/**
 * 1 file dinh kem cho 1 Audit Finding - map tu Attachment dung chung (com.govia.core.attachment),
 * bo storagePath (duong dan vat ly tren server) khoi hop dong tra ve cho AI Agent.
 */
public record EvidenceResponse(
        UUID id,
        String fileName,
        String contentType,
        Long sizeBytes,
        String downloadUrl
) {
}
