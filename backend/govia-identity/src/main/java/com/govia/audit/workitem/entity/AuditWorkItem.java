package com.govia.audit.workitem.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Cong viec kiem toan" (sheet ZTC_CV, bang ZTB_CV) - trong nhom "Danh muc" cua
 * "Lap ke hoach", module Kiem toan noi bo. */
@Getter
@Setter
@Entity
@Table(name = "audit_work_item")
public class AuditWorkItem extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    private AuditWorkPhase phase;

    /** "Mang nghiep vu" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT (sheet ZTC_Mang_NV). */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /** "Ma chi tiet" - ma dung de lien ket toi cac man hinh chon mau (vd CmTd1, CmNtd1...). */
    @Column(name = "detail_code", length = 20)
    private String detailCode;

    @Column(name = "name", nullable = false, length = 1000)
    private String name;

    @Column(name = "applicable_year")
    private Integer applicableYear;

    @Column(name = "work_set_code", length = 20)
    private String workSetCode;

    @Column(name = "work_type", length = 20)
    private String workType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "has_sample_selection", nullable = false)
    private boolean hasSampleSelection = false;
}
