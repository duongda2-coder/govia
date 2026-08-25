package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Danh muc "Loai doi tuong kiem toan" (sheet ZTC_Loai_Dtkt - bang ZTB_Loai_Dtkt), la danh muc GOC
 * (CNDT/CNDL/HO/IT/DA...) - cha cua Group1 (Group1 la cha cua Group2). Khac voi 4 danh muc "Doi
 * tuong kiem toan" cu the (Unit/Subsidiary/Project/Process) - danh muc nay chi phan loai/gan nhan,
 * khong phai ban ghi doi tuong cu the.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_category")
public class AuditObjectCategory extends BaseEntity {

    @Column(name = "code", nullable = false, length = 4)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
