package com.govia.audit.phbc.report;

import com.govia.audit.phbc.common.ReportRecommendationCode;
import com.govia.audit.phbc.common.ReportRecommendationTarget;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** "2. Nút chọn mã kiến nghị": mỗi dòng = 1 kiến nghị NSD thêm vào 1 báo cáo phát hành (AuditReportIssuance). */
@Getter
@Setter
@Entity
@Table(name = "audit_report_issuance_recommendation")
public class AuditReportIssuanceRecommendation extends BaseEntity {

    @Column(name = "report_issuance_id", nullable = false, columnDefinition = "uuid")
    private UUID reportIssuanceId;

    /** "Mã kiến nghị" - chọn 1 trong 4 (M01-M04); "Tên KN" suy ra trực tiếp từ mã, không lưu riêng. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_code", nullable = false, length = 5)
    private ReportRecommendationCode recommendationCode;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    /** "Đối tượng kiến nghị" - chọn 1 trong 3: HĐTV, TGĐ, ALL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 10)
    private ReportRecommendationTarget target;

    /** "Đơn vị thực hiện" - lấy từ danh mục Đối tượng kiểm toán, cột loại đối tượng là 'HO'. */
    @Column(name = "executing_unit_id", columnDefinition = "uuid")
    private UUID executingUnitId;

    /** "Tên đơn vị thực hiện" - NSD nhập tự do (tách biệt với tên trong danh mục Đơn vị thực hiện). */
    @Column(name = "executing_unit_name", length = 100)
    private String executingUnitName;

    @Column(name = "deadline")
    private LocalDate deadline;
}
