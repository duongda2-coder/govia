package com.govia.audit.controlpoint.dto;

import com.govia.audit.controlpoint.entity.AuditControlType;
import com.govia.audit.masterdata.entity.AuditLevel;

import java.util.UUID;

public record AuditControlPointResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String name,
        String possibleRisk,
        String controlPointByStep,
        String actualControl,
        AuditControlType controlType,
        AuditLevel controlFrequency,
        String auditProcedure,
        String residualRiskAssessment,
        String processRegulation,
        String referenceClause,
        String processEffectiveness,
        String controlEffectivenessAssessment,
        String controlEfficiencyAssessment,
        boolean active
) {
}
