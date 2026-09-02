package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Don vi lien quan" - luoi con cua 1 CKT, moi dong link toi 1 AuditObjectUnit khac. */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_related_unit")
public class AuditEngagementRelatedUnit extends BaseEntity {

    @Column(name = "audit_engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID auditEngagementId;

    @Column(name = "audit_object_unit_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectUnitId;
}
