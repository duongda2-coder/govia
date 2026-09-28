package com.govia.audit.dgcl;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 1 phieu cham diem (PL01A/PL01B/PL01F) cua 1 doi tuong trong 1 CKT. Doi tuong = 1 thanh vien doan
 * (subjectKey = employeeId) hoac ca cuoc kiem toan (subjectKey = {@link AuditDgclService#TEAM_SUBJECT}). */
@Getter
@Setter
@Entity
@Table(name = "audit_dgcl_evaluation")
public class AuditDgclEvaluation extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    @Column(name = "subject_key", nullable = false, length = 40)
    private String subjectKey;

    @Column(name = "employee_id", columnDefinition = "uuid")
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "appendix", nullable = false, length = 10)
    private DgclAppendix appendix;

    /** PL01A/PL01B: ty le tuan thu x 100; PL01F: tong diem dat duoc (dong VI). Tinh lai moi lan luu. */
    @Column(name = "score", precision = 9, scale = 4)
    private BigDecimal score;

    /** Chi PL01F: diem cong (dong IV) / diem tru (dong V). */
    @Column(name = "bonus_points", precision = 9, scale = 4)
    private BigDecimal bonusPoints;

    @Column(name = "penalty_points", precision = 9, scale = 4)
    private BigDecimal penaltyPoints;

    /** Chi PL01F: xep loai tinh tu diem. */
    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "evaluator_name")
    private String evaluatorName;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "confirmed_by")
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "controlled", nullable = false)
    private boolean controlled;

    @Column(name = "controlled_by")
    private String controlledBy;

    @Column(name = "controlled_at")
    private Instant controlledAt;
}
