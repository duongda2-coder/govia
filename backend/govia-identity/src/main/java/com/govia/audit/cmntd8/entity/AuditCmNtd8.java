package com.govia.audit.cmntd8.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Man hinh "Danh sach chon mau khach hang giao dich so tien lon va khong co TK" (sheet ZTC_CM_NTD8,
 * phu luc 13B/BKS-KTNB) - trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi
 * bo. Cac cot kieu "List" tham chieu du lieu IPCAS (ma chi nhanh, ly do chon mau) chua co danh muc
 * tuong ung trong GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi
 * AuditBranchStaff.branchCode. engagementId/assignedEmployeeId/processStepSummaryId them sau de loc
 * theo Cuoc kiem toan (giong bo loc Nam ben Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd8")
public class AuditCmNtd8 extends BaseEntity {

    /** "Ma cuoc kiem toan" - dong nay thuoc ve dot thuc hien kiem toan nao. */
    @Column(name = "engagement_id", columnDefinition = "uuid")
    private UUID engagementId;

    /** "Nguoi duoc phan cong" - link toi Employee (KHONG qua he thong Nhom/Phan cong cua man hinh
     * Khoi tao doan kiem toan, chi la 1 o chon nhan vien don gian tren chinh dong du lieu nay). */
    @Column(name = "assigned_employee_id", columnDefinition = "uuid")
    private UUID assignedEmployeeId;

    /** "Ma BQT_TH" - link toi AuditProcessStepSummary (danh muc "Buoc quy trinh tong hop", sheet ZTB_BQT_TH). */
    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "posting_user", nullable = false, length = 20)
    private String postingUser;

    @Column(name = "entry_number", nullable = false, precision = 20, scale = 2)
    private BigDecimal entryNumber;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "ordering_party", length = 120)
    private String orderingParty;

    @Column(name = "beneficiary_party", length = 120)
    private String beneficiaryParty;

    @Column(name = "beneficiary_account", length = 20)
    private String beneficiaryAccount;

    @Column(name = "sample_reason", length = 1000)
    private String sampleReason;

    @Column(name = "audit_result", length = 120)
    private String auditResult;

    @Column(name = "recommendation_type", length = 120)
    private String recommendationType;

    @Column(name = "transaction_staff", length = 120)
    private String transactionStaff;

    @Column(name = "control_user", length = 120)
    private String controlUser;

    @Column(name = "control_staff", length = 120)
    private String controlStaff;

    @Column(name = "control_staff_title", length = 120)
    private String controlStaffTitle;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
