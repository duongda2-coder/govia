package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(@NotNull EmployeeStatus status) {
}
