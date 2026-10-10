package com.govia.audit.agent.dto;

import java.util.List;

/**
 * Ket qua "Kiểm tra file trước khi import" (A7, G4). readyToImport = du cot mau, khong co loi dong (canh bao van co the
 * con - nguoi dung tu quyet). He thong khong import gi: nguoi dung sua file roi tu bam Import tren man hinh danh muc.
 */
public record ImportCheckResponse(
        String catalog,
        String catalogLabel,
        String fileName,
        int totalRows,
        int okRows,
        int errorRows,
        int warningRows,
        List<String> expectedHeaders,
        List<String> missingHeaders,
        List<String> unknownHeaders,
        List<String> headerHints,
        String codeColumn,
        String yearColumn,
        String nameColumn,
        List<Issue> errors,
        List<Issue> warnings,
        boolean truncated,
        boolean readyToImport
) {

    public record Issue(int row, String message) {
    }
}
