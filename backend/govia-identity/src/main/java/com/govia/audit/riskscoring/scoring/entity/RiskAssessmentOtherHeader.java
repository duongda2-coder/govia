package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Header cua man hinh "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet
 * ZTC_CDRR_KHAC, tcode ztc_cdrr_khac/ztb_cdrr_khac) - dai dien cho 1 ky cham diem cua 1 doi tuong
 * kiem toan (auditObjectCode, tro toi 1 trong 4 danh muc doi tuong tuy theo auditObjectCategoryId
 * - xem RiskAssessmentOtherService) trong 1 nam. Cac dong "Line" (xem RiskAssessmentOtherLine)
 * duoc he thong tu dong sinh theo danh muc chi tieu (RiskCriteriaOther) phu hop voi category nay.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_assessment_other_header")
public class RiskAssessmentOtherHeader extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "assessment_year", nullable = false)
    private Integer year;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
