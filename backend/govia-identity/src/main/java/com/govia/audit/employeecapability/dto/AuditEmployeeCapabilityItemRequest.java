package com.govia.audit.employeecapability.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 1 dong cap nhat trong 1 lan luu hang loat (bulk save) cua man hinh khai bao kha nang dam nhan linh vuc. */
public record AuditEmployeeCapabilityItemRequest(
        @NotNull UUID employeeId,
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
        boolean dgclCapable
) {
}
