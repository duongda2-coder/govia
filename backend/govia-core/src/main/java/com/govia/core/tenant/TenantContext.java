package com.govia.core.tenant;

import java.util.UUID;

/**
 * ThreadLocal holder cho tenant hien tai cua request.
 * Duoc set boi TenantFilter (tu JWT claim "tenantId") va doc lai
 * o tang repository/service de tu dong loc du lieu theo tenant.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentUser(String username) {
        CURRENT_USER.set(username);
    }

    public static String getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}
