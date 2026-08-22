package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Gan role cho user. scopeOrgUnitId (nullable) la nen tang ABAC don gian:
 * neu co gia tri, role chi co hieu luc trong pham vi org unit do (va cac unit con) -
 * vd "Audit Manager" chi duyet finding trong pham vi chi nhanh cua minh.
 * Null = hieu luc toan tenant.
 */
@Getter
@Setter
@Entity
@Table(name = "user_role")
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "role_id", nullable = false, columnDefinition = "uuid")
    private UUID roleId;

    @Column(name = "scope_org_unit_id", columnDefinition = "uuid")
    private UUID scopeOrgUnitId;
}
