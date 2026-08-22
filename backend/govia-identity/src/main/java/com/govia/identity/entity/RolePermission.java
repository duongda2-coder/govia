package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "role_permission")
public class RolePermission extends BaseEntity {

    @Column(name = "role_id", nullable = false, columnDefinition = "uuid")
    private UUID roleId;

    @Column(name = "permission_id", nullable = false, columnDefinition = "uuid")
    private UUID permissionId;
}
