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
 * "Ho so rui ro dinh luong" - gia tri thuc te da nhap/upload cho 1 chi tieu dinh luong
 * (RiskCriteriaQuantitative) cua 1 chi nhanh trong 1 nam (sheet ZTC_HSRR, template upload
 * DL_HSRR_Upload, luu vao "man hinh" DL_Nhaptructiep trong tai lieu goc). 1 dong o day tuong ung 1
 * O trong bang wide-format goc (1 dong = 1 chi nhanh/nam, N cot = N chi tieu) sau khi da unpivot.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_quantitative_value")
public class RiskCriteriaQuantitativeValue extends BaseEntity {

    @Column(name = "criteria_id", nullable = false, columnDefinition = "uuid")
    private UUID criteriaId;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "assessment_year", nullable = false)
    private Integer year;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "value", precision = 18, scale = 4)
    private BigDecimal value;
}
