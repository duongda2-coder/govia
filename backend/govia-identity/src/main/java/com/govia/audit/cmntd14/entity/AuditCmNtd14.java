package com.govia.audit.cmntd14.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Danh sách chọn mẫu User Ipcas chấm công (08C)" (sheet ZTC_CM_NTD14, phu luc 08C/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh, ma can bo, ly do chon mau) chua co danh muc tuong ung trong
 * GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode.
 * note: spec ghi "Char 250" nhung o Vi du trong sheet la 1 doan van dai (giai trinh) - noi rong thanh
 * VARCHAR(1000) de khong cat mat du lieu thuc te, xem huong dan man hinh. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd14")
public class AuditCmNtd14 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "staff_code", nullable = false, length = 20)
    private String staffCode;

    @Column(name = "staff_name", length = 100)
    private String staffName;

    @Column(name = "attendance_code", length = 1)
    private String attendanceCode;

    @Column(name = "description", length = 120)
    private String description;

    @Column(name = "matched_transaction_count", precision = 20, scale = 2)
    private BigDecimal matchedTransactionCount;

    @Column(name = "unmatched_transaction_count", precision = 20, scale = 2)
    private BigDecimal unmatchedTransactionCount;

    @Column(name = "adjusted_transaction_count", precision = 20, scale = 2)
    private BigDecimal adjustedTransactionCount;

    @Column(name = "user_code", length = 15)
    private String userCode;

    /** Spec: Char 250, nhung Vi du la doan van dai hon 250 ky tu -> noi rong, xem javadoc class. */
    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "sample_code", length = 20)
    private String sampleCode;

    @Column(name = "sample_reason", length = 50)
    private String sampleReason;

    @Column(name = "audit_result", length = 200)
    private String auditResult;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
