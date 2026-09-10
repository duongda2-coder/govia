package com.govia.audit.exceptionmappingqt.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Mapping ton tai sai sot quy trinh" (sheet ZTC_TTSS_MAP_QT, bang ztb_TTSS_MAP_QT) -
 * phien ban "Kiem toan quy trinh" cua AuditExceptionMapping (ZTC_TTSS_MAP), tach bang rieng. Gan
 * 1 loai TTSS quy trinh (AuditExceptionTypeQt) vao 1 buoc quy trinh chi tiet
 * (AuditProcessStepDetailQt). */
@Getter
@Setter
@Entity
@Table(name = "audit_exception_mapping_qt")
public class AuditExceptionMappingQt extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "process_step_detail_id", nullable = false, columnDefinition = "uuid")
    private UUID processStepDetailId;

    @Column(name = "exception_type_id", nullable = false, columnDefinition = "uuid")
    private UUID exceptionTypeId;

    @Column(name = "applicable_year")
    private Integer applicableYear;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
