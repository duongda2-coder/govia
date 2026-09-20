package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 2 file Word xuat tu man hinh KHNS_NAM ("QD thanh lap doan", "QD kiem ke"): phan chu do cua file mau phai duoc thay bang danh sach
 * can bo that, khong con mau do, ten chi nhanh mau "Hoang Mai" phai doi sang chi nhanh dang xuat. */
class AuditKhnsNamDecisionWriterTest {

    private static final List<AuditKhnsNamDecisionWriter.Member> MEMBERS = List.of(
            new AuditKhnsNamDecisionWriter.Member("Ông", "Nguyễn Hoàng Mai", "Trưởng Phòng", "Trưởng đoàn", LocalDate.of(2015, 3, 9), "Cục CS QLHC Hà Nội"),
            new AuditKhnsNamDecisionWriter.Member("Bà", "Đỗ Ngọc Việt", "Cán Bộ Kiểm Toán", "Thành viên nhóm TD", null, null));

    /** Doan van cua than van ban lan cac o bang (danh sach can bo cua 2 file mau nam trong bang). */
    private static List<XWPFParagraph> allParagraphs(XWPFDocument doc) {
        List<XWPFParagraph> result = new ArrayList<>(doc.getParagraphs());
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    result.addAll(cell.getParagraphs());
                }
            }
        }
        return result;
    }

    private static List<String> paragraphs(byte[] docx) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return allParagraphs(doc).stream().map(XWPFParagraph::getText).toList();
        }
    }

    private static boolean anyRedRun(byte[] docx) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph p : allParagraphs(doc)) {
                for (XWPFRun r : p.getRuns()) {
                    if ("FF0000".equalsIgnoreCase(r.getColor())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Test
    void teamDecisionListsEachMemberAndSwapsBranch() throws IOException {
        byte[] docx = AuditKhnsNamDecisionWriter.writeTeamDecision("Agribank chi nhánh Cầu Giấy", MEMBERS);
        List<String> text = paragraphs(docx);

        assertThat(text).anyMatch(t -> t.contains("Thành lập Đoàn kiểm toán nội bộ tại Agribank chi nhánh Cầu Giấy gồm"));
        assertThat(text).anyMatch(t -> t.contains("Giám đốc Agribank chi nhánh Cầu Giấy"));
        assertThat(text).noneMatch(t -> t.contains("Hoàng Thành Nam") || t.contains("Phạm Văn Đúng"));
        // ten can bo co chu "Hoang Mai" khong bi thay nham thanh ten chi nhanh
        assertThat(text).contains("Ông Nguyễn Hoàng Mai", "Trưởng Phòng", "Trưởng đoàn", "Bà Đỗ Ngọc Việt", "Cán Bộ Kiểm Toán", "Thành viên nhóm TD");
        assertThat(text.stream().filter(t -> t.trim().matches("\\d+\\.")).map(String::trim)).containsExactly("1.", "2.");
        assertThat(anyRedRun(docx)).isFalse();
    }

    @Test
    void inventoryDecisionListsMembersWithIdIssueInfo() throws IOException {
        byte[] docx = AuditKhnsNamDecisionWriter.writeInventoryDecision("Chi nhánh Cầu Giấy", MEMBERS);
        List<String> text = paragraphs(docx);

        assertThat(text).contains(
                "Ông Nguyễn Hoàng Mai, cấp ngày 09/03/2015, nơi cấp Cục CS QLHC Hà Nội;",
                "Bà Đỗ Ngọc Việt, cấp ngày ……/……/……, nơi cấp ……………" + ".");
        assertThat(text).anyMatch(t -> t.contains("kiểm kê đột xuất tiền mặt, tài sản quý, giấy tờ có giá tại Agribank Cầu Giấy gồm"));
        assertThat(text).noneMatch(t -> t.contains("TỰ SINH") || t.equals("VD:") || t.contains("Hoàng Mai") && !t.contains("Nguyễn Hoàng Mai"));
        assertThat(anyRedRun(docx)).isFalse();
    }

    @Test
    void teamRoleNamesGroupLikeBatchReport() {
        assertThat(AuditKhnsNamDecisionWriter.teamRole(List.of("TEAM_LEAD"), AuditKhnsRoleInTeam.TEAM_LEAD, "CE")).isEqualTo("Trưởng đoàn");
        assertThat(AuditKhnsNamDecisionWriter.teamRole(List.of("QTDH_GROUP_LEAD"), null, "CE")).isEqualTo("Trưởng nhóm QTĐH");
        assertThat(AuditKhnsNamDecisionWriter.teamRole(List.of("TD_MEMBER"), null, "LN")).isEqualTo("Thành viên nhóm TD");
        assertThat(AuditKhnsNamDecisionWriter.teamRole(List.of(), null, null)).isEqualTo("Thành viên");
    }

    @Test
    void shortBranchNameStripsAgribankPrefix() {
        assertThat(AuditKhnsNamDecisionWriter.shortBranchName("Agribank chi nhánh Hoàng Mai")).isEqualTo("Hoàng Mai");
        assertThat(AuditKhnsNamDecisionWriter.shortBranchName("Chi nhánh Cầu Giấy")).isEqualTo("Cầu Giấy");
        assertThat(AuditKhnsNamDecisionWriter.shortBranchName("Hà Đông")).isEqualTo("Hà Đông");
    }
}
