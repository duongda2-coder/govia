package com.govia.audit.appendix.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Quan ly phu luc" (sheet ZTC_phuluc, bang ZTB_phuluc) - phu luc/mau bieu dinh kem theo
 * tung loai mau chon cua 1 mang nghiep vu, dung lam tham chieu khi lap ho so kiem toan. */
@Getter
@Setter
@Entity
@Table(name = "audit_appendix")
public class AuditAppendix extends BaseEntity {

    /** "Ma mang nghiep vu" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT (sheet ZTC_Mang_NV). */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "sample_type", nullable = false, length = 100)
    private String sampleType;

    @Column(name = "appendix_code", nullable = false, length = 100)
    private String appendixCode;

    @Column(name = "note", length = 100)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
