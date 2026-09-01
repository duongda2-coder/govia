package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginResolveRequest(
        @NotBlank String pendingToken,
        @NotBlank @Pattern(regexp = "KICK_OTHERS|ALLOW_BOTH") String action
) {
}
