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

    /** Chuc vu cua can bo tai don vi nay (KHNS_PB), nhieu gia tri cach nhau dau phay - xem {@link AuditKhnsPosition}.
     * null = chua co chuc vu chi tiet (du lieu nhap tu KHNS_NAM) -> KHNS_PB suy ra tu roleInTeam. */
    @Column(name = "positions", length = 200)
    private String positions;

    /** Ma nghiep vu (BUSINESS_SEGMENT) can bo lam tai don vi nay, cach nhau dau phay; toi da 3 va khong tron Tin dung voi NTD. */
    @Column(name = "segment_codes", length = 100)
    private String segmentCodes;
}
