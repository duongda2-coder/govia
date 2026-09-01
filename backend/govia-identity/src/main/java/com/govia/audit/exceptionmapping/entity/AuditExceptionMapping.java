package com.govia.audit.exceptionmapping.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Mapping ton tai sai sot" (sheet ZTC_TTSS_MAP) - gan 1 loai TTSS (AuditExceptionType)
 * vao 1 buoc quy trinh chi tiet (AuditProcessStepDetail) cu the, trong nhom "Danh muc" cua "Lap ke
 * hoach", module Kiem toan noi bo. */
@Getter
@Setter
@Entity
@Table(name = "audit_exception_mapping")
public class AuditExceptionMapping extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "process_step_detail_id", nullable = false, columnDefinition = "uuid")
    private UUID processStepDetailId;

    @Column(name = "exception_type_id", nullable = false, columnDefinition = "uuid")
    private UUID exceptionTypeId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
