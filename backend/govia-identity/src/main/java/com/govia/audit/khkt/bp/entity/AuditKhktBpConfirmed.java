package com.govia.audit.khkt.bp.entity;

import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** "De xuat DTKT nam theo phong" - Phan 2, da xac nhan (sheet ZTC_KHKT_BP2, bang ZTB_KHKT_Bophan2).
 * Sao chep nguyen trang tu AuditKhktBpCandidate khi bam "Xac nhan danh sach" - CHI DOC, sua lai
 * phai thuc hien o Phan 1 roi xac nhan lai (se GHI DE toan bo Phan 2 cua phong+nam do). */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_bp_confirmed")
public class AuditKhktBpConfirmed extends BaseEntity {

    @Column(name = "department_id", nullable = false, columnDefinition = "uuid")
    private UUID departmentId;

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private AuditKhktSourceType sourceType;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "audit_object_name", nullable = false, length = 255)
    private String auditObjectName;

    @Column(name = "audit_object_category_code", length = 20)
    private String auditObjectCategoryCode;

    @Column(name = "risk_score", precision = 12, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "rank_label", length = 50)
    private String rankLabel;

    @Column(name = "audit_object_unit_id", columnDefinition = "uuid")
    private UUID auditObjectUnitId;

    @Column(name = "on_balance_sheet_loan", precision = 20, scale = 2)
    private BigDecimal onBalanceSheetLoan;

    @Column(name = "funding_source", precision = 20, scale = 2)
    private BigDecimal fundingSource;

    @Column(name = "review_result", length = 250)
    private String reviewResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_decision", length = 20)
    private AuditKhktSelectionDecision selectionDecision;

    @Column(name = "proposal_basis", length = 500)
    private String proposalBasis;

    @Enumerated(EnumType.STRING)
    @Column(name = "approved_selection", length = 10)
    private AuditKhktSelectionChoice approvedSelection;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_selection", length = 10)
    private AuditKhktSelectionChoice expectedSelection;

    @Column(name = "audit_scope", length = 100)
    private String auditScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "adhoc_audit_or_supervision", length = 10)
    private AuditKhktSelectionChoice adhocAuditOrSupervision;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_adjustment", length = 10)
    private AuditKhktSelectionChoice planAdjustment;

    @Column(name = "adjustment_reason", length = 255)
    private String adjustmentReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "khktgs_after_adjustment", length = 10)
    private AuditKhktSelectionChoice khktgsAfterAdjustment;

    /** "Trang thai" - chi co o Phan 2 (BP2). Reset ve PENDING moi lan "Xac nhan danh sach" ghi de
     * (xoa+tao lai dong), chuyen APPROVED qua nut phe duyet rieng o Phan 2. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private AuditKhktApprovalStatus approvalStatus = AuditKhktApprovalStatus.PENDING;
}
