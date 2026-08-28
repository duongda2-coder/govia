package com.govia.audit.controlpoint.entity;

import com.govia.audit.masterdata.entity.AuditLevel;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Danh muc "Chot kiem soat" (sheet ZTC_CKS, bang ZTB_CKS) - cac diem kiem soat chuan theo tung
 * mang nghiep vu, dung lam tham chieu khi danh gia kiem soat noi bo trong 1 cuoc kiem toan. */
@Getter
@Setter
@Entity
@Table(name = "audit_control_point")
public class AuditControlPoint extends BaseEntity {

    /** "Mang nghiep vu" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT (sheet ZTC_Mang_NV). */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "possible_risk", length = 500)
    private String possibleRisk;

    @Column(name = "control_point_by_step", length = 1000)
    private String controlPointByStep;

    @Column(name = "actual_control", length = 1000)
    private String actualControl;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", length = 20)
    private AuditControlType controlType;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_frequency", length = 20)
    private AuditLevel controlFrequency;

    @Column(name = "audit_procedure", length = 1000)
    private String auditProcedure;

    @Column(name = "residual_risk_assessment", length = 1000)
    private String residualRiskAssessment;

    @Column(name = "process_regulation", length = 255)
    private String processRegulation;

    @Column(name = "reference_clause", length = 500)
    private String referenceClause;

    @Column(name = "process_effectiveness", length = 255)
    private String processEffectiveness;

    @Column(name = "control_effectiveness_assessment", length = 1000)
    private String controlEffectivenessAssessment;

    @Column(name = "control_efficiency_assessment", length = 1000)
    private String controlEfficiencyAssessment;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
