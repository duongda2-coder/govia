package com.govia.audit.tdkp.assignment;

import com.govia.audit.tdkp.common.TdkpTarget;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpAssignmentDto {

    private AuditTdkpAssignmentDto() {
    }

    public record Request(
            @NotNull TdkpAssignmentScope scope,
            UUID recommendationTypeId,
            UUID businessSegmentId,
            TdkpTarget targetObject,
            UUID auditObjectUnitId,
            UUID geographicAreaId,
            @NotNull UUID employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record Response(
            UUID id,
            TdkpAssignmentScope scope,
            UUID recommendationTypeId,
            String recommendationTypeName,
            UUID businessSegmentId,
            String businessSegmentCode,
            String businessSegmentName,
            TdkpTarget targetObject,
            String targetObjectLabel,
            UUID auditObjectUnitId,
            String auditObjectUnitCode,
            String auditObjectUnitName,
            UUID geographicAreaId,
            String geographicAreaName,
            UUID employeeId,
            String employeeCode,
            String employeeName,
            String username,
            String departmentName,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
