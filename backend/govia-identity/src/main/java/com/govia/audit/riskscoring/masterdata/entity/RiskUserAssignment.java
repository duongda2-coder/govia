package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Phan quyen user duoc nhap/xem 1 chi tieu dinh luong theo chi nhanh (sheet ZTC_HSRR_DL_User). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_user_assignment")
public class RiskUserAssignment extends BaseEntity {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "criteria_id", nullable = false, columnDefinition = "uuid")
    private UUID criteriaId;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "classification", length = 255)
    private String classification;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
