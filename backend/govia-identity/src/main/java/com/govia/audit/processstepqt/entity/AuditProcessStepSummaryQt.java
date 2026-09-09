package com.govia.audit.processstepqt.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Buoc quy trinh tong hop" (sheet ZTC_BQT_TH_QT, bang ZTB_BQT_TH_QT) - phien ban "Kiem
 * toan quy trinh" cua AuditProcessStepSummary (ZTB_BQT_TH), tach bang rieng. Link workItemId toi
 * AuditWorkItemQt (ZTC_CV_QT) thay vi AuditWorkItem goc. */
@Getter
@Setter
@Entity
@Table(name = "audit_process_step_summary_qt")
public class AuditProcessStepSummaryQt extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** "Ma cong viec" - link toi AuditWorkItemQt (sheet ZTC_CV_QT). */
    @Column(name = "work_item_id", columnDefinition = "uuid")
    private UUID workItemId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
