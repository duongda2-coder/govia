package com.govia.audit.khkt.khnsnam.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFConnector;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/** Dung file Excel "Bao cao theo dot" theo mau ZTC_BC_DOT: "NHAN SU CAC DOAN KIEM TOAN NOI BO THANG x NAM y" - moi don vi kiem toan
 * trong thang la 1 khoi dong (STT, Don vi, Linh vuc, Thoi gian gop o), moi can bo trong doan 1 dong (Ho ten, Don vi cong tac,
 * Linh vuc duoc phan cong, Chuc vu). Cac bieu mau/font/vien giu nhu file mau (Times New Roman 12). */
final class AuditKhnsNamBatchReportWriter {

    record Staff(String fullName, String department, String group, String role) {
    }

    record Unit(String name, String segments, String period, List<Staff> staff) {
    }

    private static final String[] HEADERS = {"STT", "Đơn vị kiểm toán", "Lĩnh vực kiểm toán", "Thời gian kiểm toán", "Họ và tên",
            "Đơn vị công tác", "Lĩnh vực được phân công kiểm toán", "Chức vụ"};
    private static final int[] WIDTHS = {6, 28, 18, 18, 26, 18, 22, 18};
    /** So ky tu 1 dong cua cot "Don vi kiem toan" (gop o nhieu dong) - dung de uoc luong chieu cao khi ten dai. */
    private static final int UNIT_NAME_CHARS_PER_LINE = 28;
    private static final float LINE_HEIGHT = 16f;
    private static final float MIN_ROW_HEIGHT = 20f;

    private AuditKhnsNamBatchReportWriter() {
    }

    static byte[] write(int year, int month, String decisionNumber, LocalDate decisionDate, List<Unit> units) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Báo cáo theo đợt");
            Styles styles = new Styles(workbook);
            for (int c = 0; c < WIDTHS.length; c++) {
                sheet.setColumnWidth(c, WIDTHS[c] * 256);
            }

            int r = 0;
            r = letterhead(sheet, styles, r);
            text(sheet, r, 0, 7, "NHÂN SỰ CÁC ĐOÀN KIỂM TOÁN NỘI BỘ THÁNG " + month + " NĂM " + year, styles.title, 24f);
            r++;
            text(sheet, r, 0, 7, decisionLine(decisionNumber, decisionDate), styles.subtitle, 20f);
            r += 2;

            Row header = sheet.createRow(r++);
            header.setHeightInPoints(32f);
            for (int c = 0; c < HEADERS.length; c++) {
                cell(header, c, HEADERS[c], styles.header);
            }

            int stt = 1;
            for (Unit unit : units) {
                r = unitBlock(sheet, styles, r, stt++, unit);
            }

            r++;
            r = signatures(sheet, styles, r);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** "Theo Quyet dinh so ___/QD-BKS ngay __ thang __ nam __" - phan chua nhap de trong (khoang trang) nhu file mau. */
    static String decisionLine(String decisionNumber, LocalDate decisionDate) {
        String number = decisionNumber == null || decisionNumber.isBlank() ? "    " : decisionNumber.trim();
        String day = decisionDate == null ? "    " : String.format("%02d", decisionDate.getDayOfMonth());
        String month = decisionDate == null ? "    " : String.format("%02d", decisionDate.getMonthValue());
        String year = decisionDate == null ? "    " : String.valueOf(decisionDate.getYear());
        return "Theo Quyết định số " + number + "/QĐ-BKS ngày " + day + " tháng " + month + " năm " + year;
    }

