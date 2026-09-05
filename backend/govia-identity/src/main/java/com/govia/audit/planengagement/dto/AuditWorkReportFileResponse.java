package com.govia.audit.planengagement.dto;

import java.time.Instant;
import java.util.UUID;

/** 1 dong "File báo cáo khác" (sheet "Quản lý công việc", muc "1. File báo cáo khác"). */
public record AuditWorkReportFileResponse(
        UUID id,
        String businessSegmentCode,
        Instant uploadedAt,
        String uploadedByUsername,
        String uploadedByName,
        /** "Loại báo cáo": TINDUNG neu mang nghiep vu cua nguoi upload la LN, DIEUHANH neu la CE,
         * con lai la NTINDUNG - dung theo dac ta. */
        String reportType,
        String fileName
) {
}
