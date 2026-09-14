package com.govia.audit.khkt.thang.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** "Khai bao so thang kiem toan trong nam" (sheet ZTC_KHKT_THANG, bang ZTB_KHKT_THANG) - phan tich
 * chon (kiem toan hay khong) cho tung thang trong nam, cho 1 doi tuong kiem toan da co trong TH2
 * (AuditKhktThConfirmed) cua nam do. Cac cot con lai cua man hinh (ten, phan loai, xep hang, du no,
 * nguon von, khu vuc dia ly, linh vuc kiem toan, quy mo) deu doc song tu TH2/AuditObjectUnit/
 * AuditKhktScale - bang nay CHI luu phan NSD tu nhap (12 thang + ghi chu). */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_thang")
public class AuditKhktThang extends BaseEntity {

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "month1", nullable = false)
    private boolean month1 = false;

    @Column(name = "month2", nullable = false)
    private boolean month2 = false;

    @Column(name = "month3", nullable = false)
    private boolean month3 = false;

    @Column(name = "month4", nullable = false)
    private boolean month4 = false;

    @Column(name = "month5", nullable = false)
    private boolean month5 = false;

    @Column(name = "month6", nullable = false)
    private boolean month6 = false;

    @Column(name = "month7", nullable = false)
    private boolean month7 = false;

    @Column(name = "month8", nullable = false)
    private boolean month8 = false;

    @Column(name = "month9", nullable = false)
    private boolean month9 = false;

    @Column(name = "month10", nullable = false)
    private boolean month10 = false;

    @Column(name = "month11", nullable = false)
    private boolean month11 = false;

    @Column(name = "month12", nullable = false)
    private boolean month12 = false;

    @Column(name = "note", length = 250)
    private String note;
}
