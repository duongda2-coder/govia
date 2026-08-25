package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Danh muc doi tuong kiem toan Du an/Dich vu thue ngoai (sheet ZTC_DTKT3, bang ZTB_DTKT3). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_project")
public class AuditObjectProject extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "project_type", length = 50)
    private String projectType;

    @Column(name = "approval_authority", length = 100)
    private String approvalAuthority;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "investment_value", precision = 20, scale = 2)
    private BigDecimal investmentValue;

    @Column(name = "provider", length = 255)
    private String provider;

    @Column(name = "related_parties", length = 255)
    private String relatedParties;

    @Column(name = "inspection_year")
    private Integer inspectionYear;

    @Column(name = "inspection_result", length = 1000)
    private String inspectionResult;

    @Column(name = "inspection_recommendation", length = 1000)
    private String inspectionRecommendation;

    @Column(name = "audit_year")
    private Integer auditYear;

    @Column(name = "audit_result", length = 1000)
    private String auditResult;

    @Column(name = "audit_recommendation", length = 1000)
    private String auditRecommendation;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
