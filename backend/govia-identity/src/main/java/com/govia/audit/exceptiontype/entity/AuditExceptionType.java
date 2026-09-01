package com.govia.audit.exceptiontype.entity;

import com.govia.audit.masterdata.entity.AuditLevel;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Loai ton tai sai sot" (sheet ZTC_TTSS, bang ZTB_TTSS) - trong nhom "Danh muc" cua
 * "Lap ke hoach", module Kiem toan noi bo. Cot dau tien cua sheet nguon ghi header "Ma Buoc quy
 * trinh" nhung Logic/Vi du deu la ma Mang nghiep vu (vd "LN") - mo hinh theo du lieu thuc te. */
@Getter
@Setter
@Entity
@Table(name = "audit_exception_type")
public class AuditExceptionType extends BaseEntity {

    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private AuditExceptionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", length = 20)
    private AuditLevel impactLevel;

    @Column(name = "classification_basis", length = 255)
    private String classificationBasis;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
