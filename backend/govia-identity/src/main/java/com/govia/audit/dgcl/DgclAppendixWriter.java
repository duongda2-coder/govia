package com.govia.audit.dgcl;

import com.govia.audit.dgcl.AuditDgclDto.Line;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.Summary;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nut "Xuất PL01A/PL01B/PL01F" (test 30.9): do du lieu phieu dang xem vao dung mau FORM_PL01A/FORM_PL01F/FORM_PL01B
 * (templates/audit/dgcl-pl01*.xlsx). Dong tieu chi cua mau duoc do theo noi dung cot B (khong theo so dong) nen mau co
 * the co them/bot dong so voi bo tieu chi - vd FORM_PL01F khong co dong "Trong đó: Điểm trừ chất lượng..." (F005).
 * Dong IV/V/VI (PL01A/B) va cac o diem PL01F ghi gia tri he thong da tinh (ke ca quy tac "gián đoạn"), khong dung cong thuc cua mau.
 */
final class DgclAppendixWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MARK = "X";
    private static final int MATCH_PREFIX = 25;
    /** So dong mau duoc bo qua toi da khi do dong tieu chi tiep theo. */
    private static final int LOOKAHEAD = 4;
    private static final int COL_B = 1;
    private static final int COL_C = 2;
    private static final int COL_D = 3;
    private static final int COL_E = 4;
    private static final int COL_F = 5;
    private static final int COL_G = 6;
    private static final int COL_H = 7;
    private static final int COL_I = 8;

    private DgclAppendixWriter() {
    }

    static String templatePath(DgclAppendix appendix) {
        return "templates/audit/dgcl-" + appendix.name().toLowerCase() + ".xlsx";
    }

    static byte[] write(AuditEngagementResponse engagement, Sheet data) {
        try (InputStream in = new ClassPathResource(templatePath(data.appendix())).getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.getSheetAt(0);
            Styles styles = new Styles(wb);
            fillHeader(sheet, engagement, data);

            Map<Line, Integer> rows = locateRows(sheet, data.lines());
            for (Map.Entry<Line, Integer> e : rows.entrySet()) {
                Row row = sheet.getRow(e.getValue());
                if (data.appendix() == DgclAppendix.PL01F) {
                    fillQualityRow(sheet, row, e.getKey(), data.summary(), styles);
                } else {
                    fillComplianceRow(row, e.getKey(), data.summary(), styles);
                }
            }
            fillSigner(sheet, data.evaluatorName());
            wb.setForceFormulaRecalculation(true);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Khong tao duoc file " + data.appendix(), e);
        }
    }

    // ===================== Tieu de =====================

    private static void fillHeader(XSSFSheet sheet, AuditEngagementResponse engagement, Sheet data) {
        String engagementLabel = engagement.name() == null || engagement.name().isBlank()
                ? engagement.code() : engagement.code() + " - " + engagement.name();
        LocalDate today = LocalDate.now();
        String dateLine = "Hà Nội, ngày " + today.getDayOfMonth() + " tháng " + today.getMonthValue() + " năm " + today.getYear();
        int lastHeaderRow = Math.min(sheet.getLastRowNum(), 15);
        for (int r = 0; r <= lastHeaderRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String text = cell.getStringCellValue();
                String replaced = text;
                if (replaced.contains("[Tên cuộc KT]")) {
                    replaced = data.team()
                            ? engagementLabel + " - Đoàn kiểm toán"
                            : replaced.replace("[Tên cuộc KT]", engagementLabel).replace("[Tên thành viên đoàn kiểm toán]", nz(data.subjectName()));
                }
                if (replaced.contains("[Tên đơn vị]")) {
                    replaced = replaced.replace("[Tên đơn vị]", nz(engagement.auditObjectUnitName()));
                }
                if (replaced.startsWith("Hà Nội, ngày")) {
                    replaced = dateLine;
                }
                if (replaced.contains("Quyết định số") && replaced.contains("của Trưởng Ban kiểm soát") && engagement.decisionNumber() != null) {
                    String decision = "Quyết định số " + engagement.decisionNumber()
                            + (engagement.decisionDate() == null ? "" : " ngày " + DATE.format(engagement.decisionDate()));
                    replaced = replaced.substring(0, replaced.indexOf("Quyết định số")) + decision + " của Trưởng Ban kiểm soát";
                }
                if (!replaced.equals(text)) {
                    cell.setCellValue(replaced);
                }
            }
        }
    }

    /** Ten NSD danh gia duoi dong "(Ký và ghi rõ họ tên)" cua cot "NGƯỜI THỰC HIỆN". */
    private static void fillSigner(XSSFSheet sheet, String evaluatorName) {
        if (evaluatorName == null) {
            return;
        }
        for (int r = sheet.getLastRowNum(); r > 0; r--) {
            Row row = sheet.getRow(r);
            Cell cell = row == null ? null : row.getCell(COL_B);
            if (cell != null && cell.getCellType() == CellType.STRING && cell.getStringCellValue().startsWith("(Ký và ghi rõ họ tên)")) {
                Row target = rowAt(sheet, r + 4);
                Cell name = cellAt(target, COL_B);
                name.setCellStyle(cell.getCellStyle());
                name.setCellValue(evaluatorName);
                return;
            }
        }
    }

    // ===================== Do dong =====================

    /** Dong mau cua tung tieu chi: tim dong dau tien khop noi dung, sau do do tuan tu (cho phep mau bo/them vai dong). */
    private static Map<Line, Integer> locateRows(XSSFSheet sheet, List<Line> lines) {
        Map<Line, Integer> result = new LinkedHashMap<>();
        int next = -1;
        for (Line line : lines) {
            String key = key(line.content());
            if (key.isEmpty()) {
                continue;
            }
            int from = next < 0 ? 0 : next;
            int to = next < 0 ? sheet.getLastRowNum() : Math.min(sheet.getLastRowNum(), next + LOOKAHEAD);
            for (int r = from; r <= to; r++) {
                Row row = sheet.getRow(r);
                Cell cell = row == null ? null : row.getCell(COL_B);
                if (cell != null && cell.getCellType() == CellType.STRING && key(cell.getStringCellValue()).equals(key)) {
                    result.put(line, r);
                    next = r + 1;
                    break;
                }
            }
        }
        return result;
    }

    private static String key(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase();
        return normalized.length() > MATCH_PREFIX ? normalized.substring(0, MATCH_PREFIX) : normalized;
    }

    // ===================== PL01A / PL01B =====================

    private static void fillComplianceRow(Row row, Line line, Summary summary, Styles styles) {
        if (line.header()) {
            return;
        }
        String kind = line.kind();
        if ("TOTAL_COUNT".equals(kind)) {
            number(row, COL_C, summary.requiredCount(), styles.integer);
            number(row, COL_D, summary.compliantCount(), styles.integer);
            number(row, COL_E, summary.nonCompliantCount(), styles.integer);
            return;
        }
        if ("RATIO".equals(kind)) {
            number(row, COL_D, summary.ratio(), styles.percent);
            return;
        }
        if ("SCORE".equals(kind)) {
            number(row, COL_C, 100d, styles.decimal);
            number(row, COL_D, summary.score(), styles.decimal);
            return;
        }
        if ("DISRUPTION".equals(kind)) {
            mark(row, COL_C, line.checked(), styles);
            return;
        }
        mark(row, COL_C, line.required(), styles);
        mark(row, COL_D, line.compliant(), styles);
        mark(row, COL_E, line.nonCompliant(), styles);
        text(row, COL_F, withNote(line));
        text(row, COL_G, line.document());
    }

    // ===================== PL01F =====================

    private static void fillQualityRow(XSSFSheet sheet, Row row, Line line, Summary summary, Styles styles) {
        String kind = line.kind();
        if ("CLASSIFICATION".equals(kind)) {
            Cell cell = cellAt(row, COL_C);
            cell.setCellValue(nz(summary.classification()));
            cell.setCellStyle(styles.boldCenter(cell.getCellStyle()));
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), COL_C, COL_G));
            return;
        }
        number(row, COL_C, line.calcMax(), styles.decimal);
        if ("DEDUCTION".equals(kind)) {
            number(row, COL_D, line.violationCount() == null ? null : line.violationCount().doubleValue(), styles.integer);
        } else if ("BONUS".equals(kind) || "PENALTY".equals(kind)) {
            mark(row, COL_D, line.checked(), styles);
        }
        number(row, COL_E, line.calcDeduction(), styles.decimal);
        number(row, COL_F, line.calcPoints(), styles.decimal);
        number(row, COL_G, line.calcRatio(), styles.percent);
        text(row, COL_H, withNote(line));
        text(row, COL_I, line.document());
    }

    // ===================== O =====================

    private static String withNote(Line line) {
        String detail = line.detail();
        String note = line.note();
        if (note == null || note.isBlank()) {
            return detail;
        }
        return (detail == null || detail.isBlank() ? "" : detail + "\n") + "Ghi chú: " + note;
    }

    private static void mark(Row row, int col, boolean on, Styles styles) {
        if (on) {
            Cell cell = cellAt(row, col);
            cell.setCellValue(MARK);
            cell.setCellStyle(styles.center.apply(cell));
        }
    }

    private static void text(Row row, int col, String value) {
        if (value != null && !value.isBlank()) {
            cellAt(row, col).setCellValue(value);
        }
    }

    private static void number(Row row, int col, Number value, StyleFormat format) {
        Cell cell = cellAt(row, col);
        if (cell.getCellType() == CellType.FORMULA) {
            cell.removeFormula();
        }
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(format.apply(cell));
    }

    private static Row rowAt(XSSFSheet sheet, int r) {
        return sheet.getRow(r) == null ? sheet.createRow(r) : sheet.getRow(r);
    }

    private static Cell cellAt(Row row, int col) {
        return row.getCell(col) == null ? row.createCell(col) : row.getCell(col);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @FunctionalInterface
    private interface StyleFormat {
        XSSFCellStyle apply(Cell cell);
    }

    private static final class Styles {
        private final XSSFWorkbook wb;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();
        final StyleFormat center = cell -> derived(cell, null);
        final StyleFormat integer = cell -> derived(cell, "0");
        final StyleFormat decimal = cell -> derived(cell, "0.00");
        final StyleFormat percent = cell -> derived(cell, "0.00%");

        Styles(XSSFWorkbook wb) {
            this.wb = wb;
        }

        XSSFCellStyle boldCenter(CellStyle base) {
            return cache.computeIfAbsent("bold:" + base.getIndex(), k -> {
                XSSFCellStyle style = wb.createCellStyle();
                style.cloneStyleFrom(base);
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setWrapText(true);
                XSSFFont font = wb.createFont();
                font.setFontName(wb.getFontAt(base.getFontIndex()).getFontName());
                font.setFontHeightInPoints(wb.getFontAt(base.getFontIndex()).getFontHeightInPoints());
                font.setBold(true);
                style.setFont(font);
                return style;
            });
        }

        /** Style cua mau (vien/font) + can giua, them dinh dang so neu co - moi style goc chi nhan ban 1 lan. */
        private XSSFCellStyle derived(Cell cell, String format) {
            XSSFCellStyle base = (XSSFCellStyle) cell.getCellStyle();
            return cache.computeIfAbsent(base.getIndex() + ":" + format, k -> {
                XSSFCellStyle style = wb.createCellStyle();
                style.cloneStyleFrom(base);
                if (format != null) {
                    style.setDataFormat(wb.createDataFormat().getFormat(format));
                }
                style.setAlignment(HorizontalAlignment.CENTER);
                return style;
            });
        }
    }
}
