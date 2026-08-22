package com.govia.core.export;

import java.util.List;
import java.util.Map;

/**
 * DLL xuat Excel dung chung cho toan platform - moi man hinh chi can truyen
 * ten sheet, danh sach cot va du lieu dang list of map, khong can tu viet POI.
 */
public interface ExcelExportService {

    byte[] export(String sheetName, List<ExportColumn> columns, List<Map<String, Object>> rows);
}
