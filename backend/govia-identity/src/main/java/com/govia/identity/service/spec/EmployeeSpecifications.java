package com.govia.identity.service.spec;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Cac dieu kien loc dung chung cho list/export nhan vien - ket hop qua Specification.and(),
 * dieu kien null se tu dong bo qua (khong sinh predicate).
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> tenantId(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Employee> orgUnitId(UUID orgUnitId) {
        return orgUnitId == null ? null : (root, query, cb) -> cb.equal(root.get("orgUnitId"), orgUnitId);
    }

    public static Specification<Employee> status(EmployeeStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Employee> keyword(String keyword) {
        if (isBlank(keyword)) {
            return null;
        }
        String like = likePattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), like),
                cb.like(cb.lower(root.get("employeeCode")), like));
    }

    /** Loc theo tung cot rieng le - dung cho tim kiem inline tren header bang. */
    public static Specification<Employee> fieldContains(String field, String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), like);
    }

    public static Specification<Employee> orgUnitNameContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> {
            Join<Object, Object> orgUnit = root.join("orgUnit", JoinType.LEFT);
            return cb.like(cb.lower(orgUnit.get("name")), like);
        };
    }

    /** Employee.positionId tro toi AuditMasterDataItem (category=POSITION) - khac module nen khong co
     * quan he JPA @ManyToOne de join truc tiep, phai loc qua subquery theo id. */
    public static Specification<Employee> positionNameContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<AuditMasterDataItem> item = sub.from(AuditMasterDataItem.class);
            sub.select(item.get("id"))
                    .where(cb.equal(item.get("category"), AuditMasterDataCategory.POSITION),
                            cb.like(cb.lower(item.get("name")), like));
            return root.get("positionId").in(sub);
        };
    }

    /** Employee.departmentId tro toi AuditMasterDataItem (category=DEPARTMENT) - cung ly do khong co
     * quan he JPA @ManyToOne nhu positionNameContains o tren. */
    public static Specification<Employee> departmentNameContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<AuditMasterDataItem> item = sub.from(AuditMasterDataItem.class);
            sub.select(item.get("id"))
                    .where(cb.equal(item.get("category"), AuditMasterDataCategory.DEPARTMENT),
                            cb.like(cb.lower(item.get("name")), like));
            return root.get("departmentId").in(sub);
        };
    }

    public static Specification<Employee> managerNameContains(String value) {
        if (isBlank(value)) {
            return null;
        }
        String like = likePattern(value);
        return (root, query, cb) -> {
            Join<Object, Object> manager = root.join("manager", JoinType.LEFT);
            return cb.like(cb.lower(manager.get("fullName")), like);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
