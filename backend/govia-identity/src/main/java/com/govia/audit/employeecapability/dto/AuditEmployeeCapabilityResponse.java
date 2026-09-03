package com.govia.audit.employeecapability.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEmployeeCapabilityResponse(
        UUID employeeId,
        String employeeCode,
        String username,
        String fullName,
        boolean theCapable,
        boolean qtdhCapable,
        boolean hdvCapable,
        boolean tcktCapable,
        boolean cnttCapable,
        boolean ttkqCapable,
        boolean pcrtCapable,
        boolean ttqtCapable,
        boolean xdcbCapable,
        boolean tdCapable,
        boolean truongDoanCapable,
        boolean truongNhomCapable,
        boolean toGiamSatCapable,
        boolean dgclCapable,
        String enteredBy,
        Instant updatedAt,
        boolean approved,
        String approvedBy,
        Instant approvedAt
) {
}
