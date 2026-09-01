package com.govia.audit.processstep.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Buoc quy trinh tong hop" (sheet ZTB_BQT_TH) - trong nhom "Danh muc" cua "Lap ke
 * hoach", module Kiem toan noi bo. Duoc AuditProcessStepDetail (ZTC_BQT_MAP) tham chieu toi. */
@Getter
@Setter
@Entity
@Table(name = "audit_process_step_summary")
public class AuditProcessStepSummary extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "work_item_id", columnDefinition = "uuid")
    private UUID workItemId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
