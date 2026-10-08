package com.govia.audit.agent.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Ban nhap thu don doc - 1 thu cho moi don vi. Danh sach kien nghi trong thu do HE THONG dung tu du lieu
 * that (ma, noi dung, thoi han, so ngay qua han); AI chi viet tieu de, loi mo dau va ket. KHONG gui di dau
 * ca: nguoi dung sao chep va gui theo kenh van ban hien co.
 */
public record ReminderDraftResponse(String source, String sourceLabel, List<Letter> letters, boolean grounded, String model) {

    public record Letter(String unit, String subject, String body, List<Item> items) {
    }

    public record Item(String code, String content, LocalDate deadline, Long daysOverdue, String status) {
    }
}
