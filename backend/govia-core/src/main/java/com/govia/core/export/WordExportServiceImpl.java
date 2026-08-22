package com.govia.core.export;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@Service
public class WordExportServiceImpl implements WordExportService {

    @Override
    public byte[] export(String title, List<ExportColumn> columns, List<Map<String, Object>> rows) {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            XWPFTable table = document.createTable(rows.size() + 1, columns.size());
            XWPFTableRow headerRow = table.getRow(0);
            for (int c = 0; c < columns.size(); c++) {
                setCellText(headerRow.getCell(c), columns.get(c).header());
            }

            for (int r = 0; r < rows.size(); r++) {
                XWPFTableRow row = table.getRow(r + 1);
                Map<String, Object> rowData = rows.get(r);
                for (int c = 0; c < columns.size(); c++) {
                    Object value = rowData.get(columns.get(c).field());
                    setCellText(row.getCell(c), value == null ? "" : value.toString());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Khong the xuat Word", e);
        }
    }

    private void setCellText(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.createRun().setText(text);
    }
}
