package com.govia.audit.riskscoring.scoring.entity;

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
 * Bang xep hang rui ro theo y kien chuyen gia cua DTKT khac (sheet ZTC_XHRR_KHAC_CG, tcode
 * ztc_xhrr_khac_cg/ztb_xhrr_khac_cg) - 1 dong ung voi 1 doi tuong kiem toan trong 1 nam da co ket
 * qua o "Bang xep hang cham diem rui ro khac" (xem RiskAssessmentOtherRankingService). riskScore/
 * baseRankLabel la ANH CHUP (snapshot) tai thoi diem "Cap nhat du lieu tu nguon" - cac truong con
 * lai do chuyen gia nhap khi ra soat lai xep hang.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_assessment_other_expert_rank")
public class RiskAssessmentOtherExpertRank extends BaseEntity {

    @Column(name = "audit_object_category_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "audit_object_code", nullable = false, length = 20)
    private String auditObjectCode;

    @Column(name = "assessment_year", nullable = false)
    private Integer year;

    /** "Diem rui ro" - anh chup tu ZTC_BXHRR_KHAC tai lan cap nhat gan nhat. */
    @Column(name = "risk_score", precision = 10, scale = 2)
    private BigDecimal riskScore;

    /** "Xep loai" - anh chup tu ZTC_BXHRR_KHAC tai lan cap nhat gan nhat. */
    @Column(name = "base_rank_label", length = 50)
    private String baseRankLabel;

    /** "Xep hang lai theo YKCG" - chuyen gia chon lai tu danh muc xep loai (RiskScoreRank). */
    @Column(name = "re_rank_label", length = 50)
    private String reRankLabel;

    @Column(name = "reason", length = 125)
    private String reason;

    @Column(name = "assessed_date")
    private LocalDate assessedDate;

    @Column(name = "expert_name", length = 125)
    private String expertName;

    /** "Ket qua xep loai" - mac dinh bang reRankLabel, chuyen gia co the doi lai. */
    @Column(name = "final_rank_label", length = 50)
    private String finalRankLabel;
}
