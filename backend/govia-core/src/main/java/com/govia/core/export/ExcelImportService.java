package com.govia.core.export;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * DLL doc Excel dung chung - doc theo DUNG bo ExportColumn da dung de xuat file mau,
 * khop cot theo TIEU DE (header text) thay vi vi tri, de linh hoat neu nguoi dung doi thu tu cot.
 */
public interface ExcelImportService {

    /** Moi phan tu trong list la 1 dong du lieu, key la field cua ExportColumn, value la noi dung o (dang text). */
    List<Map<String, String>> parse(InputStream inputStream, List<ExportColumn> columns);
}
