package com.govia.audit.agent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** 1 ket qua cua tool search_documents - 1 van ban trong Thu vien tai lieu kem doan trich noi dung.
 * matchMode: "semantic" (co embedding) hoac "keyword" (lui ve so khop tu khoa). */
public record KnowledgeHit(
        UUID documentId,
        String documentNumber,
        String documentName,
        String topic,
        String businessActivity,
        LocalDate issueDate,
        LocalDate effectiveDate,
        boolean expired,
        LocalDate expiryDate,
        String legalBasis,
        String excerpt,
        BigDecimal score,
        String matchMode,
        /** Doan trich trong noi dung FILE dinh kem khop cau hoi nhat (chi khi bat doc file), null neu khong co. */
        String fileExcerpt
) {
}
