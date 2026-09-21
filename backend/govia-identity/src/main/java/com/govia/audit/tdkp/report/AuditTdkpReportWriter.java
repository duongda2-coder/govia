package com.govia.audit.tdkp.report;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Dựng file Excel 5 báo cáo TDKP theo Template_BC_01 ... Template_BC_05 (sheet 8-12 file 6.TDKP_29.5.2026): tiêu đề/đầu mục/gộp ô/dòng số thứ tự cột
 * "(1)..(n)"/dòng TC/khối chữ ký giữ như mẫu; cột "Hệ thống tự đẩy" thay bằng dữ liệu thật. Font Times New Roman. */
final class AuditTdkpReportWriter {

    private static final float LINE_HEIGHT = 15.5f;
    private static final float MIN_ROW_HEIGHT = 20f;

    /** Ô tiêu đề cột: (dòng lệch so với dòng đầu bảng, cột, số dòng gộp, số cột gộp, chữ). */
    private record H(int row, int col, int rowSpan, int colSpan, String text) {
    }

    /** Ô chữ ký: nhãn ở dòng chữ ký, gộp từ cột from đến to. */
    private record Sig(String label, int from, int to) {
    }

    private AuditTdkpReportWriter() {
    }

    static byte[] write(AuditTdkpReportDto.ReportData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(data.type().name());
            Styles styles = new Styles(workbook);
            switch (data.type()) {
                case BC01 -> bc01(sheet, styles, data);
                case BC02 -> bc02(sheet, styles, data);
                case BC03 -> bc03(sheet, styles, data);
                case BC04 -> bc04(sheet, styles, data);
                case BC05 -> bc05(sheet, styles, data);
            }
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setPaperSize((short) 9);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ===================== Template_BC_01 =====================

    private static void bc01(XSSFSheet sheet, Styles st, AuditTdkpReportDto.ReportData data) {
        widths(sheet, 5, 10.5, 10.5, 30, 22.5, 12, 14.9, 20, 17.5, 14.6, 12.7, 20, 14.3, 14);
        letterhead(sheet, st, 0, 0, 3, "NGÂN HÀNG NÔNG NGHIỆP", "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM");
        text(sheet, 2, 11, 13, "Báo cáo 01", st.reportLabel, 18f);
        text(sheet, 3, 0, 13, "TỔNG HỢP TÌNH HÌNH THỰC HIỆN KIẾN NGHỊ", st.title, 22f);
        text(sheet, 4, 0, 13, "CỦA KIỂM TOÁN NỘI BỘ ĐỐI VỚI HỘI ĐỒNG THÀNH VIÊN, TỔNG GIÁM ĐỐC", st.title, 22f);
        text(sheet, 5, 0, 13, data.subtitle(), st.subtitle, 20f);
        int next = flatTable(sheet, st, 6, data, 14, true);
        signatures(sheet, st, next + 1, 9, 11, "........,Ngày........tháng......năm......", new Sig("Lập bảng", 1, 2), new Sig("Kiểm soát", 7, 8),
                new Sig("Trưởng Kiểm toán nội bộ", 9, 11));
    }

    // ===================== Template_BC_02 =====================

    private static void bc02(XSSFSheet sheet, Styles st, AuditTdkpReportDto.ReportData data) {
        widths(sheet, 5, 12, 10.5, 9, 22, 19.3, 14, 23.4, 23.4, 20, 11.1, 12, 11.1, 14.3, 20, 11.1, 12);
        letterhead(sheet, st, 0, 0, 3, "NGÂN HÀNG NÔNG NGHIỆP", "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM");
        text(sheet, 2, 2, 5, "Chi nhánh: " + data.heading(), st.left, 18f);
        text(sheet, 2, 15, 16, "Báo cáo 02", st.reportLabel, 18f);
        text(sheet, 3, 0, 16, "BÁO CÁO TÌNH HÌNH THỰC HIỆN KIẾN NGHỊ", st.title, 22f);
        text(sheet, 4, 0, 16, "CỦA KIỂM TOÁN NỘI BỘ TẠI AGRIBANK CHI NHÁNH " + data.heading().toUpperCase(java.util.Locale.ROOT), st.title, 22f);
        text(sheet, 5, 0, 16, data.subtitle(), st.subtitle, 34f);
        int next = flatTable(sheet, st, 6, data, 17, true);
        next = signatures(sheet, st, next + 1, -1, -1, null, new Sig("Lập biểu", 3, 5), new Sig("Kiểm soát", 11, 13));
        // khoi thong ke cuoi bao cao (Tong so kien nghi / TTSS / da thuc hien / con phai chinh sua)
        next++;
        for (List<String> stat : data.stats()) {
            Row row = sheet.createRow(next);
            row.setHeightInPoints(MIN_ROW_HEIGHT);
            merged(sheet, row, 3, 5, stat.get(0), st.statLabel);
            cell(row, 6, stat.get(1), st.statValue);
            if (stat.size() > 2 && !stat.get(2).isEmpty()) {
                cell(row, 7, stat.get(2), st.statValue);
            }
            next++;
        }
    }

    // ===================== Template_BC_03 =====================

    private static void bc03(XSSFSheet sheet, Styles st, AuditTdkpReportDto.ReportData data) {
        widths(sheet, 10.3, 10.4, 10.4, 9, 24, 17.4, 14, 13.7, 12.9, 11.1, 11.1, 17.6, 11.1, 14);
        letterhead(sheet, st, 0, 2, 4, "NGÂN HÀNG NÔNG NGHIỆP", "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM", "BAN KIỂM SOÁT", "KIỂM TOÁN NỘI BỘ");
        text(sheet, 3, 11, 13, "Báo cáo 03", st.reportLabel, 18f);
        text(sheet, 4, 0, 13, "BÁO CÁO TỔNG HỢP TÌNH HÌNH THỰC HIỆN KIẾN NGHỊ", st.title, 22f);
        text(sheet, 5, 0, 13, "CỦA KIỂM TOÁN NỘI BỘ TẠI CÁC CHI NHÁNH ĐƯỢC KIỂM TOÁN NỘI BỘ", st.title, 22f);
        text(sheet, 6, 0, 13, data.subtitle(), st.subtitle, 20f);
        List<H> headers = new ArrayList<>();
        String[] leaf = {"STT", "Chi nhánh", "Mã chi nhánh", "Năm KT", "Nội dung kiến nghị", "Phân loại kiến nghị", "Lĩnh vực kiến nghị (QTĐH, TD, HĐV…)",
                "Số kiến nghị", "Số tồn tại sai sót liên quan"};
        for (int c = 0; c < leaf.length; c++) {
            headers.add(new H(0, c, 2, 1, leaf[c]));
        }
        headers.add(new H(0, 9, 1, 2, "Thực hiện chỉnh sửa trong kỳ"));
        headers.add(new H(1, 9, 1, 1, "Số kiến nghị"));
        headers.add(new H(1, 10, 1, 1, "Số tồn tại sai sót liên quan"));
        headers.add(new H(0, 11, 1, 2, "Số kiến nghị còn phải thực hiện"));
        headers.add(new H(1, 11, 1, 1, "Số kiến nghị"));
        headers.add(new H(1, 12, 1, 1, "Số tồn tại sai sót liên quan"));
        headers.add(new H(0, 13, 2, 1, "Ghi Chú"));
        int next = table(sheet, st, 7, headers, 14, data.rows(), data.totals(), true);
        signatures(sheet, st, next + 1, -1, -1, null, new Sig("Lập biểu", 3, 5), new Sig("Kiểm soát", 11, 13));
    }

    // ===================== Template_BC_04 =====================

    private static void bc04(XSSFSheet sheet, Styles st, AuditTdkpReportDto.ReportData data) {
        widths(sheet, 8, 13, 19.4, 22, 34, 13.6, 30, 13.4, 30, 15.1, 18.6);
        letterhead(sheet, st, 0, 0, 3, "NGÂN HÀNG NÔNG NGHIỆP", "VÀ PHÁT TRIỂN NÔNG THÔN VIỆT NAM");
        text(sheet, 2, 9, 10, "Báo cáo 04", st.reportLabel, 18f);
        text(sheet, 3, 0, 10, "TỔNG HỢP TÌNH HÌNH THỰC HIỆN KIẾN NGHỊ", st.title, 22f);
        text(sheet, 4, 0, 10, "CỦA CÁC ĐƠN VỊ, BỘ PHẬN ĐỐI VỚI KTNB", st.title, 22f);
        text(sheet, 5, 0, 10, data.subtitle(), st.subtitle, 20f);
        List<H> headers = new ArrayList<>();
        String[] leaf = {"STT", "Số báo cáo", "Ngày báo cáo", "Đơn vị kiến nghị", "Nội dung kiến nghị", "Thời hạn hoàn thành"};
        for (int c = 0; c < leaf.length; c++) {
            headers.add(new H(0, c, 2, 1, leaf[c]));
        }
        headers.add(new H(0, 6, 1, 2, "Tình hình thực hiện kiến nghị"));
        headers.add(new H(1, 6, 1, 1, "Nội dung"));
        headers.add(new H(1, 7, 1, 1, "Hiện trạng (đã/đang/chưa)"));
        headers.add(new H(0, 8, 1, 2, "Đánh giá tình hình thực hiện kiến nghị"));
        headers.add(new H(1, 8, 1, 1, "Nội dung"));
        headers.add(new H(1, 9, 1, 1, "Trạng thái (trong hạn/quá hạn)"));
        headers.add(new H(0, 10, 2, 1, "Ghi Chú"));
        int next = table(sheet, st, 6, headers, 11, data.rows(), data.totals(), true);
        signatures(sheet, st, next + 1, -1, -1, null, new Sig("Lập bảng", 1, 2), new Sig("Kiểm soát", 6, 7));
    }

    // ===================== Template_BC_05 =====================

    private static void bc05(XSSFSheet sheet, Styles st, AuditTdkpReportDto.ReportData data) {
        widths(sheet, 19.9, 22.9, 40.9, 45.3, 16.1, 16.1, 27.3, 28.9);
        text(sheet, 0, 0, 7, "BẢNG TỔNG HỢP THEO DÕI TÌNH HÌNH THỰC HIỆN NGHỊ QUYẾT HỘI ĐỒNG THÀNH VIÊN", st.title, 24f);
        text(sheet, 1, 0, 7, "Năm: " + data.heading(), st.reportLabelLeft, 20f);
        List<H> headers = new ArrayList<>();
        headers.add(new H(0, 0, 2, 1, "SỐ NQ"));
        headers.add(new H(0, 1, 2, 1, "NGÀY BAN HÀNH"));
        headers.add(new H(0, 2, 2, 1, "NỘI DUNG NGHỊ QUYẾT"));
        headers.add(new H(0, 3, 1, 3, "THỜI ĐIỂM " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(data.asOfDate())));
        headers.add(new H(1, 3, 1, 1, "TÌNH HÌNH THỰC HIỆN"));
        headers.add(new H(1, 4, 1, 1, "ĐÁNH GIÁ (Đã/Đang/Chưa thực hiện)"));
        headers.add(new H(1, 5, 1, 1, "NGƯỜI GIÁM SÁT"));
        headers.add(new H(0, 6, 2, 1, "ĐÁNH GIÁ VỀ VIỆC BAN HÀNH NGHỊ QUYẾT CỦA HĐTV"));
        headers.add(new H(0, 7, 2, 1, "Ghi chú"));
        table(sheet, st, 2, headers, 8, data.rows(), null, false);
    }

    // ===================== khung chung =====================

    /** Bang 1 tang tieu de (moi cot gop 2 dong) + dong so thu tu cot (1)..(n) - dung cho BC01, BC02. */
    private static int flatTable(XSSFSheet sheet, Styles st, int headerRow, AuditTdkpReportDto.ReportData data, int cols, boolean numbering) {
        List<H> headers = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            headers.add(new H(0, c, 2, 1, data.headers().get(c)));
        }
        return table(sheet, st, headerRow, headers, cols, data.rows(), data.totals(), numbering);
    }

