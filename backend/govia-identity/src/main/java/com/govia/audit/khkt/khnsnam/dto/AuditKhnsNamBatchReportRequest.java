package com.govia.audit.khkt.khnsnam.dto;

import java.time.LocalDate;
import java.util.List;

/** Thong tin NSD nhap khi bam "Xuat bao cao theo dot" (file mau ZTC_BC_DOT): so/ngay quyet dinh ghi o dong tieu de va
 * cot "Thoi gian kiem toan" cua tung don vi. Tat ca deu tuy chon - de trong thi de trong o file de NSD dien tay. */
public record AuditKhnsNamBatchReportRequest(
        String decisionNumber,
        LocalDate decisionDate,
        List<UnitPeriod> unitPeriods
) {
    public record UnitPeriod(String auditObjectCode, String period) {
    }
}
