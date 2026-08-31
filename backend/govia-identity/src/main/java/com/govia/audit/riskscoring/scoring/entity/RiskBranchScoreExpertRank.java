package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bang xep hang rui ro chi nhanh theo y kien chuyen gia (sheet ZTC_DGRR_cg, tcode
 * ztc_dgrr_cg/ztb_dgrr_cg) - 1 dong ung voi 1 chi nhanh trong 1 nam da co ket qua o "Ket qua cham
 * diem tong hop" (xem RiskBranchScoreCombinedService). totalScore/baseRankLabel la ANH CHUP
 * (snapshot) tai thoi diem "Cap nhat du lieu tu nguon" - cac truong con lai do chuyen gia nhap khi
 * ra soat lai xep hang. Doi tuong luon la chi nhanh (khong co chieu "loai doi tuong" nhu track
 * "rui ro khac" - xem RiskAssessmentOtherExpertRank).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_branch_score_expert_rank")
public class RiskBranchScoreExpertRank extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "assessment_year", nullable = false)
    private Integer year;

    /** "Tong diem" - anh chup tu ket qua cham diem tong hop tai lan cap nhat gan nhat. */
    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    /** "Xep hang (Xep loai)" - anh chup tu ket qua cham diem tong hop tai lan cap nhat gan nhat. */
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
