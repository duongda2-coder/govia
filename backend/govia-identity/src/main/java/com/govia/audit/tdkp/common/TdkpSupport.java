package com.govia.audit.tdkp.common;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/** Tien ich dung chung cua nhom man hinh "Theo doi khac phuc" (TDKP): trang thai han, sinh ma, parse Excel. */
public final class TdkpSupport {

    public static final String ON_TIME = "ON_TIME";
    public static final String OVERDUE = "OVERDUE";
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    private TdkpSupport() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /** "Trạng thái (Trong hạn/Quá hạn)": ngày hiện tại > Thời hạn hoàn thành -> Quá hạn, ngược lại Trong hạn; chưa có thời hạn -> null. */
    public static String deadlineState(LocalDate deadline) {
        return deadlineState(deadline, LocalDate.now());
    }

    public static String deadlineState(LocalDate deadline, LocalDate today) {
        if (deadline == null) {
            return null;
        }
        return today.isAfter(deadline) ? OVERDUE : ON_TIME;
    }

    public static String deadlineLabel(String state) {
        if (state == null) {
            return null;
        }
        return OVERDUE.equals(state) ? "Quá hạn" : "Trong hạn";
    }

    /** "Trạng thái phê duyệt" nhap/chon tay o cac man hinh TDKP (khong gan voi quy trinh Flowable,
     * khac voi AuditTtssRecord.recommendationApprovalStatus). */
    public static String approvalStatusLabel(AssignmentApprovalStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Đang chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
        };
    }

    /** Nhận tên enum hoặc nhãn tiếng Việt (file Excel import); rỗng -> null, khác list -> lỗi dòng import. */
    public static AssignmentApprovalStatus parseApprovalStatus(String text) {
        if (isBlank(text)) {
            return null;
        }
        String value = text.trim();
        for (AssignmentApprovalStatus status : AssignmentApprovalStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || approvalStatusLabel(status).equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Trang thai phe duyet khong hop le: " + value);
    }

    /** Ngay import: dd.MM.yyyy / dd/MM/yyyy / yyyy-MM-dd hoac so serial cua Excel (vd 44275). */
    public static LocalDate parseDate(String text) {
        if (isBlank(text)) {
            return null;
        }
        String value = text.trim();
        if (value.matches("\\d{4,6}(\\.0+)?")) {
            long serial = Long.parseLong(value.replaceAll("\\.0+$", ""));
            if (serial > 20000 && serial < 80000) {
                return LocalDate.of(1899, 12, 30).plusDays(serial);
            }
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (Exception ignored) {
                // thu dinh dang tiep theo
            }
        }
        throw new BusinessException("IMPORT_INVALID_DATE", "Ngay khong hop le: " + value);
    }

    public static Integer parseInt(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim().replaceAll("\\.0+$", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException("IMPORT_INVALID_NUMBER", "So khong hop le: " + text);
        }
    }

    /** Ma tu sinh dang PREFIX + so thu tu (KN001, NQ001, KNIA001): lay so lon nhat da co + 1, toi thieu 3 chu so. */
    public static String nextCode(String prefix, Collection<String> existingCodes) {
        int max = 0;
        for (String code : existingCodes) {
            if (code != null && code.startsWith(prefix) && code.substring(prefix.length()).matches("\\d+")) {
                max = Math.max(max, Integer.parseInt(code.substring(prefix.length())));
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    public static ResponseEntity<byte[]> xlsx(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
