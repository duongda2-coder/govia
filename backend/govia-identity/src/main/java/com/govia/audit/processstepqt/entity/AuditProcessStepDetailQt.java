package com.govia.audit.processstepqt.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP_QT, bang ZTB_BQT_CT_QT) - phien ban
 * "Kiem toan quy trinh" cua AuditProcessStepDetail (ZTC_BQT_MAP). Da bo cot control_point_id
 * (xem migration 142, giong migration 095 cua ban goc): nguoi dung xac nhan "Ma BQT chi tiet"
 * chinh la ma CKS nen cot lien ket rieng la thua. */
@Getter
@Setter
@Entity
@Table(name = "audit_process_step_detail_qt")
public class AuditProcessStepDetailQt extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "applicable_year")
    private Integer applicableYear;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
