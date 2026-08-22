package com.govia.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Danh muc quyen TOAN PLATFORM, khong thuoc rieng 1 tenant - moi module khai bao
 * permission cua minh o day theo chuan "MODULE.RESOURCE.ACTION", vd "AUDIT.FINDING.CREATE".
 * Khong ke thua BaseEntity vi day la catalog dung chung, khong phan vung theo tenant.
 */
@Getter
@Setter
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 150)
    private String code;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "description", length = 500)
    private String description;

    /** Ten hien thi cua man hinh (resource) chua quyen nay, vd "Quan ly nhan vien" - dung de nhom quyen tren UI/Excel
     * ma khong can hardcode danh sach man hinh trong code FE. */
    @Column(name = "resource_label", length = 150)
    private String resourceLabel;
}
