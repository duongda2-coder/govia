package com.govia.core.tenant;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Cung cap "nguoi thuc hien" cho @CreatedBy/@LastModifiedBy trong BaseEntity,
 * lay tu TenantContext (duoc JwtAuthenticationFilter set tu token dang nhap).
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(TenantContext.getCurrentUser()).or(() -> Optional.of("system"));
    }
}
