package com.govia.audit.riskscoring.scoring.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Dong "Cham diem cac chi tieu rui ro" cua 1 header (xem RiskAssessmentOtherHeader) - moi dong
 * ung voi 1 chi tieu (RiskCriteriaOther); he thong tu dong sinh dong nay cho tat ca chi tieu phu
 * hop voi category cua header (xem RiskAssessmentOtherLineService#ensureLines). scaleId la muc
 * thang diem NSD chon (xem RiskCriteriaOtherScale) - null khi chua cham.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_score_assessment_other_line")
public class RiskAssessmentOtherLine extends BaseEntity {

    @Column(name = "header_id", nullable = false, columnDefinition = "uuid")
    private UUID headerId;

    @Column(name = "criteria_other_id", nullable = false, columnDefinition = "uuid")
    private UUID criteriaOtherId;

    @Column(name = "scale_id", columnDefinition = "uuid")
    private UUID scaleId;
}
