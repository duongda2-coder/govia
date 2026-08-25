package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Danh muc doi tuong kiem toan Quy trinh (sheet ZTC_DTKT4, bang ZTB_DTKT4). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_process")
public class AuditObjectProcess extends BaseEntity {

    /** "Mang nghiep vu" - lien ket toi ztc_mang_nv, tai lieu khong cap catalog nay nen luu ma tu do. */
    @Column(name = "segment_code", length = 20)
    private String segmentCode;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "reference_document", length = 2000)
    private String referenceDocument;

    @Column(name = "audit_result", length = 2000)
    private String auditResult;

    @Column(name = "event_note", length = 2000)
    private String eventNote;

    @Column(name = "incident_note", length = 2000)
    private String incidentNote;

    @Column(name = "review_result", length = 1000)
    private String reviewResult;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
