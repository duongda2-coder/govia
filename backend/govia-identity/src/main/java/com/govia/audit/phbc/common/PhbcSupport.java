package com.govia.audit.phbc.common;

import com.govia.core.web.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Tien ich dung chung cho nhom man hinh "Phat hanh bao cao" (PHBC): sinh ma, parse Excel - sao chep tu TdkpSupport. */
public final class PhbcSupport {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    private PhbcSupport() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
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

    public static ResponseEntity<byte[]> xlsx(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
