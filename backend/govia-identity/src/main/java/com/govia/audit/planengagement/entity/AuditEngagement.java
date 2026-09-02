package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** "Cuoc kiem toan" (CKT) - man hinh "Khoi tao va quan ly cuoc kiem toan", trong nhom moi
 * "Lap ke hoach" cua module Kiem toan noi bo (nguon: file "Tao CKT.xlsx", sheet "khoi tao"). */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement")
public class AuditEngagement extends BaseEntity {

    /** Ma CKT - he thong tu sinh khi luu, khong nhan tu client. Quy tac: Loai doi tuong (unit_type
     * cua don vi duoc chon) + Ma DTKT (code cua don vi) + Nam + STT 2 chu so (dem theo don vi + nam). */
    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    /** Doi tuong kiem toan (AuditObjectUnit) duoc chon - "Loai DTKT" va "Ma DTKT" cua form deu la
     * unitType/code cua chinh don vi nay, khong luu rieng. */
    @Column(name = "audit_object_unit_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectUnitId;

    @Column(name = "engagement_year", nullable = false)
    private Integer year;

    @Column(name = "expected_month", nullable = false)
    private Integer expectedMonth;

    @Column(name = "decision_date", nullable = false)
    private LocalDate decisionDate;

    @Column(name = "team_lead_employee_id", nullable = false, columnDefinition = "uuid")
    private UUID teamLeadEmployeeId;

    @Column(name = "decision_number", nullable = false, length = 50)
    private String decisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditEngagementStatus status = AuditEngagementStatus.DRAFT;

    @Column(name = "risk_rank", length = 20)
    private String riskRank;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "objective", length = 4000)
    private String objective;

    @Column(name = "scope", length = 4000)
    private String scope;

    @Column(name = "planning_start_date")
    private LocalDate planningStartDate;

    @Column(name = "planning_end_date")
    private LocalDate planningEndDate;

    @Column(name = "fieldwork_start_date")
    private LocalDate fieldworkStartDate;

    @Column(name = "fieldwork_end_date")
    private LocalDate fieldworkEndDate;

    @Column(name = "report_start_date")
    private LocalDate reportStartDate;

    @Column(name = "report_end_date")
    private LocalDate reportEndDate;

    @Column(name = "info_collection_start")
    private LocalDateTime infoCollectionStart;

    @Column(name = "info_collection_end")
    private LocalDateTime infoCollectionEnd;

    @Column(name = "sample_request_start")
    private LocalDateTime sampleRequestStart;

    @Column(name = "sample_request_end")
    private LocalDateTime sampleRequestEnd;

    @Column(name = "report_plan_start")
    private LocalDateTime reportPlanStart;

    @Column(name = "report_plan_end")
    private LocalDateTime reportPlanEnd;
}
