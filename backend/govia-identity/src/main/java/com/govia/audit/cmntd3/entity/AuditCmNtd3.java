package com.govia.audit.cmntd3.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Man hinh "Danh sach chon mau khach hang to chuc tien gui HDV" (sheet ZTC_CM_NTD3, phu luc
 * 03C/BKS-KTNB) - trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo.
 * Cac cot kieu "List" tham chieu du lieu IPCAS (ma chi nhanh/ma KH...) chua co danh muc tuong ung
 * trong GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi AuditCmTd1.
 * engagementId/assignedEmployeeId/processStepSummaryId them sau de loc theo Cuoc kiem toan (giong
 * bo loc Nam ben Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd3")
public class AuditCmNtd3 extends BaseEntity {

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

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_address", length = 200)
    private String customerAddress;

    /** "Ma KH cua Corebank" - ma khach hang theo he thong Core Banking, khac voi customerCode noi bo. */
    @Column(name = "corebank_customer_code", length = 50)
    private String corebankCustomerCode;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "original_currency_balance", precision = 20, scale = 2)
    private BigDecimal originalCurrencyBalance;

    @Column(name = "converted_balance", precision = 20, scale = 2)
    private BigDecimal convertedBalance;

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
