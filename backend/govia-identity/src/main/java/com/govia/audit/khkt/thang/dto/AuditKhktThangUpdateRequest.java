package com.govia.audit.khkt.thang.dto;

public record AuditKhktThangUpdateRequest(
        boolean month1,
        boolean month2,
        boolean month3,
        boolean month4,
        boolean month5,
        boolean month6,
        boolean month7,
        boolean month8,
        boolean month9,
        boolean month10,
        boolean month11,
        boolean month12,
        String note
) {
}
