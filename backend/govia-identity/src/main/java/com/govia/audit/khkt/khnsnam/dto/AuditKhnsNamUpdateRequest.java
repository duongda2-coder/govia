package com.govia.audit.khkt.khnsnam.dto;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;

import java.time.LocalDate;
import java.util.List;

public record AuditKhnsNamUpdateRequest(
        AuditKhnsRoleInTeam roleInTeam,
        String otherDuties,
        List<String> auditObjectCodes,
        String decisionNumber,
        LocalDate decisionDate,
        String expectedBatch,
        String note,
        String month1AuditObjectCode,
        String month2AuditObjectCode,
        String month3AuditObjectCode,
        String month4AuditObjectCode,
        String month5AuditObjectCode,
        String month6AuditObjectCode,
        String month7AuditObjectCode,
        String month8AuditObjectCode,
        String month9AuditObjectCode,
        String month10AuditObjectCode,
        String month11AuditObjectCode,
        String month12AuditObjectCode
) {
}
