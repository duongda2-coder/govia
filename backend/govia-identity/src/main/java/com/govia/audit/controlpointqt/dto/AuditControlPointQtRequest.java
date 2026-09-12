package com.govia.audit.controlpointqt.dto;

import com.govia.audit.controlpoint.entity.AuditControlType;
import com.govia.audit.masterdata.entity.AuditLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditControlPointQtRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String name,
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
        Integer applicableYear,
        boolean active
) {
}
