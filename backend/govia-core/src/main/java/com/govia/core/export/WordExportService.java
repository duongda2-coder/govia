package com.govia.core.export;

import java.util.List;
import java.util.Map;

/**
 * DLL xuat Word dung chung cho toan platform - render tieu de + bang du lieu don gian.
 * Man hinh nao can layout phuc tap hon co the mo rong service nay theo tung module.
 */
public interface WordExportService {

    byte[] export(String title, List<ExportColumn> columns, List<Map<String, Object>> rows);
}
