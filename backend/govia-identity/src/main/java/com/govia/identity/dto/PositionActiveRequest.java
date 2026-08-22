package com.govia.identity.dto;

import jakarta.validation.constraints.NotNull;

public record PositionActiveRequest(@NotNull Boolean active) {
}
