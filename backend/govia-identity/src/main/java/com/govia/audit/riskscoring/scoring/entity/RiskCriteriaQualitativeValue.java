package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * "Ho so rui ro dinh tinh" - gia tri thuc te da nhap/upload cho 1 chi tieu dinh tinh
 * (RiskCriteriaQualitative) cua 1 chi nhanh trong 1 nam (sheet ZTC_HSRR, template upload
 * DT_HSRR_Upload, luu vao "man hinh" DT_Nhaptructiep trong tai lieu goc). "violation" giu nguyen
 * dang chuoi vi file mau co ca gia tri "N" lan so (0/1).
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_criteria_qualitative_value")
public class RiskCriteriaQualitativeValue extends BaseEntity {

    @Column(name = "criteria_id", nullable = false, columnDefinition = "uuid")
    private UUID criteriaId;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "assessment_year", nullable = false)
    private Integer year;

    @Column(name = "violation", length = 20)
    private String violation;

    @Column(name = "note", length = 500)
    private String note;
}
