package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Chuc danh (master-data dung chung, giong OrganizationUnit) - Employee tham chieu qua positionId
 * thay vi luu chuoi tu do, dam bao du lieu nhat quan va co the quan ly rieng (them/sua/vo hieu hoa).
 */
@Getter
@Setter
@Entity
@Table(name = "position")
public class Position extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
