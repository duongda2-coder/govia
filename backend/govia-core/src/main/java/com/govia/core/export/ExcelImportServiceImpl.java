package com.govia.core.export;

import com.govia.core.web.BusinessException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    @Override
    public List<Map<String, String>> parse(InputStream inputStream, List<ExportColumn> columns) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
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
        } catch (EncryptedDocumentException e) {
            throw new BusinessException("EXCEL_ENCRYPTED",
                    "File Excel dang duoc bao ve mat khau, vui long go bo mat khau truoc khi import", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new BusinessException("EXCEL_INVALID_FORMAT",
                    "File khong dung dinh dang Excel (.xls/.xlsx) hoac da bi hong, vui long kiem tra lai file", HttpStatus.BAD_REQUEST);
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
