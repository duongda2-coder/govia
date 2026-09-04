package com.govia.audit.cmtd2.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Man hinh "Ket qua kiem toan chon mau cac but toan giao dich huy, lui ngay" (sheet ZTC_CM_TD2, phu
 * luc 02G/BKS-KTNB) - trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo.
 * Cac cot kieu "List" tham chieu du lieu IPCAS (ma chi nhanh/ma KH/can bo...) chua co danh muc tuong
 * ung trong GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi AuditCmTd1. postingDateDiff
 * la cot tinh toan (valueDate - transactionDate, tinh bang ngay), khong nhan input truc tiep tu nguoi dung.
 * engagementId/assignedEmployeeId/processStepSummaryId them sau de loc theo Cuoc kiem toan (giong bo
 * loc Nam ben Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_td2")
public class AuditCmTd2 extends BaseEntity {

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

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "posting_user", nullable = false, length = 20)
    private String postingUser;

    /** Spec ghi Type = Number nhung "So but toan" la ma dinh danh but toan - luu BigDecimal theo dung
     * Type de nhat quan voi cac cot Number khac, khong dung lam phep tinh. */
    @Column(name = "entry_number", precision = 20, scale = 2)
    private BigDecimal entryNumber;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "disbursement_number", length = 20)
    private String disbursementNumber;

    @Column(name = "business_code", length = 20)
    private String businessCode;

    @Column(name = "transaction_status", length = 50)
    private String transactionStatus;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "debit_amount", precision = 20, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 20, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    /** Chenh lech ngay hach toan (D-C) = valueDate - transactionDate, tinh theo ngay - he thong tu
     * tinh khi create/update, xem AuditCmTd2Service. */
    @Column(name = "posting_date_diff", precision = 20, scale = 2)
    private BigDecimal postingDateDiff;

    @Column(name = "ipcas_review_result", length = 120)
    private String ipcasReviewResult;

    @Column(name = "document_check_result", length = 120)
    private String documentCheckResult;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
