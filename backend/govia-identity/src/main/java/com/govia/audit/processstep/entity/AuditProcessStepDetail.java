package com.govia.audit.processstep.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP) - chi tiet cua 1 buoc quy trinh tong hop
 * (AuditProcessStepSummary). Ma cua chinh dong nay (vd "LN1400_01") duoc ZTC_TTSS_MAP tham chieu
 * lai qua "ma BQT_CT". */
@Getter
@Setter
@Entity
@Table(name = "audit_process_step_detail")
public class AuditProcessStepDetail extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
