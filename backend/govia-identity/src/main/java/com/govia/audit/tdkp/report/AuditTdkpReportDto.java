package com.govia.audit.tdkp.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AuditTdkpReportDto {

    private AuditTdkpReportDto() {
    }

    /** Bộ lọc/tham số của 1 báo cáo. asOfDate = "Đến thời điểm" (mặc định hôm nay): các trạng thái Trong hạn/Quá hạn tính theo ngày này. */
    public record ReportRequest(
            @NotNull AuditTdkpReportType type,
            LocalDate asOfDate,
            /** Năm kiểm toán (BC02, BC03) / năm ban hành nghị quyết (BC05); bỏ trống = tất cả. */
            Integer year,
            /** BC02: chi nhánh cần xuất báo cáo (bắt buộc). */
            UUID branchUnitId,
            /** "ban hành kèm báo cáo số ..../BKS ngày ..." (BC01, BC03, BC04). */
            @Size(max = 50) String attachedReportNumber,
            LocalDate attachedReportDate,
            /** BC02: "Theo biên bản kiểm toán ngày ..., quyết định số ..../QĐ-BKS ngày ...". */
            LocalDate minutesDate,
            @Size(max = 50) String decisionNumber,
            LocalDate decisionDate
    ) {
    }

    /** Dữ liệu 1 báo cáo (dùng chung cho màn hình xem trước dạng bảng và file Excel). */
    public record ReportData(
            AuditTdkpReportType type,
            LocalDate asOfDate,
            /** Dòng phụ đề "(Đến thời điểm ..., ...)". */
            String subtitle,
            /** BC02: tên chi nhánh; BC05: năm. */
            String heading,
            List<String> headers,
            List<List<String>> rows,
            /** Dòng "TC" / "Tổng cộng" (cùng số cột với headers) - null nếu báo cáo không có. */
            List<String> totals,
            /** BC02: khối thống kê cuối báo cáo (nhãn, giá trị). */
            List<List<String>> stats
    ) {
    }

    public record ArchiveRequest(
            @NotNull AuditTdkpReportType reportType,
            @jakarta.validation.constraints.NotBlank @Size(max = 255) String title,
            LocalDate asOfDate,
            @Size(max = 500) String note
    ) {
    }

    public record ArchiveResponse(
            UUID id,
            AuditTdkpReportType reportType,
            String reportTypeTitle,
            String title,
            LocalDate asOfDate,
            String note,
            String createdBy,
            java.time.Instant createdAt,
            long attachmentCount
    ) {
    }
}
