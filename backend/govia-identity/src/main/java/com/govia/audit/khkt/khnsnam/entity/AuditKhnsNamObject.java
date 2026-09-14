package com.govia.audit.khkt.khnsnam.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Mã đối tượng KT" cua ZTC_KHNS_NAM (cot K, chon nhieu tu TH2) - danh sach doi tuong kiem toan
 * du kien nhan vien nay se tham gia trong nam, KHONG gan voi thang cu the (xem cac cot monthN cua
 * AuditKhnsNam cho phan gan theo thang). */
@Getter
@Setter
@Entity
@Table(name = "audit_khns_nam_object")
public class AuditKhnsNamObject extends BaseEntity {

    @Column(name = "khns_nam_id", nullable = false, columnDefinition = "uuid")
    private UUID khnsNamId;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;
}
