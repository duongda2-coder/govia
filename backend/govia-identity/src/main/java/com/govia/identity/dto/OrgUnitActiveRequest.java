package com.govia.identity.dto;

import jakarta.validation.constraints.NotNull;

public record OrgUnitActiveRequest(@NotNull Boolean active) {
}
