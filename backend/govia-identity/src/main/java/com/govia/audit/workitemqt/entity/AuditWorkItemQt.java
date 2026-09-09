package com.govia.audit.workitemqt.entity;

import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Bang ma cong viec quy trinh" (sheet ZTC_CV_QT, bang ztb_cv_qt) - phien ban "Kiem
 * toan quy trinh" cua danh muc Cong viec kiem toan (AuditWorkItem/ZTC_CV), tach bang rieng de
 * khong lan voi cong viec kiem toan thong thuong. Cung cau truc voi AuditWorkItem. */
@Getter
@Setter
@Entity
@Table(name = "audit_work_item_qt")
public class AuditWorkItemQt extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    private AuditWorkPhase phase;

    /** "Mang nghiep vu" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT (sheet ZTC_Mang_NV). */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /** "Ma chi tiet" - cot thu 2 trung ten "Ma bo cong viec" trong sheet nguon, mo hinh giong
     * AuditWorkItem.detailCode (khop voi cot "Ma chi..." trong anh chup man hinh minh hoa). */
    @Column(name = "detail_code", length = 20)
    private String detailCode;

    @Column(name = "name", nullable = false, length = 1000)
    private String name;

    @Column(name = "applicable_year")
    private Integer applicableYear;

    @Column(name = "work_set_code", length = 50)
    private String workSetCode;

    @Column(name = "work_type", length = 20)
    private String workType;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
