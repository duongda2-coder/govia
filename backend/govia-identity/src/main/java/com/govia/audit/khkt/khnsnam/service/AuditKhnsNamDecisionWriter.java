package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Do du lieu vao 2 file Word mau cua Ban kiem soat: "QD thanh lap doan" va "QD kiem ke" (templates/audit/*.docx). Phan chu do
 * (FF0000) trong file mau la phan tu sinh theo danh sach can bo cua doan kiem toan tai 1 chi nhanh; ban sao khong con mau do.
 * Ten chi nhanh mau "Hoang Mai" duoc thay bang chi nhanh dang xuat. Cac o so quyet dinh/ngay thang... giu nguyen de NSD dien tay.
 *
 * <p>Sua truc tiep word/document.xml (giu nguyen moi phan khac cua file .docx) - khong dung POI XWPF vi chi can nhan ban/doi text
 * vai doan van co dinh dang san. */
final class AuditKhnsNamDecisionWriter {

    /** 1 can bo trong doan. salutation = Ong/Ba; position = chuc danh (danh muc chuc vu); teamRole = vai tro trong doan. */
    record Member(String salutation, String fullName, String position, String teamRole, LocalDate idIssueDate, String idIssuePlace) {
    }

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String TEAM_TEMPLATE = "templates/audit/qd-thanh-lap-doan.docx";
    private static final String INVENTORY_TEMPLATE = "templates/audit/qd-kiem-ke.docx";
    private static final String SAMPLE_BRANCH = "Hoàng Mai";
    private static final String W14_NS = "http://schemas.microsoft.com/office/word/2010/wordml";
    private static final String RED = "FF0000";
    private static final String BLANK_DATE = "……/……/……";
    private static final String BLANK_PLACE = "……………";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AuditKhnsNamDecisionWriter() {
    }

    /** "Quyet dinh thanh lap doan kiem toan noi bo": bang 4 cot (STT, Ong/Ba + ten, chuc vu, vai tro trong doan), moi can bo 1 dong. */
    static byte[] writeTeamDecision(String branchName, List<Member> members) {
        return fill(TEAM_TEMPLATE, branchName, (doc, redParagraphs) -> {
            // cac dong bang co chu do la can bo mau; dong dau tien lam mau cho tung can bo that
            List<Element> sampleRows = ancestors(redParagraphs, "tr");
            Element prototype = sampleRows.get(0);
            int stt = 1;
            for (Member m : members) {
                Element row = (Element) prototype.cloneNode(true);
                stripRed(row);
                List<Element> cells = children(row, "tc");
                String[] texts = {stt + ".", m.salutation() + " " + m.fullName(), nullToEmpty(m.position()), nullToEmpty(m.teamRole())};
                for (int i = 0; i < texts.length; i++) {
                    setText(cells.get(i), texts[i]);
                }
                prototype.getParentNode().insertBefore(row, prototype);
                stt++;
            }
            return new ArrayList<>(sampleRows);
        });
    }

    /** "Quyet dinh cu can bo chung kien kiem ke": moi can bo 1 dong danh so tu dong cua Word: "Ong X, cap ngay dd/MM/yyyy, noi cap Y;". */
    static byte[] writeInventoryDecision(String branchName, List<Member> members) {
        return fill(INVENTORY_TEMPLATE, branchName, (doc, redParagraphs) -> {
            // redParagraphs: [huong dan tu sinh, "VD:", dong vi du] - dong vi du la dong danh so nen la mau cho tung can bo
            Element prototype = redParagraphs.get(redParagraphs.size() - 1);
            Node anchor = redParagraphs.get(0);
            for (int i = 0; i < members.size(); i++) {
                Member m = members.get(i);
                String date = m.idIssueDate() == null ? BLANK_DATE : DATE_FORMAT.format(m.idIssueDate());
                String place = m.idIssuePlace() == null || m.idIssuePlace().isBlank() ? BLANK_PLACE : m.idIssuePlace().trim();
                String end = i == members.size() - 1 ? "." : ";";
                Element copy = (Element) prototype.cloneNode(true);
                stripRed(copy);
                setText(copy, m.salutation() + " " + m.fullName() + ", cấp ngày " + date + ", nơi cấp " + place + end);
                anchor.getParentNode().insertBefore(copy, anchor);
            }
            return new ArrayList<>(redParagraphs);
        });
    }

    /** Dien danh sach can bo vao tai lieu; tra ve cac node mau (chu do) can xoa sau khi da chen ban sao that. */
    private interface RedSectionFiller {
        List<Node> fill(Document doc, List<Element> redParagraphs);
    }

    private static byte[] fill(String templatePath, String branchName, RedSectionFiller filler) {
        try (InputStream template = AuditKhnsNamDecisionWriter.class.getClassLoader().getResourceAsStream(templatePath)) {
            if (template == null) {
                throw new IllegalStateException("Khong tim thay file mau: " + templatePath);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipInputStream zin = new ZipInputStream(template); ZipOutputStream zout = new ZipOutputStream(out)) {
                for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
                    byte[] content = zin.readAllBytes();
                    if ("word/document.xml".equals(entry.getName())) {
                        content = transformDocument(content, branchName, filler);
                    }
                    ZipEntry copy = new ZipEntry(entry.getName());
                    zout.putNextEntry(copy);
                    zout.write(content);
                    zout.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] transformDocument(byte[] xml, String branchName, RedSectionFiller filler) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

            List<Element> red = redParagraphs(doc);
            if (red.isEmpty()) {
                throw new IllegalStateException("File mau khong con doan van chu do de do danh sach can bo");
            }
            // doi ten chi nhanh TRUOC khi do danh sach can bo - neu khong ten can bo co chu "Hoang Mai" cung bi thay nham
            replaceBranch(doc, branchName);
            filler.fill(doc, red).forEach(sample -> sample.getParentNode().removeChild(sample));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new IllegalStateException("Khong xu ly duoc file mau Word", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Cac doan van co chu mau do (ke ca doan trong o bang) - dung thu tu trong file. */
    private static List<Element> redParagraphs(Document doc) {
        List<Element> result = new ArrayList<>();
        NodeList paragraphs = doc.getElementsByTagNameNS(W_NS, "p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element p = (Element) paragraphs.item(i);
            if (hasRedRun(p)) {
                result.add(p);
            }
        }
        return result;
    }

    /** Cac node tien to gan nhat co ten localName (vd "tr") cua tung doan van, khong trung lap, theo thu tu file. */
    private static List<Element> ancestors(List<Element> paragraphs, String localName) {
        List<Element> result = new ArrayList<>();
        for (Element p : paragraphs) {
            Node n = p.getParentNode();
            while (n != null && !(n instanceof Element e && localName.equals(e.getLocalName()))) {
                n = n.getParentNode();
            }
            if (n == null) {
                throw new IllegalStateException("Doan van chu do khong nam trong <w:" + localName + ">");
            }
            if (!result.contains((Element) n)) {
                result.add((Element) n);
            }
        }
        return result;
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element e && W_NS.equals(e.getNamespaceURI()) && localName.equals(e.getLocalName())) {
                result.add(e);
            }
        }
        return result;
    }

    private static boolean hasRedRun(Element paragraph) {
        NodeList runs = paragraph.getElementsByTagNameNS(W_NS, "r");
        for (int i = 0; i < runs.getLength(); i++) {
            for (Node c = runs.item(i).getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c instanceof Element props && "rPr".equals(props.getLocalName())) {
                    NodeList colors = props.getElementsByTagNameNS(W_NS, "color");
                    for (int j = 0; j < colors.getLength(); j++) {
                        if (RED.equalsIgnoreCase(((Element) colors.item(j)).getAttributeNS(W_NS, "val"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Bo mau do khoi ban sao (ca dinh dang cua run lan dau doan van) - chu ket qua phai mau den binh thuong. Bo luon w14:paraId/textId
     * (dinh danh doan van) de cac ban sao khong trung nhau. */
    private static void stripRed(Element paragraph) {
        NodeList all = paragraph.getElementsByTagName("*");
        removeParaIds(paragraph);
        for (int i = 0; i < all.getLength(); i++) {
            removeParaIds((Element) all.item(i));
        }
        NodeList colors = paragraph.getElementsByTagNameNS(W_NS, "color");
        for (int i = colors.getLength() - 1; i >= 0; i--) {
            Node color = colors.item(i);
            if (RED.equalsIgnoreCase(((Element) color).getAttributeNS(W_NS, "val"))) {
                color.getParentNode().removeChild(color);
            }
        }
    }

    private static void removeParaIds(Element e) {
        e.removeAttributeNS(W14_NS, "paraId");
        e.removeAttributeNS(W14_NS, "textId");
    }

    /** Dat noi dung doan van vao run co chu dau tien, xoa chu o cac run con lai (giu dinh dang run dau). */
    private static void setText(Element paragraph, String text) {
        NodeList texts = paragraph.getElementsByTagNameNS(W_NS, "t");
        for (int i = 0; i < texts.getLength(); i++) {
            Element t = (Element) texts.item(i);
            t.setTextContent(i == 0 ? text : "");
            if (i == 0) {
                t.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
            }
        }
    }

    /** "Hoang Mai" (chi nhanh trong file mau) -> chi nhanh dang xuat. Mau dat truoc ten "Agribank" / "Agribank chi nhanh" nen bo
     * tien to nay neu ten doi tuong kiem toan da co san. */
    private static void replaceBranch(Document doc, String branchName) {
        String name = shortBranchName(branchName);
        NodeList texts = doc.getElementsByTagNameNS(W_NS, "t");
        for (int i = 0; i < texts.getLength(); i++) {
            Node t = texts.item(i);
            String value = t.getTextContent();
            if (value.contains(SAMPLE_BRANCH)) {
                t.setTextContent(value.replace(SAMPLE_BRANCH, name));
            }
        }
    }

    static String shortBranchName(String auditObjectName) {
        if (auditObjectName == null) {
            return "";
        }
        String trimmed = auditObjectName.trim();
        String stripped = trimmed.replaceFirst("(?iu)^(agribank\\s+)?(chi\\s+nhánh\\s+)?", "");
        return stripped.isBlank() ? trimmed : stripped;
    }

    /** Vai tro trong doan: "Truong doan" / "Truong nhom QTĐH|TD|NTD" / "Thanh vien nhom QTĐH|TD|NTD" (nhom lay theo nghiep vu cua can bo,
     * cung cach tinh cot "Linh vuc duoc phan cong" o bao cao theo dot). */
    static String teamRole(List<String> positions, AuditKhnsRoleInTeam roleInTeam, String employeeSegmentCode) {
        AuditKhnsRoleInTeam kind = AuditKhnsPositionLabel.batchRoleKind(positions, roleInTeam);
        if (kind == null) {
            return "Thành viên";
        }
        if (kind == AuditKhnsRoleInTeam.TEAM_LEAD || kind == AuditKhnsRoleInTeam.SUPPORT) {
            return AuditKhnsPositionLabel.role(kind);
        }
        String base = kind == AuditKhnsRoleInTeam.GROUP_LEAD ? "Trưởng nhóm" : "Thành viên";
        String group = AuditKhnsPositionLabel.group(employeeSegmentCode, positions);
        return group == null ? base : (kind == AuditKhnsRoleInTeam.GROUP_LEAD ? base : base + " nhóm") + " " + group;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Ten file de tai: dung ten mau + chi nhanh. */
    static String fileName(String baseName, String branchName, int month, int year) {
        String safe = shortBranchName(branchName).replaceAll("[\\\\/:*?\"<>|]", " ").trim();
        return baseName + " - " + safe + " - T" + month + "-" + year + ".docx";
    }
}
