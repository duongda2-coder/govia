package com.govia.audit.planengagement.statistics.dto;

import java.util.UUID;

/** "4: Thống kê theo đơn vị bị kiểm toán" - 1 dòng cho mỗi chi nhánh (AuditObjectUnit). */
public record UnitStatRow(
        UUID auditObjectUnitId,
        String unitCode,
        String unitName,
        long engagementCount,
        long ttssCount,
        long materialTtssCount,
        long recommendationCount
) {
}
