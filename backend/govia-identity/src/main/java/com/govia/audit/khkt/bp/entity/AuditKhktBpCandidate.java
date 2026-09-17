package com.govia.audit.khkt.bp.entity;

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

/** "De xuat DTKT nam theo phong" - Phan 1, ban nhap (sheet ZTC_KHKT_BP, bang ZTB_KHKT_BP). Duoc
 * phong nghiep vu tu chon doi tuong (tu RiskBranchScoreExpertRank hoac RiskAssessmentOtherExpertRank),
 * chinh sua, roi bam "Xac nhan danh sach" de sao chep sang AuditKhktBpConfirmed (Phan 2). */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_bp_candidate")
public class AuditKhktBpCandidate extends BaseEntity {

    /** "Phong NV" - link toi AuditMasterDataItem danh muc DEPARTMENT. */
    @Column(name = "department_id", nullable = false, columnDefinition = "uuid")
    private UUID departmentId;

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    /** "Loai DTKT" - CN (tu RiskBranchScoreExpertRank) hay khac (tu RiskAssessmentOtherExpertRank). */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private AuditKhktSourceType sourceType;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "audit_object_name", nullable = false, length = 500)
    private String auditObjectName;

    /** Chi co gia tri khi sourceType = OTHER - "ma loai doi tuong kiem toan" cua ztc_xhrr_khac_cg. */
    @Column(name = "audit_object_category_code", length = 20)
    private String auditObjectCategoryCode;

    /** Snapshot tai thoi diem them vao - KHONG link dong toi bang cham diem goc, tranh so lieu ke
     * hoach bi thay doi ngoai y muon khi diem rui ro goc duoc cap nhat lai sau nay. */
    @Column(name = "risk_score", precision = 12, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "rank_label", length = 50)
    private String rankLabel;

    /** Resolve tu AuditObjectUnit theo ma (neu tim thay) - dung de lay Du no/Nguon von va tra cuu
     * "Lich su KT" (AuditObjectInspectionHistory) khi hien thi cac cot T-5..T-1. */
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

    /** "Can cu de xuat" - khac voi reviewResult ("Ket qua ra soat"), day la ly do dua doi tuong
     * vao de xuat KHKT nam nay. */
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
}
