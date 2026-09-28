package com.govia.audit.dgcl;

import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Nut "PL04B1": Excel "Tổng hợp kết quả đánh giá, phân loại chất lượng đoàn KTNB" theo sheet PL4B1 cua DGCL_CN.xlsx.
 * Nhom cot "Kết quả bộ phận đánh giá chất lượng" (G-J) lay tu PL01F da xac nhan (G32 x 100, diem cong, diem tru, xep loai);
 * nhom "Kết quả Đoàn KTNB tự đánh giá" (C-F, Phu luc 4A1) chua co nguon trong he thong nen de trong. */
final class DgclPl04b1Writer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int COLS = 11;

    private DgclPl04b1Writer() {
    }

    static byte[] write(AuditEngagementResponse engagement, List<SubjectRow> subjects) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("PL04B1");
            Styles st = new Styles(wb);
            int[] widths = {6, 38, 11, 9, 9, 18, 11, 9, 9, 24, 18};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            LocalDate today = LocalDate.now();
            text(sheet, 0, 1, "NGÂN HÀNG NÔNG NGHIỆP", st.boldCenter);
            merged(sheet, 0, 0, 6, 10, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", st.boldCenter);
            text(sheet, 1, 1, "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM", st.boldCenter);
            merged(sheet, 1, 1, 6, 10, "Độc lập - Tự do - Hạnh phúc", st.boldCenter);
            text(sheet, 2, 1, "BAN KIỂM SOÁT", st.boldCenter);
            merged(sheet, 2, 2, 6, 10, "Hà Nội, ngày " + today.getDayOfMonth() + " tháng " + today.getMonthValue() + " năm " + today.getYear(), st.italicCenter);
            text(sheet, 3, 1, "PHÒNG/TỔ ĐÁNH GIÁ", st.boldCenter);
            merged(sheet, 3, 3, 9, 10, "Phụ lục: 04B1/ĐGCL", st.boldCenter);

            merged(sheet, 5, 5, 0, COLS - 1, "TỔNG HỢP KẾT QUẢ ĐÁNH GIÁ, PHÂN LOẠI CHẤT LƯỢNG ĐOÀN KTNB", st.title);
            merged(sheet, 6, 6, 0, COLS - 1, "(Sử dụng khi cuộc kiểm toán đơn vị (Chi nhánh/Trụ sở chính) được bộ phận đánh giá)", st.italicCenter);
            merged(sheet, 7, 7, 0, COLS - 1, "Cuộc kiểm toán: " + engagement.code() + (engagement.name() == null ? "" : " - " + engagement.name()), st.italicCenter);

            String team = "Đoàn kiểm toán theo QĐ số " + orDots(engagement.decisionNumber()) + " ngày "
                    + (engagement.decisionDate() == null ? "…/…/…" : DATE.format(engagement.decisionDate())) + " của Trưởng Ban Kiểm soát";
            int h = 9;
            box(sheet, st.header, h, h + 2, 0, 0, "STT");
            box(sheet, st.header, h, h + 2, 1, 1, team);
            box(sheet, st.header, h, h, 2, 5, "Kết quả Đoàn KTNB tự đánh giá\n(* Kết quả tại Phụ lục 4A1)");
            box(sheet, st.header, h, h, 6, 9, "Kết quả bộ phận đánh giá chất lượng");
            box(sheet, st.header, h, h + 2, 10, 10, "Ghi chú");
            box(sheet, st.header, h + 1, h + 2, 2, 2, "Điểm/tỷ lệ");
            box(sheet, st.header, h + 1, h + 1, 3, 4, "Điểm cộng, điểm trừ");
            box(sheet, st.header, h + 1, h + 2, 5, 5, "Phân loại");
            box(sheet, st.header, h + 1, h + 2, 6, 6, "Điểm/tỷ lệ");
            box(sheet, st.header, h + 1, h + 1, 7, 8, "Điểm cộng, điểm trừ");
            box(sheet, st.header, h + 1, h + 2, 9, 9, "Phân loại");
            box(sheet, st.header, h + 2, h + 2, 3, 3, "Cộng");
            box(sheet, st.header, h + 2, h + 2, 4, 4, "Trừ");
            box(sheet, st.header, h + 2, h + 2, 7, 7, "Cộng");
            box(sheet, st.header, h + 2, h + 2, 8, 8, "Trừ");
            sheet.getRow(h).setHeightInPoints(48);

            int r = h + 3;
            sectionRow(sheet, st, r++, "I", "Trưởng đoàn/thành viên");
            int stt = 1;
            int memberNo = 1;
            for (SubjectRow s : subjects) {
                if (s.team()) {
                    continue;
                }
                String label = AuditDgclService.ROLE_TEAM_LEAD.equals(s.role()) ? "Trưởng đoàn" : "Thành viên " + memberNo++;
                dataRow(sheet, st, r++, String.valueOf(stt++), label + ": " + s.employeeName(), s);
            }
            sectionRow(sheet, st, r, "II", "Đoàn kiểm toán");
            subjects.stream().filter(SubjectRow::team).findFirst().ifPresent(s -> fillResult(sheet.getRow(sheet.getLastRowNum()), st, s));

            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setPaperSize((short) 9);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sectionRow(XSSFSheet sheet, Styles st, int r, String stt, String label) {
        Row row = sheet.createRow(r);
        for (int c = 0; c < COLS; c++) {
            row.createCell(c).setCellStyle(c <= 1 ? st.boldCell : st.cell);
        }
        row.getCell(0).setCellValue(stt);
        row.getCell(0).setCellStyle(st.boldCenterCell);
        row.getCell(1).setCellValue(label);
    }

    private static void dataRow(XSSFSheet sheet, Styles st, int r, String stt, String label, SubjectRow s) {
        Row row = sheet.createRow(r);
        for (int c = 0; c < COLS; c++) {
            row.createCell(c).setCellStyle(c == 1 ? st.leftCell : st.cell);
        }
        row.getCell(0).setCellValue(stt);
        row.getCell(1).setCellValue(label);
        fillResult(row, st, s);
    }

    private static void fillResult(Row row, Styles st, SubjectRow s) {
        number(row.getCell(6), s.pl01fScore(), st);
        number(row.getCell(7), s.bonusPoints(), st);
        number(row.getCell(8), s.penaltyPoints(), st);
        if (s.classification() != null) {
            row.getCell(9).setCellValue(s.classification());
        }
    }

    private static void number(Cell cell, BigDecimal value, Styles st) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(st.numberCell);
        }
    }

    private static String orDots(String s) {
        return s == null || s.isBlank() ? "…" : s;
    }

    private static void text(XSSFSheet sheet, int r, int c, String value, XSSFCellStyle style) {
        Row row = sheet.getRow(r) == null ? sheet.createRow(r) : sheet.getRow(r);
        Cell cell = row.createCell(c);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void merged(XSSFSheet sheet, int r1, int r2, int c1, int c2, String value, XSSFCellStyle style) {
        text(sheet, r1, c1, value, style);
        if (r1 != r2 || c1 != c2) {
            sheet.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    /** O co vien (ke ca cac o bi gop) de duong ke khong bi dut. */
    private static void box(XSSFSheet sheet, XSSFCellStyle style, int r1, int r2, int c1, int c2, String value) {
        for (int r = r1; r <= r2; r++) {
            Row row = sheet.getRow(r) == null ? sheet.createRow(r) : sheet.getRow(r);
            for (int c = c1; c <= c2; c++) {
                Cell cell = row.getCell(c) == null ? row.createCell(c) : row.getCell(c);
                cell.setCellStyle(style);
            }
        }
        sheet.getRow(r1).getCell(c1).setCellValue(value);
        if (r1 != r2 || c1 != c2) {
            sheet.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    private static final class Styles {
        final XSSFCellStyle title;
        final XSSFCellStyle boldCenter;
        final XSSFCellStyle italicCenter;
        final XSSFCellStyle header;
        final XSSFCellStyle cell;
        final XSSFCellStyle leftCell;
        final XSSFCellStyle boldCell;
        final XSSFCellStyle boldCenterCell;
        final XSSFCellStyle numberCell;

        Styles(XSSFWorkbook wb) {
            XSSFFont regular = font(wb, 12, false, false);
            XSSFFont bold = font(wb, 12, true, false);
            XSSFFont italic = font(wb, 12, false, true);
            title = style(wb, font(wb, 14, true, false), HorizontalAlignment.CENTER, false);
            boldCenter = style(wb, bold, HorizontalAlignment.CENTER, false);
            italicCenter = style(wb, italic, HorizontalAlignment.CENTER, false);
            header = style(wb, bold, HorizontalAlignment.CENTER, true);
            cell = style(wb, regular, HorizontalAlignment.CENTER, true);
            leftCell = style(wb, regular, HorizontalAlignment.LEFT, true);
            boldCell = style(wb, bold, HorizontalAlignment.LEFT, true);
            boldCenterCell = style(wb, bold, HorizontalAlignment.CENTER, true);
            numberCell = style(wb, regular, HorizontalAlignment.CENTER, true);
            numberCell.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        }

        private static XSSFFont font(XSSFWorkbook wb, int size, boolean bold, boolean italic) {
            XSSFFont font = wb.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setItalic(italic);
            return font;
        }

        private static XSSFCellStyle style(XSSFWorkbook wb, XSSFFont font, HorizontalAlignment align, boolean border) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFont(font);
            s.setAlignment(align);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(true);
            if (border) {
                s.setBorderTop(BorderStyle.THIN);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBorderLeft(BorderStyle.THIN);
                s.setBorderRight(BorderStyle.THIN);
            }
            return s;
        }
    }
}
