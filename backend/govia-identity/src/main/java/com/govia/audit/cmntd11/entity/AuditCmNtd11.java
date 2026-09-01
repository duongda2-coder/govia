package com.govia.audit.cmntd11.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Giao dich chuyen tien voi Nguoi nuoc ngoai va Kinh doanh ngoai te" (sheet ZTC_CM_NTD11,
 * phu luc 06C/BKS-KTNB) - trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi
 * bo. Cac cot kieu "List" tham chieu du lieu IPCAS (ma chi nhanh/ma KH/can bo) chua co danh muc
 * tuong ung trong GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi
 * AuditBranchStaff.branchCode. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd11")
public class AuditCmNtd11 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "reference_number", nullable = false, precision = 20, scale = 2)
    private BigDecimal referenceNumber;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "sample_reason", length = 50)
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
