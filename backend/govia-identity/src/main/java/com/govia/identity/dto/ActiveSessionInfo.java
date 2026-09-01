package com.govia.identity.dto;

import java.time.Instant;

public record ActiveSessionInfo(
        String deviceInfo,
        String ipAddress,
        Instant loginAt
) {
}
