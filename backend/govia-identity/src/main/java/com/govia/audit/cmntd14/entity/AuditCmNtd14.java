package com.govia.audit.cmntd14.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Man hinh "Danh sách chọn mẫu User Ipcas chấm công (08C)" (sheet ZTC_CM_NTD14, phu luc 08C/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh, ma can bo, ly do chon mau) chua co danh muc tuong ung trong
 * GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode.
 * note: spec ghi "Char 250" nhung o Vi du trong sheet la 1 doan van dai (giai trinh) - noi rong thanh
 * VARCHAR(1000) de khong cat mat du lieu thuc te, xem huong dan man hinh. engagementId/assignedEmployeeId/
 * processStepSummaryId them sau de loc theo Cuoc kiem toan (giong bo loc Nam ben Cham diem rui ro) +
 * theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd14")
public class AuditCmNtd14 extends BaseEntity {

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
