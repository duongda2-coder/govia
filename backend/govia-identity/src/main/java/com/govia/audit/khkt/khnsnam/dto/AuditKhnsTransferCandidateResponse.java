package com.govia.audit.khkt.khnsnam.dto;

import java.time.LocalDate;

/** 1 dong man hinh "Chuyển thông tin KHTH" = 1 doi tuong kiem toan cua nam duoc chon (tu TH2), gop
 * tu du lieu KHNS_NAM cua nguoi duoc danh dau Truong doan cho doi tuong do. "transferable=false" khi
 * thieu du lieu bat buoc de tao AuditEngagement (xem blockReason) - nut chuyen se disable dong do. */
public record AuditKhnsTransferCandidateResponse(
        String auditObjectCode,
        String auditObjectName,
        String teamLeadEmployeeCode,
        String teamLeadEmployeeName,
        int memberCount,
        Integer expectedMonth,
        String decisionNumber,
        LocalDate decisionDate,
        String expectedBatch,
        String existingEngagementCode,
        boolean transferable,
        String blockReason
) {
}
