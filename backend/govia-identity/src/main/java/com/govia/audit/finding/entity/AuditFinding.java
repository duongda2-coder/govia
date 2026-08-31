package com.govia.audit.finding.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 1 phat hien kiem toan gan voi 1 chi nhanh - nguon du lieu that cho tool "get_audit_findings" cua
 * AI Agent (xem docs/audit-tools-contract.md). branchCode la string thuan, khong FK cung toi
 * AuditObjectUnit - cung pattern voi RiskCriteriaQualitativeValue. severity tham chieu code cua
 * AuditMasterDataItem (category RISK_LEVEL) - xem AuditFindingService.validateSeverity.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_finding")
public class AuditFinding extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "detected_date", nullable = false)
    private LocalDate detectedDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
