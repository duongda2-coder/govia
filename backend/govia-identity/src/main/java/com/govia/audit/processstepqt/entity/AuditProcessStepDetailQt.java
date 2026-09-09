package com.govia.audit.processstepqt.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP_QT, bang ZTB_BQT_CT_QT) - phien ban
 * "Kiem toan quy trinh" cua AuditProcessStepDetail (ZTC_BQT_MAP). Khac voi ban goc (da bo cot
 * control_point_id - xem migration 095), sheet ZTC_BQT_MAP_QT yeu cau ro "Ma BQT chi tiet" link
 * toi danh muc Chot kiem soat quy trinh (ZTC_CKS_QT), nen entity nay giu lai controlPointId. */
@Getter
@Setter
@Entity
@Table(name = "audit_process_step_detail_qt")
public class AuditProcessStepDetailQt extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

    /** "Ma BQT chi tiet" - link toi AuditControlPointQt (sheet ZTC_CKS_QT). */
    @Column(name = "control_point_id", columnDefinition = "uuid")
    private UUID controlPointId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
