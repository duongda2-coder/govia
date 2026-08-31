package com.govia.core.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    @Override
    public List<Map<String, String>> parse(InputStream inputStream, List<ExportColumn> columns) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            Map<String, String> headerToField = new HashMap<>();
            for (ExportColumn column : columns) {
                headerToField.put(column.header(), column.field());
            }

            Map<Integer, String> columnIndexToField = new HashMap<>();
            for (Cell cell : headerRow) {
                String field = headerToField.get(cell.getStringCellValue().trim());
                if (field != null) {
                    columnIndexToField.put(cell.getColumnIndex(), field);
                }
            }

            DataFormatter formatter = new DataFormatter();
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> rowData = new HashMap<>();
                boolean hasData = false;
                for (Map.Entry<Integer, String> entry : columnIndexToField.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    String value = cellToString(cell, formatter);
                    rowData.put(entry.getValue(), value);
                    hasData = hasData || !value.isEmpty();
                }
                if (hasData) {
                    rows.add(rowData);
                }
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file Excel", e);
        }
    }

    /** O NUMERIC doc truc tiep gia tri so thay vi qua DataFormatter - formatCellValue ap dung dinh
     * dang hien thi cua o (vd dau phan cach hang nghin "1,234" hoac dau "%" cho o dinh dang phan
     * tram) khien cac ham parseDecimal/parseInt phia sau parse that bai va am tham tra ve null. */
    private String cellToString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell).trim();
    }
}
