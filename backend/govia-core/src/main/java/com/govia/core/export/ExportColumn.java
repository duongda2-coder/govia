package com.govia.core.export;

/**
 * Dinh nghia 1 cot khi xuat Excel/Word: field = key trong row map, header = tieu de hien thi.
 * Man hinh nao cung dung chung 1 dinh dang nay khi goi ExcelExportService/WordExportService.
 */
public record ExportColumn(String field, String header) {
}
