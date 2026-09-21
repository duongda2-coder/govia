package com.govia.audit.tdkp.assignment;

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

/** "Phân công theo dõi khắc phục" (ZTC_TDKP_PC, sheet 1 file 6.TDKP_29.5.2026): phân công cán bộ KTNB theo dõi kiến nghị theo lĩnh vực.
 * scope CEO = kiến nghị đối với HĐTV/TGĐ, BRANCH = kiến nghị đối với Chi nhánh. Các cột dạng list là UUID tham chiếu danh mục
 * (không FK cứng - cùng quy ước các module Kiểm toán khác). */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_assignment")
public class AuditTdkpAssignment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private TdkpAssignmentScope scope;

    /** "Phân loại kiến nghị" - danh mục RECOMMENDATION_TYPE. */
    @Column(name = "recommendation_type_id", columnDefinition = "uuid")
    private UUID recommendationTypeId;

    /** "Mã mảng nghiệp vụ" - danh mục BUSINESS_SEGMENT. */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    /** "Đối tượng được kiến nghị" (HĐTV/TGĐ) - chỉ dùng cho scope CEO. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_object", length = 10)
    private TdkpTarget targetObject;

    /** Đối tượng được kiến nghị dạng đơn vị (Chi nhánh...) - bắt buộc với scope BRANCH; "Mã đối tượng KT" lấy từ đơn vị này. */
    @Column(name = "audit_object_unit_id", columnDefinition = "uuid")
    private UUID auditObjectUnitId;

    /** "Khu vực địa lý" - danh mục GEOGRAPHIC_AREA. */
    @Column(name = "geographic_area_id", columnDefinition = "uuid")
    private UUID geographicAreaId;

    /** "Cán bộ phụ trách" - chọn từ danh sách CBNV KTNB; User phụ trách và Phòng nghiệp vụ tự hiển thị theo cán bộ. */
    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
