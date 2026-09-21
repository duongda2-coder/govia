package com.govia.audit.tdkp.ceo;

import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpTarget;
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

/** Kiến nghị của KTNB đối với HĐTV/TGĐ (sheet 2 ZTC_TDKP_CEO_ALL do Phòng nghiệp vụ cập nhật, sheet 3 ZTC_TDKP_CEO_KH do Phòng Kế hoạch
 * cập nhật). Cùng 1 bảng, phân biệt bằng scope: dòng KH được chuyển từ dòng ALL (sourceId) rồi sửa độc lập. "Ngày thực hiện chỉnh sửa" /
 * "User chỉnh sửa" lấy từ updatedAt/updatedBy của BaseEntity. */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_ceo_recommendation")
public class AuditTdkpCeoRecommendation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 5)
    private TdkpCeoScope scope;

    /** Dòng ALL nguồn (chỉ có ở dòng KH chuyển từ ALL) - chống chuyển trùng. */
    @Column(name = "source_id", columnDefinition = "uuid")
    private UUID sourceId;

    @Column(name = "report_number", length = 50)
    private String reportNumber;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "recommendation_type_id", columnDefinition = "uuid")
    private UUID recommendationTypeId;

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_object", length = 10)
    private TdkpTarget targetObject;

    /** "Đơn vị thực hiện" - đơn vị/khối trong danh mục đối tượng kiểm toán (ztc_dt_kt1/2). */
    @Column(name = "executing_unit_id", columnDefinition = "uuid")
    private UUID executingUnitId;

    /** "Chỉ đạo của HĐTV, TGĐ". */
    @Column(name = "directive", length = 500)
    private String directive;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TdkpStatus status;

    @Column(name = "evaluation", length = 500)
    private String evaluation;

    @Column(name = "note", length = 500)
    private String note;
}
