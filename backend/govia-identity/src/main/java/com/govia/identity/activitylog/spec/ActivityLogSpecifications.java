package com.govia.identity.activitylog.spec;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Dieu kien loc dung chung cho man hinh "Nhat ky thao tac" (doc bang audit_log dung chung toan
 * platform) - cung pattern voi EmployeeSpecifications, dieu kien null se tu dong bo qua. */
public final class ActivityLogSpecifications {

    private ActivityLogSpecifications() {
    }

    public static Specification<AuditLog> tenantId(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<AuditLog> entityNameContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get("entityName")), like);
    }

    public static Specification<AuditLog> action(AuditAction action) {
        return action == null ? null : (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> performedByContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get("createdBy")), like);
    }

    public static Specification<AuditLog> entityId(UUID entityId) {
        return entityId == null ? null : (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> keyword(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get("detail")), like);
    }

    /** dateTo la ngay cuoi CUNG duoc bao gom (< ngay sau do), khong phai moc gio 00:00. */
    public static Specification<AuditLog> dateBetween(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return null;
        }
        Instant from = dateFrom == null ? null : dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThan(root.get("createdAt"), to);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
