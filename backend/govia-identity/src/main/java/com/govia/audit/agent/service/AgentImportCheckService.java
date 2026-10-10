package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.ImportCheckResponse;
import com.govia.audit.agent.tools.AgentWorkToolsService;
import com.govia.audit.agent.tools.ToolExecutionResult;
import com.govia.audit.controlpoint.controller.AuditControlPointController;
import com.govia.audit.controlpointqt.controller.AuditControlPointQtController;
import com.govia.audit.exceptionmapping.controller.AuditExceptionMappingController;
import com.govia.audit.exceptionmappingqt.controller.AuditExceptionMappingQtController;
import com.govia.audit.exceptiontype.controller.AuditExceptionTypeController;
import com.govia.audit.exceptiontypeqt.controller.AuditExceptionTypeQtController;
import com.govia.audit.processstep.controller.AuditProcessStepDetailController;
import com.govia.audit.processstepqt.controller.AuditProcessStepDetailQtController;
import com.govia.audit.workitem.controller.AuditWorkItemController;
import com.govia.audit.workitemqt.controller.AuditWorkItemQtController;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * "Kiểm tra file trước khi import" (A7 Danh muc, G4). Nguoi dung tai file Excel dinh import vao 1 danh muc; he thong
 * so voi DUNG mau ma chuc nang Import hien co doc (= dong tieu de cua file Xuat Excel cua danh muc do - ExcelImportService
 * khop cot theo tieu de chinh xac) va voi du lieu dang co, roi bao: thieu cot, cot la/sai chinh ta (se bi Import bo qua),
 * dong thieu ma, ma trung trong file, ma da co trong danh muc, ten trung.
 *
 * <p>Thuan xac dinh - KHONG goi model, KHONG luu file, KHONG import: nguoi dung sua file roi tu bam Import tren man
 * hinh danh muc nhu hien nay (validate cu). File chi duoc doc trong bo nho cua request nay (khong phai "doc file dinh
 * kem" cua Cong 3). Quyen: VIEW + EXPORT cua danh muc (kiem tra lai qua controller san co).
 */
@Service
public class AgentImportCheckService {

    private static final Logger log = LoggerFactory.getLogger(AgentImportCheckService.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ISSUES = 100;

    private final AgentWorkToolsService workTools;
    private final AgentProfileRegistry registry;
    private final AgentAuditLogService auditLogService;
    private final Map<String, Supplier<byte[]>> templates = new LinkedHashMap<>();

    public AgentImportCheckService(AgentWorkToolsService workTools, AgentProfileRegistry registry, AgentAuditLogService auditLogService,
                                   AuditControlPointController controlPoint, AuditControlPointQtController controlPointQt,
                                   AuditWorkItemController workItem, AuditWorkItemQtController workItemQt,
                                   AuditExceptionTypeController exceptionType, AuditExceptionTypeQtController exceptionTypeQt,
                                   AuditExceptionMappingController exceptionMapping, AuditExceptionMappingQtController exceptionMappingQt,
                                   AuditProcessStepDetailController processStep, AuditProcessStepDetailQtController processStepQt) {
        this.workTools = workTools;
        this.registry = registry;
        this.auditLogService = auditLogService;
        templates.put("control_point", () -> controlPoint.exportExcel().getBody());
        templates.put("control_point_qt", () -> controlPointQt.exportExcel().getBody());
        templates.put("work_item", () -> workItem.exportExcel().getBody());
        templates.put("work_item_qt", () -> workItemQt.exportExcel().getBody());
        templates.put("exception_type", () -> exceptionType.exportExcel().getBody());
        templates.put("exception_type_qt", () -> exceptionTypeQt.exportExcel().getBody());
        templates.put("exception_mapping", () -> exceptionMapping.exportExcel().getBody());
        templates.put("exception_mapping_qt", () -> exceptionMappingQt.exportExcel().getBody());
        templates.put("process_step", () -> processStep.exportExcel().getBody());
        templates.put("process_step_qt", () -> processStepQt.exportExcel().getBody());
    }

    public List<String> catalogKeys() {
        return List.copyOf(templates.keySet());
    }

    public ImportCheckResponse check(String catalogKey, MultipartFile file, CurrentUserPrincipal principal) {
        if (!registry.isEnabled(AgentProfileRegistry.CATALOG)) {
            throw new BusinessException("AGENT_DISABLED", "Tro ly AI Danh muc dang duoc tat theo cau hinh", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Supplier<byte[]> template = catalogKey == null ? null : templates.get(catalogKey.trim().toLowerCase());
        if (template == null) {
            throw new BusinessException("AGENT_IMPORT_CATALOG_INVALID", "catalog phai la 1 trong: " + String.join(", ", templates.keySet()),
                    HttpStatus.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("AGENT_IMPORT_FILE_EMPTY", "Chua chon file Excel", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("AGENT_IMPORT_FILE_TOO_LARGE", "File lon hon 10 MB", HttpStatus.BAD_REQUEST);
        }
        long start = System.currentTimeMillis();
        String key = catalogKey.trim().toLowerCase();
        String label = workTools.catalogLabel(key);
        List<Map<String, Object>> existing = workTools.catalogRows(key); // AUDIT.<DANH_MUC>.VIEW
        Grid expected = read(new ByteArrayInputStream(template.get())); // AUDIT.<DANH_MUC>.EXPORT
        Grid uploaded;
        try (InputStream in = file.getInputStream()) {
            uploaded = read(in);
        } catch (IOException e) {
            throw invalidFile();
        }

        // ---- 1. Cot: Import chi nhan cot co tieu de TRUNG KHOP (da trim) voi mau
        Set<String> expectedHeaders = new LinkedHashSet<>(expected.headers());
        List<String> missing = expectedHeaders.stream().filter(h -> !uploaded.headers().contains(h)).toList();
        List<String> unknown = uploaded.headers().stream().filter(h -> !h.isBlank() && !expectedHeaders.contains(h)).toList();
        List<String> hints = new ArrayList<>();
        for (String u : unknown) {
            String nu = AgentText.normalize(u);
            expectedHeaders.stream().filter(h -> AgentText.normalize(h).equals(nu)).findFirst()
                    .ifPresent(h -> hints.add("Cột \"" + u + "\" gần giống cột mẫu \"" + h + "\" nhưng không khớp chính xác - Import sẽ bỏ qua cột này"));
        }

        // ---- 2. Cot ma/nam/ten: do theo du lieu that cua danh muc (cot co gia tri trung voi truong code/name)
        int codeCol = column(expected, existing, "code");
        int nameCol = column(expected, existing, "name", "exceptionTypeName", "processStepSummaryName");
        int yearCol = column(expected, existing, "applicableYear");
        if (codeCol < 0) {
            codeCol = headerLike(expected.headers(), "ma ");
        }
        if (yearCol < 0) {
            yearCol = headerLike(expected.headers(), "nam");
        }
        if (nameCol < 0) {
            nameCol = headerLike(expected.headers(), "ten ");
        }
        String codeHeader = codeCol < 0 ? null : expected.headers().get(codeCol);
        String nameHeader = nameCol < 0 ? null : expected.headers().get(nameCol);
        String yearHeader = yearCol < 0 ? null : expected.headers().get(yearCol);
        int upCode = codeHeader == null ? -1 : uploaded.headers().indexOf(codeHeader);
        int upName = nameHeader == null ? -1 : uploaded.headers().indexOf(nameHeader);
        int upYear = yearHeader == null ? -1 : uploaded.headers().indexOf(yearHeader);

        Set<String> existingKeys = new HashSet<>();
        Map<String, String> existingNames = new HashMap<>();
        for (Map<String, Object> r : existing) {
            String code = text(r.get("code"));
            if (code != null) {
                existingKeys.add(keyOf(code, yearHeader == null ? null : text(r.get("applicableYear"))));
            }
            String name = firstText(r, "name", "exceptionTypeName", "processStepSummaryName");
            if (name != null && code != null) {
                existingNames.putIfAbsent(AgentText.normalize(name) + "|" + (yearHeader == null ? "" : Objects.toString(r.get("applicableYear"), "")), code);
            }
        }

        // ---- 3. Tung dong
        List<ImportCheckResponse.Issue> errors = new ArrayList<>();
        List<ImportCheckResponse.Issue> warnings = new ArrayList<>();
        Map<String, Integer> firstRowOfKey = new HashMap<>();
        Map<String, Integer> firstRowOfName = new HashMap<>();
        Set<Integer> errorRows = new HashSet<>();
        Set<Integer> warningRows = new HashSet<>();
        for (Map.Entry<Integer, List<String>> e : uploaded.rows().entrySet()) {
            int rowNumber = e.getKey();
            List<String> cells = e.getValue();
            String code = cell(cells, upCode);
            String year = cell(cells, upYear);
            String name = cell(cells, upName);
            if (upCode >= 0 && (code == null || code.isBlank())) {
                add(errors, errorRows, rowNumber, "Thiếu " + codeHeader);
                continue;
            }
            if (upName >= 0 && (name == null || name.isBlank())) {
                add(errors, errorRows, rowNumber, "Thiếu " + nameHeader);
            }
            if (code != null) {
                String k = keyOf(code, year);
                Integer first = firstRowOfKey.putIfAbsent(k, rowNumber);
                if (first != null) {
                    add(errors, errorRows, rowNumber, "Trùng " + codeHeader + " \"" + code + "\"" + (year == null ? "" : " năm " + year)
                            + " với dòng " + first + " trong file");
                } else if (existingKeys.contains(k)) {
                    add(warnings, warningRows, rowNumber, codeHeader + " \"" + code + "\"" + (year == null ? "" : " năm " + year)
                            + " đã có trong danh mục");
                }
            }
            if (name != null && !name.isBlank()) {
                String nk = AgentText.normalize(name) + "|" + (year == null ? "" : year);
                Integer firstName = firstRowOfName.putIfAbsent(nk, rowNumber);
                if (firstName != null) {
                    add(warnings, warningRows, rowNumber, "Tên trùng với dòng " + firstName + " trong file");
                } else {
                    String other = existingNames.get(nk);
                    if (other != null && !other.equalsIgnoreCase(code)) {
                        add(warnings, warningRows, rowNumber, "Tên trùng với mã \"" + other + "\" đang có trong danh mục");
                    }
                }
            }
        }

        int total = uploaded.rows().size();
        boolean ready = missing.isEmpty() && errors.isEmpty() && total > 0;
        ImportCheckResponse response = new ImportCheckResponse(key, label, file.getOriginalFilename(), total,
                total - errorRows.size(), errorRows.size(), warningRows.size(), List.copyOf(expectedHeaders), missing, unknown, hints,
                codeHeader, yearHeader, nameHeader, limit(errors), limit(warnings), errors.size() > MAX_ISSUES || warnings.size() > MAX_ISSUES, ready);
        auditLogService.logToolCall(principal.userId(), UUID.randomUUID(), 0, "Kiem tra file truoc import " + key, "check_import_file",
                Map.of("catalog", key, "file", String.valueOf(file.getOriginalFilename()), "rows", total),
                ToolExecutionResult.success("errors=" + errors.size() + ", warnings=" + warnings.size() + ", missingHeaders=" + missing.size()),
                System.currentTimeMillis() - start);
        return response;
    }

    // ------------------------------------------------------------------ doc Excel (cung cach ExcelImportServiceImpl)

    /** Tieu de (dong dau) + du lieu theo so dong Excel (1-based, dong tieu de = 1). */
    record Grid(List<String> headers, Map<Integer, List<String>> rows) {
    }

    private Grid read(InputStream in) {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new Grid(List.of(), Map.of());
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
            }
            Map<Integer, List<String>> rows = new LinkedHashMap<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                List<String> cells = new ArrayList<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    String v = value(row.getCell(c), formatter);
                    cells.add(v);
                    hasData = hasData || !v.isEmpty();
                }
                if (hasData) {
                    rows.put(r + 1, cells);
                }
            }
            return new Grid(headers, rows);
        } catch (EncryptedDocumentException e) {
            throw new BusinessException("EXCEL_ENCRYPTED", "File Excel dang duoc bao ve mat khau, vui long go bo mat khau truoc", HttpStatus.BAD_REQUEST);
        } catch (IOException | RuntimeException e) {
            if (e instanceof BusinessException be) {
                throw be;
            }
            log.info("Kiem tra file import: khong doc duoc file Excel ({})", e.toString());
            throw invalidFile();
        }
    }

    private static String value(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static BusinessException invalidFile() {
        return new BusinessException("EXCEL_INVALID_FORMAT", "File khong dung dinh dang Excel (.xls/.xlsx) hoac da bi hong", HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ helpers

    /** Cot cua file mau co gia tri trung voi 1 truong cua du lieu dang co (>= 80% so o co gia tri). */
    private static int column(Grid template, List<Map<String, Object>> existing, String... fields) {
        if (existing.isEmpty() || template.rows().isEmpty()) {
            return -1;
        }
        Set<String> values = new HashSet<>();
        for (Map<String, Object> r : existing) {
            String v = firstText(r, fields);
            if (v != null) {
                values.add(v.trim());
            }
        }
        if (values.isEmpty()) {
            return -1;
        }
        int best = -1;
        double bestShare = 0.8;
        for (int c = 0; c < template.headers().size(); c++) {
            int filled = 0;
            int hit = 0;
            for (List<String> cells : template.rows().values()) {
                String v = cell(cells, c);
                if (v != null && !v.isEmpty()) {
                    filled++;
                    if (values.contains(v)) {
                        hit++;
                    }
                }
            }
            double share = filled == 0 ? 0 : (double) hit / filled;
            if (share >= bestShare) {
                best = c;
                bestShare = share + 1e-9;
            }
        }
        return best;
    }

    /** Danh muc chua co du lieu: doan cot theo tieu de (khong dau) - vd "ma " cho cot ma, bo "mang nghiep vu". */
    private static int headerLike(List<String> headers, String prefix) {
        for (int c = 0; c < headers.size(); c++) {
            String n = AgentText.normalize(headers.get(c));
            if (n.startsWith(prefix) || (prefix.equals("nam") && n.contains("nam ap dung"))) {
                return c;
            }
        }
        return -1;
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        String v = cells.get(index);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String keyOf(String code, String year) {
        return code.trim().toUpperCase() + "|" + (year == null ? "" : year.trim());
    }

    private static String firstText(Map<String, Object> r, String... fields) {
        for (String f : fields) {
            String v = text(r.get(f));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String text(Object v) {
        return v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v).trim();
    }

    private static void add(List<ImportCheckResponse.Issue> target, Set<Integer> rows, int row, String message) {
        rows.add(row);
        target.add(new ImportCheckResponse.Issue(row, message));
    }

    private static List<ImportCheckResponse.Issue> limit(List<ImportCheckResponse.Issue> issues) {
        return issues.size() > MAX_ISSUES ? issues.subList(0, MAX_ISSUES) : issues;
    }
}
