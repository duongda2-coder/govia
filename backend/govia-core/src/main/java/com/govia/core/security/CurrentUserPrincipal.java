package com.govia.core.security;

import java.util.List;
import java.util.UUID;

/**
 * Thong tin nguoi dung duoc giai ma tu JWT, gan vao SecurityContext.
 * Dung chung cho moi module de biet ai dang goi API, thuoc tenant nao, quyen gi.
 */
public record CurrentUserPrincipal(
        UUID userId,
        String username,
        UUID tenantId,
        String employeeCode,
        List<String> roles,
        List<String> permissions
) {
}
