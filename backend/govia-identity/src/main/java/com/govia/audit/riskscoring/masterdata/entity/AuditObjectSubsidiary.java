package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Danh muc doi tuong kiem toan Cong ty con (sheet ZTC_DTKT2, bang ZTB_DTKT2). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_subsidiary")
public class AuditObjectSubsidiary extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "company_type", length = 100)
    private String companyType;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "staff_count")
    private Integer staffCount;

    @Column(name = "leader_count")
    private Integer leaderCount;

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

    @Column(name = "revenue", precision = 20, scale = 2)
    private BigDecimal revenue;

    @Column(name = "cost", precision = 20, scale = 2)
    private BigDecimal cost;

    @Column(name = "profit", precision = 20, scale = 2)
    private BigDecimal profit;

    @Column(name = "salary_fund", precision = 20, scale = 2)
    private BigDecimal salaryFund;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
