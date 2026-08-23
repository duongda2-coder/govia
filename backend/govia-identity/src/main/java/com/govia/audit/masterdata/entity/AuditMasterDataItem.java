package com.govia.audit.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 1 dong danh muc dung chung cho TOAN BO module Kiem toan noi bo - cot "category" phan biet dong
 * nay thuoc danh muc nao (xem AuditMasterDataCategory). Mot bang duy nhat thay vi 1 bang rieng cho
 * moi danh muc, vi tat ca deu cung hinh dang: ma + ten + mo ta + thu tu + hieu luc.
 *
 * validFrom/validTo: chi mot so danh muc dung (vd FISCAL_PERIOD la ky bat dau/ket thuc, REGULATION/
 * POLICY/STANDARD la ngay hieu luc) - de trong voi danh muc khac.
 * parentId: tu tham chieu, dung cho danh muc co cha-con (vd BUSINESS_PROCESS_STEP thuoc ve 1
 * BUSINESS_PROCESS) - de trong voi danh muc phang binh thuong.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_master_data_item")
public class AuditMasterDataItem extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private AuditMasterDataCategory category;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
