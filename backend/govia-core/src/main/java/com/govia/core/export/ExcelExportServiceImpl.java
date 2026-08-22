package com.govia.core.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    @Override
    public byte[] export(String sheetName, List<ExportColumn> columns, List<Map<String, Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName == null ? "Data" : sheetName);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(columns.get(c).header());
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Map<String, Object> rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < columns.size(); c++) {
                    Object value = rowData.get(columns.get(c).field());
                    row.createCell(c).setCellValue(value == null ? "" : value.toString());
                }
            }

            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Khong the xuat Excel", e);
        }
    }
}
