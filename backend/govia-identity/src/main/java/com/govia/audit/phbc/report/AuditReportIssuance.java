package com.govia.audit.phbc.report;

import com.govia.audit.phbc.common.ReportIssuingUnit;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** "Màn hình Phát hành báo cáo" (file ztc_phbc): 1 dòng = 1 báo cáo kiểm toán đã phát hành, kèm danh sách kiến nghị (bảng con). */
@Getter
@Setter
@Entity
@Table(name = "audit_report_issuance")
public class AuditReportIssuance extends BaseEntity {

    @Column(name = "report_number", nullable = false, length = 100)
    private String reportNumber;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    /** "Nội dung trích yếu". */
    @Column(name = "summary_content", length = 1000)
    private String summaryContent;

    /** "Đơn vị phát hành" - NSD chọn KTNB hoặc BKS. */
    @Enumerated(EnumType.STRING)
    @Column(name = "issuing_unit", length = 10)
    private ReportIssuingUnit issuingUnit;
}
