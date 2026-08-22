package com.govia.core.export;

import java.util.List;

/** Ket qua import Excel dung chung - moi dong loi ghi ro so dong (tinh ca header) va ly do. */
public record ImportResult(int successCount, int failureCount, List<ImportRowError> errors) {

    public record ImportRowError(int row, String message) {
    }
}