    /** Ve tieu de (2 dong tu headerRow), dong so cot (neu numbering), du lieu, dong tong; tra ve chi so dong ke tiep sau bang. */
    private static int table(XSSFSheet sheet, Styles st, int headerRow, List<H> headers, int cols, List<List<String>> rows, List<String> totals, boolean numbering) {
        Row first = sheet.createRow(headerRow);
        Row second = sheet.createRow(headerRow + 1);
        first.setHeightInPoints(30f);
        second.setHeightInPoints(48f);
        for (int c = 0; c < cols; c++) {
            cell(first, c, null, st.header);
            cell(second, c, null, st.header);
        }
        for (H h : headers) {
            Row row = sheet.getRow(headerRow + h.row());
            row.getCell(h.col()).setCellValue(h.text());
            if (h.rowSpan() > 1 || h.colSpan() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(headerRow + h.row(), headerRow + h.row() + h.rowSpan() - 1, h.col(), h.col() + h.colSpan() - 1));
            }
        }
        int r = headerRow + 2;
        if (numbering) {
            Row numberRow = sheet.createRow(r++);
            numberRow.setHeightInPoints(18f);
            for (int c = 0; c < cols; c++) {
                cell(numberRow, c, "(" + (c + 1) + ")", st.numbering);
            }
        }
        for (List<String> data : rows) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(estimateHeight(sheet, data, cols));
            for (int c = 0; c < cols; c++) {
                String value = c < data.size() ? data.get(c) : "";
                Cell cell = row.createCell(c);
                setValue(cell, value);
                cell.setCellStyle(isNumericColumnValue(value) || value.length() <= 12 ? st.centerCell : st.leftCell);
            }
        }
        if (totals != null) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(MIN_ROW_HEIGHT);
            for (int c = 0; c < cols; c++) {
                String value = c < totals.size() ? totals.get(c) : "";
                Cell cell = row.createCell(c);
                setValue(cell, value);
                cell.setCellStyle(st.totalCell);
            }
        }
        return r;
    }

    /** So nguyen/thuan so -> ghi dang so de Excel tinh toan duoc; con lai ghi chuoi. */
    private static void setValue(Cell cell, String value) {
        if (value.matches("(0|[1-9]\\d{0,5})")) {
            cell.setCellValue(Double.parseDouble(value));
        } else {
            cell.setCellValue(value);
        }
    }

    private static boolean isNumericColumnValue(String value) {
        return value.matches("\\d{1,9}");
    }

    private static float estimateHeight(XSSFSheet sheet, List<String> data, int cols) {
        int lines = 1;
        for (int c = 0; c < cols && c < data.size(); c++) {
            String value = data.get(c);
            double chars = Math.max(6, sheet.getColumnWidth(c) / 256.0 - 1);
            int needed = 0;
            for (String part : value.split("\n", -1)) {
                needed += Math.max(1, (int) Math.ceil(part.length() / chars));
            }
            lines = Math.max(lines, needed);
        }
        return Math.max(MIN_ROW_HEIGHT, lines * LINE_HEIGHT);
    }

    /** Khoi chu ky: dong ngay thang (neu co) roi dong nhan chu ky; tra ve dong ke tiep. */
    private static int signatures(XSSFSheet sheet, Styles st, int r, int dateFrom, int dateTo, String dateText, Sig... sigs) {
        if (dateText != null) {
            Row dateRow = sheet.createRow(r++);
            dateRow.setHeightInPoints(MIN_ROW_HEIGHT);
            merged(sheet, dateRow, dateFrom, dateTo, dateText, st.signatureItalic);
        }
        Row row = sheet.createRow(r);
        row.setHeightInPoints(MIN_ROW_HEIGHT);
        for (Sig sig : sigs) {
            merged(sheet, row, sig.from(), sig.to(), sig.label(), st.signature);
        }
        return r + 5;
    }

    /** Quoc hieu/co quan chu quan: cac dong lien tiep, gop cot from..to, can giua. */
    private static void letterhead(XSSFSheet sheet, Styles st, int firstRow, int from, int to, String... lines) {
        for (int i = 0; i < lines.length; i++) {
            text(sheet, firstRow + i, from, to, lines[i], i >= 2 && lines.length > 2 ? st.centeredBold : st.centered, 18f);
        }
    }

    private static void widths(XSSFSheet sheet, double... widths) {
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, (int) (widths[c] * 256));
        }
    }

    private static void text(XSSFSheet sheet, int r, int from, int to, String value, XSSFCellStyle style, float height) {
        Row row = sheet.getRow(r) != null ? sheet.getRow(r) : sheet.createRow(r);
        row.setHeightInPoints(height);
        merged(sheet, row, from, to, value, style);
    }

    private static void merged(XSSFSheet sheet, Row row, int from, int to, String value, XSSFCellStyle style) {
        for (int c = from; c <= to; c++) {
            cell(row, c, c == from ? value : null, style);
        }
        if (to > from) {
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), from, to));
        }
    }

    private static Cell cell(Row row, int c, String value, XSSFCellStyle style) {
        Cell cell = row.getCell(c) != null ? row.getCell(c) : row.createCell(c);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
        return cell;
    }

    /** Bo style dung chung - Times New Roman. */
    private static final class Styles {
        final XSSFCellStyle title;
        final XSSFCellStyle subtitle;
        final XSSFCellStyle centered;
        final XSSFCellStyle centeredBold;
        final XSSFCellStyle reportLabel;
        final XSSFCellStyle reportLabelLeft;
        final XSSFCellStyle left;
        final XSSFCellStyle header;
        final XSSFCellStyle numbering;
        final XSSFCellStyle centerCell;
        final XSSFCellStyle leftCell;
        final XSSFCellStyle totalCell;
        final XSSFCellStyle signature;
        final XSSFCellStyle signatureItalic;
        final XSSFCellStyle statLabel;
        final XSSFCellStyle statValue;

        Styles(XSSFWorkbook wb) {
            XSSFFont regular = font(wb, 12, false, false);
            XSSFFont bold = font(wb, 12, true, false);
            XSSFFont boldBig = font(wb, 14, true, false);
            XSSFFont italic = font(wb, 12, false, true);
            XSSFFont boldItalic = font(wb, 12, true, true);

            title = style(wb, boldBig, HorizontalAlignment.CENTER, false, false);
            subtitle = style(wb, italic, HorizontalAlignment.CENTER, false, true);
            centered = style(wb, regular, HorizontalAlignment.CENTER, false, false);
            centeredBold = style(wb, bold, HorizontalAlignment.CENTER, false, false);
            reportLabel = style(wb, boldItalic, HorizontalAlignment.RIGHT, false, false);
            reportLabelLeft = style(wb, bold, HorizontalAlignment.LEFT, false, false);
            left = style(wb, bold, HorizontalAlignment.LEFT, false, false);
            header = style(wb, bold, HorizontalAlignment.CENTER, true, true);
            numbering = style(wb, italic, HorizontalAlignment.CENTER, true, false);
            centerCell = style(wb, regular, HorizontalAlignment.CENTER, true, true);
            leftCell = style(wb, regular, HorizontalAlignment.LEFT, true, true);
            totalCell = style(wb, bold, HorizontalAlignment.CENTER, true, true);
            signature = style(wb, bold, HorizontalAlignment.CENTER, false, false);
            signatureItalic = style(wb, italic, HorizontalAlignment.CENTER, false, false);
            statLabel = style(wb, regular, HorizontalAlignment.LEFT, false, false);
            statValue = style(wb, bold, HorizontalAlignment.CENTER, false, false);
        }

        private static XSSFFont font(XSSFWorkbook wb, int size, boolean bold, boolean italic) {
            XSSFFont font = wb.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setItalic(italic);
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
