package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Co cau to chuc (phong ban/khoi/chi nhanh), dung chung cho GOVIA People
 * va lam scope cho ABAC (vd: quyen chi ap dung trong pham vi 1 org unit + cac unit con).
 * parentId de dang de query cay to chuc ma khong can JPA self-join.
 */
@Getter
@Setter
@Entity
@Table(name = "organization_unit")
public class OrganizationUnit extends BaseEntity {

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type", length = 50)
    private String type;

    /** Cap bac don vi: 001 = Khoi, 002 = Trung tam, 003 = Phong ban, 004 = Bo phan. */
    @Column(name = "level_code", length = 10)
    private String levelCode;

    @Column(name = "manager_employee_id", columnDefinition = "uuid")
    private UUID managerEmployeeId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
