package com.govia.audit.khkt.th.entity;

import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
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

/** "Danh sach DTKT nam cua Phong ke hoach" - Phan 2, da xac nhan (sheet ZTC_KHKT_TH2, bang
 * ZTB_KHKT_TH2) - CHINH LA "TH2" ma sheet ZTC_KHKT_THANG tham chieu toi. Sao chep nguyen trang tu
 * AuditKhktThCandidate khi bam "Xac nhan danh sach" - CHI DOC. */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_th_confirmed")
public class AuditKhktThConfirmed extends BaseEntity {

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private AuditKhktSourceType sourceType;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "audit_object_name", nullable = false, length = 500)
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

    @Column(name = "bp_review_result", length = 250)
    private String bpReviewResult;

    @Column(name = "proposal_basis_th", length = 120)
    private String proposalBasisTh;

    @Column(name = "expert_opinion", length = 250)
    private String expertOpinion;

    @Column(name = "selection1", nullable = false)
    private boolean selection1 = false;

    @Column(name = "selection2", nullable = false)
    private boolean selection2 = false;

    @Column(name = "selection3", nullable = false)
    private boolean selection3 = false;

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

    /** "Trang thai" - chi co o Phan 2 (TH2). Reset ve PENDING moi lan "Xac nhan danh sach" ghi de
     * (xoa+tao lai dong), chuyen APPROVED qua nut phe duyet rieng o Phan 2. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private AuditKhktApprovalStatus approvalStatus = AuditKhktApprovalStatus.PENDING;
}
