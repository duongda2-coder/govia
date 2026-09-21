package com.govia.audit.tdkp.report;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Báo cáo TDKP đã xuất, upload lại để lưu trữ (ZTC_TDKP_BC: "Cho phép upload lại Báo cáo đã xuất nhằm mục đích lưu trữ"). File nằm ở bảng
 * attachment dùng chung (entityName = {@link #ATTACHMENT_ENTITY}, entityId = id dòng này). */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_report_archive")
public class AuditTdkpReportArchive extends BaseEntity {

    public static final String ATTACHMENT_ENTITY = "AUDIT_TDKP_REPORT";

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 10)
    private AuditTdkpReportType reportType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Column(name = "note", length = 500)
    private String note;
}