    private static int letterhead(XSSFSheet sheet, Styles styles, int r) {
        // ben trai: co quan chu quan (A:D) - ben phai: quoc hieu, tieu ngu (E:G) nhu file mau
        text(sheet, r, 0, 3, "NGÂN HÀNG NÔNG NGHIỆP", styles.centered, 18f);
        text(sheet, r, 4, 6, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", styles.centeredBold, 18f);
        text(sheet, r + 1, 0, 3, "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM", styles.centered, 18f);
        text(sheet, r + 1, 4, 6, "Độc Lập -Tự do- Hạnh phúc", styles.centeredBoldUnderline, 18f);
        text(sheet, r + 2, 0, 3, "BAN KIỂM SOÁT", styles.centered, 18f);
        text(sheet, r + 3, 0, 3, "KIỂM TOÁN NỘI BỘ", styles.centeredBold, 18f);

        // gach ngang ngan phia tren "BAN KIEM SOAT" (connector cua file mau), can giua khoi A:D (giao cot B/C)
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(1428750, 0, 485775, 0, 1, r + 2, 2, r + 2);
        XSSFConnector line = drawing.createConnector(anchor);
        line.setLineWidth(0.75);
        line.setLineStyleColor(0, 0, 0);
        return r + 4;
    }

    private static int unitBlock(XSSFSheet sheet, Styles styles, int firstRow, int stt, Unit unit) {
        int rows = Math.max(1, unit.staff().size());
        int nameLines = (int) Math.ceil(Math.max(1, unit.name() == null ? 1 : unit.name().length()) / (double) UNIT_NAME_CHARS_PER_LINE);
        float rowHeight = Math.max(MIN_ROW_HEIGHT, nameLines * LINE_HEIGHT / rows);
        for (int i = 0; i < rows; i++) {
            Row row = sheet.createRow(firstRow + i);
            row.setHeightInPoints(rowHeight);
            // cac o gop (A:D) van can vien o moi dong de khung bang day du sau khi gop
            for (int c = 0; c < 4; c++) {
                cell(row, c, null, c == 1 ? styles.leftCell : styles.centerCell);
            }
            if (i < unit.staff().size()) {
                Staff s = unit.staff().get(i);
                cell(row, 4, s.fullName(), styles.leftCell);
                cell(row, 5, s.department(), styles.leftCell);
                cell(row, 6, s.group(), styles.centerCell);
                cell(row, 7, s.role(), styles.centerCell);
            } else {
                for (int c = 4; c < 8; c++) {
                    cell(row, c, null, styles.leftCell);
                }
            }
        }
        Row first = sheet.getRow(firstRow);
        first.getCell(0).setCellValue(stt);
        first.getCell(1).setCellValue(unit.name());
        first.getCell(2).setCellValue(unit.segments() == null ? "" : unit.segments());
        first.getCell(3).setCellValue(unit.period() == null ? "" : unit.period());
        if (rows > 1) {
            for (int c = 0; c < 4; c++) {
                sheet.addMergedRegion(new CellRangeAddress(firstRow, firstRow + rows - 1, c, c));
            }
        }
        return firstRow + rows;
    }

    private static int signatures(XSSFSheet sheet, Styles styles, int r) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(20f);
        merged(sheet, row, 0, 1, "LẬP BIỂU", styles.signature);
        merged(sheet, row, 2, 3, "TRƯỞNG PHÒNG KẾ HOẠCH", styles.signature);
        merged(sheet, row, 5, 7, "TRƯỞNG KIỂM TOÁN NỘI BỘ", styles.signature);
        return r + 5;
    }

    private static void merged(XSSFSheet sheet, Row row, int from, int to, String value, XSSFCellStyle style) {
        for (int c = from; c <= to; c++) {
            cell(row, c, c == from ? value : null, style);
        }
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), from, to));
    }

    private static void text(XSSFSheet sheet, int r, int from, int to, String value, XSSFCellStyle style, float height) {
        Row row = sheet.getRow(r) != null ? sheet.getRow(r) : sheet.createRow(r);
        row.setHeightInPoints(height);
        merged(sheet, row, from, to, value, style);
    }

    private static Cell cell(Row row, int c, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(c);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
        return cell;
    }

    /** Bo style dung chung - Times New Roman nhu file mau. */
    private static final class Styles {
        final XSSFCellStyle title;
        final XSSFCellStyle subtitle;
        final XSSFCellStyle centered;
        final XSSFCellStyle centeredBold;
        final XSSFCellStyle centeredBoldUnderline;
        final XSSFCellStyle header;
        final XSSFCellStyle centerCell;
        final XSSFCellStyle leftCell;
        final XSSFCellStyle signature;

        Styles(XSSFWorkbook wb) {
            XSSFFont regular = font(wb, 12, false, false, false);
            XSSFFont bold = font(wb, 12, true, false, false);
            XSSFFont boldUnderline = font(wb, 12, true, false, true);
            XSSFFont boldBig = font(wb, 14, true, false, false);
            XSSFFont boldItalic = font(wb, 12, true, true, false);

            title = style(wb, boldBig, HorizontalAlignment.CENTER, false, false);
            subtitle = style(wb, boldItalic, HorizontalAlignment.CENTER, false, false);
            centered = style(wb, regular, HorizontalAlignment.CENTER, false, false);
            centeredBold = style(wb, bold, HorizontalAlignment.CENTER, false, false);
            centeredBoldUnderline = style(wb, boldUnderline, HorizontalAlignment.CENTER, false, false);
            header = style(wb, bold, HorizontalAlignment.CENTER, true, true);
            centerCell = style(wb, regular, HorizontalAlignment.CENTER, true, true);
            leftCell = style(wb, regular, HorizontalAlignment.LEFT, true, true);
            signature = style(wb, bold, HorizontalAlignment.CENTER, false, false);
        }

        private static XSSFFont font(XSSFWorkbook wb, int size, boolean bold, boolean italic, boolean underline) {
            XSSFFont font = wb.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setItalic(italic);
            if (underline) {
                font.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);
            }
            return font;
        }

        private static XSSFCellStyle style(XSSFWorkbook wb, XSSFFont font, HorizontalAlignment align, boolean bordered, boolean wrap) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFont(font);
            s.setAlignment(align);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(wrap);
            s.setFillPattern(FillPatternType.NO_FILL);
            if (bordered) {
                s.setBorderTop(BorderStyle.THIN);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBorderLeft(BorderStyle.THIN);
                s.setBorderRight(BorderStyle.THIN);
            }
            return s;
        }
    }
}
