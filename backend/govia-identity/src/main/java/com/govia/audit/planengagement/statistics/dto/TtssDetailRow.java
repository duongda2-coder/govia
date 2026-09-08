package com.govia.audit.planengagement.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** "Có chức năng xem chi tiết TTSS" - drill-down cua 1 dong Thong ke theo nam (Nam + Nghiep vu). */
public record TtssDetailRow(
        String engagementCode,
        String findingCode,
        String findingName,
        boolean material,
        String customerName,
        BigDecimal amount,
        LocalDate exceptionDate
) {
}
