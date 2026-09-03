package com.govia.core.export;

import java.util.List;

/**
 * Ket qua import Excel dung chung - moi dong loi ghi ro so dong (tinh ca header) va ly do.
 * "notices" la cac thong bao KHONG phai loi (vd: mat khau tam vua tao khi import kem tai khoan dang
 * nhap) - rong voi da so module dang chi dung 3 tham so cu.
 */
public record ImportResult(int successCount, int failureCount, List<ImportRowError> errors, List<String> notices) {

    public ImportResult(int successCount, int failureCount, List<ImportRowError> errors) {
        this(successCount, failureCount, errors, List.of());
    }

    public record ImportRowError(int row, String message) {
    }
}
