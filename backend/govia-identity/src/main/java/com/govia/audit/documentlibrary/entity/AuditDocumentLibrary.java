package com.govia.audit.documentlibrary.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Danh muc "Thu vien tai lieu" (sheet ZTC_TVTL, bang ZTB_TVTL) - quan ly van ban/tai lieu noi bo. */
@Getter
@Setter
@Entity
@Table(name = "audit_document_library")
public class AuditDocumentLibrary extends BaseEntity {

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "document_name", nullable = false, length = 500)
    private String documentName;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** "Nguoi ban hanh" - link toi Position (chuc vu), xem PositionRepository. */
    @Column(name = "issuer_position_id", columnDefinition = "uuid")
    private UUID issuerPositionId;

    @Column(name = "business_activity", length = 500)
    private String businessActivity;

    @Column(name = "topic", length = 500)
    private String topic;

    @Column(name = "replaced_document", length = 1000)
    private String replacedDocument;

    @Column(name = "amended_document", length = 1000)
    private String amendedDocument;

    @Column(name = "legal_basis", length = 1000)
    private String legalBasis;

    @Column(name = "expired", nullable = false)
    private boolean expired = false;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "content", length = 4000)
    private String content;
}
