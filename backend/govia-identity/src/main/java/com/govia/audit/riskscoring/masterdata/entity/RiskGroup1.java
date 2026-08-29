package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Nhom chi tieu cham diem rui ro cap 1 (sheet ZTC_DGRR_Group1 - bang ZTB_DGRR_GROUP).
 * auditObjectCategoryId tro toi danh muc goc "Loai doi tuong kiem toan" (sheet ZTC_Loai_Dtkt,
 * xem AuditObjectCategory) - la cha cua Group1 (Group1 la cha cua Group2).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_group1")
public class RiskGroup1 extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;

    /** Ma nghiep vu (khop domain cua RiskWeightByBusiness.businessCode) - chi dung o man "Ket qua
     * cham diem tong hop" (sheet CT_Diem_All) de gop nhieu nhom cap 1 dinh luong (vi du TDQM/TDCL/
     * TDAT) ve chung 1 nghiep vu ("Tin dung"/LN) khi quy doi theo ti trong dinh tinh/dinh luong. */
    @Column(name = "business_line_code", length = 10)
    private String businessLineCode;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
