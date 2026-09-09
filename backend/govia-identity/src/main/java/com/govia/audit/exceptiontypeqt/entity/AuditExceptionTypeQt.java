package com.govia.audit.exceptiontypeqt.entity;

import com.govia.audit.exceptiontype.entity.AuditExceptionCategory;
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

/** Danh muc "Ton tai sai sot quy trinh" (sheet ZTC_TTSS_QT, bang ztb_TTSS_Qt) - phien ban "Kiem
 * toan quy trinh" cua danh muc Loai ton tai sai sot (AuditExceptionType/ZTC_TTSS), tach bang
 * rieng. Cot dau tien cua sheet nguon ghi header "Ma Buoc quy trinh" nhung Logic/Vi du deu la ma
 * Mang nghiep vu (vd "LN") - giong het tinh huong da gap o ZTC_TTSS goc, nen mo hinh theo du lieu
 * thuc te (xem AuditExceptionType). */
@Getter
@Setter
@Entity
@Table(name = "audit_exception_type_qt")
public class AuditExceptionTypeQt extends BaseEntity {

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
