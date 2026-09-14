package com.govia.audit.khkt.thang.dto;

import java.math.BigDecimal;
import java.util.List;

/** 1 dong man hinh ZTC_KHKT_THANG = 1 doi tuong kiem toan da co trong TH2 cua nam duoc chon. Cac
 * truong tu auditObjectCode den businessSegmentCodes doc song tu TH2/AuditObjectUnit/AuditKhktScale
 * (khong sua o day); tu month1 den note la du lieu rieng cua man hinh nay (xem AuditKhktThang). */
public record AuditKhktThangRowResponse(
        Integer year,
        String auditObjectCode,
        String auditObjectName,
        String auditObjectCategoryCode,
        String rankLabel,
        BigDecimal onBalanceSheetLoan,
        BigDecimal fundingSource,
        Integer creditScale,
        Integer fundingScale,
        String geographicArea,
        List<String> businessSegmentCodes,
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
