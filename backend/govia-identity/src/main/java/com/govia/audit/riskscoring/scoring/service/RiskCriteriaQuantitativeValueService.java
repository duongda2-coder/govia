package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitativeOrder;
import com.govia.audit.riskscoring.masterdata.entity.RiskUserAssignment;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskUserAssignmentRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeWideRowRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeWideRowResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQuantitativeValue;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaQuantitativeValueRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Ho so rui ro dinh luong" (sheet ZTC_HSRR, upload theo mau DL_HSRR_Upload) - bang wide-format
 * (1 dong = 1 chi nhanh/nam, tung cot la 1 ma chi tieu dinh luong) nen KHONG dung duoc
 * ExcelImportService dung chung (doi hoi bo cot co dinh) - tu doc Excel bang POI, "unpivot" tung o
 * thanh 1 dong gia tri. Chi ghi gia tri neu user dang upload duoc PHAN QUYEN chi tieu do (toan bo
 * chi nhanh hoac dung chi nhanh dang upload) - xem RiskUserAssignment (sheet ZTC_HSRR_DL_User),
 * dung theo dung yeu cau "chi update nhung cot theo user duoc mapping" trong tai lieu goc.
 * SUPER_ADMIN bo qua kiem tra nay (giong pattern o WorkflowTaskService) - neu khong, tai khoan
 * admin se bi chan hoan toan cho toi khi co nguoi tao san du lieu RiskUserAssignment cho no.
 */
@Service
public class RiskCriteriaQuantitativeValueService {

    private static final Set<String> IGNORED_HEADERS = Set.of("STT", "Tên Chi Nhánh");
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RiskCriteriaQuantitativeValueRepository repository;
    private final RiskCriteriaQuantitativeRepository criteriaRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final RiskUserAssignmentRepository userAssignmentRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;

    public RiskCriteriaQuantitativeValueService(RiskCriteriaQuantitativeValueRepository repository,
                                                 RiskCriteriaQuantitativeRepository criteriaRepository,
                                                 AuditObjectUnitRepository auditObjectUnitRepository,
                                                 RiskUserAssignmentRepository userAssignmentRepository,
                                                 AuditLogService auditLogService,
                                                 ExcelExportService excelExportService,
                                                 WordExportService wordExportService) {
        this.repository = repository;
        this.criteriaRepository = criteriaRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.userAssignmentRepository = userAssignmentRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
    }

    @Transactional(readOnly = true)
    public List<RiskCriteriaQuantitativeValueResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskCriteriaQuantitative> criteria = criteriaById(tenantId);
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);
        return repository.findByTenantIdAndYearOrderByBranchCodeAsc(tenantId, year).stream()
                .map(item -> toResponse(item, criteria, units))
                .toList();
    }

    /** Ban "wide" cua list() (1 dong = 1 chi nhanh/nam, tung chi tieu la 1 entry trong map) - dung
     * dinh dang voi sheet DL_Nhaptructiep / mau DL_HSRR_Upload thay vi 1 dong/1 chi tieu. */
    @Transactional(readOnly = true)
    public List<RiskCriteriaQuantitativeWideRowResponse> listWide(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskCriteriaQuantitative> criteria = criteriaById(tenantId);
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);

        Map<String, List<RiskCriteriaQuantitativeValue>> byBranch = new LinkedHashMap<>();
        for (RiskCriteriaQuantitativeValue item : repository.findByTenantIdAndYearOrderByBranchCodeAsc(tenantId, year)) {
            byBranch.computeIfAbsent(item.getBranchCode(), k -> new ArrayList<>()).add(item);
        }

        List<RiskCriteriaQuantitativeWideRowResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<RiskCriteriaQuantitativeValue>> entry : byBranch.entrySet()) {
            result.add(toWideRow(entry.getKey(), year, entry.getValue(), criteria, units));
        }
        return result;
    }

    /** Luu tat ca gia tri cua 1 chi nhanh/nam cung luc (dong wide-format tren man hinh). Gia tri
     * null trong map xoa dong chi tieu do neu da co, khac null thi tao/cap nhat - giong het logic
     * "unpivot" cua importFromExcel nhung ap dung cho 1 dong duy nhat thay vi ca file. */
    @Transactional
    public RiskCriteriaQuantitativeWideRowResponse saveWideRow(RiskCriteriaQuantitativeWideRowRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateBranch(tenantId, request.branchCode());
        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> criteriaIdsByCode.put(c.getCode(), c.getId()));

        Map<String, BigDecimal> values = request.valuesByCriteriaCode() != null ? request.valuesByCriteriaCode() : Map.of();
        for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
            UUID criteriaId = criteriaIdsByCode.get(entry.getKey());
            if (criteriaId == null) {
                throw new BusinessException("RISK_CRITERIA_DL_NOT_FOUND", "Khong tim thay chi tieu dinh luong: " + entry.getKey());
            }
            if (entry.getValue() == null) {
                repository.findByTenantIdAndCriteriaIdAndBranchCodeAndYear(tenantId, criteriaId, request.branchCode(), request.year())
                        .ifPresent(repository::delete);
            } else {
                saveValue(tenantId, criteriaId, request.branchCode(), request.year(), request.entryDate(), entry.getValue());
            }
        }

        auditLogService.record("RiskCriteriaQuantitativeValue", null, AuditAction.UPDATE,
                "Cap nhat HSRR dinh luong (dang bang tong hop): " + request.branchCode() + "/" + request.year());

        List<RiskCriteriaQuantitativeValue> items = repository.findByTenantIdAndBranchCodeAndYear(tenantId, request.branchCode(), request.year());
        if (items.isEmpty()) {
            // Dong wide-format khong phai 1 entity rieng - no chi "ton tai" gian tiep qua it nhat 1
            // gia tri chi tieu da luu. Neu khong con gia tri nao (vd nguoi dung tao moi nhung khong
            // nhap chi tieu nao ca) thi KHONG co gi de tra ve/hien thi lai o listWide() - bao loi ro
            // thay vi tra ve "thanh cong" nhung dong lai bien mat sau khi tai lai danh sach.
            throw new BusinessException("RISK_CRITERIA_QUANTITATIVE_VALUE_EMPTY",
                    "Phai nhap it nhat 1 chi tieu dinh luong de luu duoc dong nay");
        }
        return toWideRow(request.branchCode(), request.year(), items, criteriaById(tenantId), unitsByCode(tenantId));
    }

    private RiskCriteriaQuantitativeWideRowResponse toWideRow(String branchCode, Integer year, List<RiskCriteriaQuantitativeValue> items,
                                                                Map<UUID, RiskCriteriaQuantitative> criteria, Map<String, AuditObjectUnit> units) {
        AuditObjectUnit unit = units.get(branchCode);
        Map<String, BigDecimal> values = new HashMap<>();
        LocalDate entryDate = null;
        for (RiskCriteriaQuantitativeValue item : items) {
            RiskCriteriaQuantitative criterion = criteria.get(item.getCriteriaId());
            if (criterion != null && item.getValue() != null) {
                values.put(criterion.getCode(), item.getValue());
            }
            if (item.getEntryDate() != null) {
                entryDate = item.getEntryDate();
            }
        }
        return new RiskCriteriaQuantitativeWideRowResponse(branchCode, unit != null ? unit.getName() : null, year, entryDate, values);
    }

    @Transactional
    public RiskCriteriaQuantitativeValueResponse create(RiskCriteriaQuantitativeValueRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateCriteria(tenantId, request.criteriaId());
        validateBranch(tenantId, request.branchCode());
        checkNoDuplicate(tenantId, request.criteriaId(), request.branchCode(), request.year(), null);

        RiskCriteriaQuantitativeValue item = new RiskCriteriaQuantitativeValue();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQuantitativeValue", item.getId(), AuditAction.CREATE,
                "Tao HSRR dinh luong: " + item.getBranchCode() + "/" + item.getYear());
        return toResponse(item, criteriaById(tenantId), unitsByCode(tenantId));
    }

    @Transactional
    public RiskCriteriaQuantitativeValueResponse update(UUID id, RiskCriteriaQuantitativeValueRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQuantitativeValue item = getOwnedOrThrow(tenantId, id);
        validateCriteria(tenantId, request.criteriaId());
        validateBranch(tenantId, request.branchCode());
        checkNoDuplicate(tenantId, request.criteriaId(), request.branchCode(), request.year(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQuantitativeValue", item.getId(), AuditAction.UPDATE,
                "Cap nhat HSRR dinh luong: " + item.getBranchCode() + "/" + item.getYear());
        return toResponse(item, criteriaById(tenantId), unitsByCode(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQuantitativeValue item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaQuantitativeValue", id, AuditAction.DELETE,
                "Xoa HSRR dinh luong: " + item.getBranchCode() + "/" + item.getYear());
    }

    /** Xoa 1 "dong" tren man hinh dang bang tong hop - vi 1 dong khong phai entity rieng (chi ton
     * tai gian tiep qua cac gia tri chi tieu), xoa dong nghia la xoa TOAN BO gia tri cua chi
     * nhanh/nam do cung luc. */
    @Transactional
    public void deleteWideRow(String branchCode, Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<RiskCriteriaQuantitativeValue> items = repository.findByTenantIdAndBranchCodeAndYear(tenantId, branchCode, year);
        if (items.isEmpty()) {
            throw new BusinessException("RISK_CRITERIA_QUANTITATIVE_VALUE_NOT_FOUND", "Khong tim thay du lieu HSRR cho chi nhanh/nam nay",
                    HttpStatus.NOT_FOUND);
        }
        repository.deleteAll(items);
        auditLogService.record("RiskCriteriaQuantitativeValue", null, AuditAction.DELETE,
                "Xoa HSRR dinh luong (dang bang tong hop): " + branchCode + "/" + year + " (" + items.size() + " gia tri)");
    }

    /** Xuat dung dinh dang wide (khop 1-1 voi man hinh va voi mau upload DL_HSRR_Upload: STT | Nam
     * | Chi Nhanh | Ten Chi Nhanh | Ngay | tung chi tieu 1 cot) thay vi dang bang dai truoc day
     * (Ma chi tieu/Ma chi nhanh/Nam/Ngay nhap/Gia tri) - dam bao file xuat ra co the import lai duoc
     * ngay (xem importFromExcel: doc header "Nam"/"Chi Nhanh"/"Ngay" + 1 cot/ma chi tieu). */
    @Transactional(readOnly = true)
    public byte[] exportExcel(Integer year) {
        return excelExportService.export("risk_score_criteria_quantitative_value", wideExportColumns(), wideExportRows(year));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(Integer year) {
        return wordExportService.export("Hồ sơ rủi ro định lượng", wideExportColumns(), wideExportRows(year));
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        String username = TenantContext.getCurrentUser();
        boolean isSuperAdmin = principal != null && principal.roles() != null && principal.roles().contains("SUPER_ADMIN");

        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        Map<UUID, String> criteriaCodesById = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> {
            criteriaIdsByCode.put(c.getCode(), c.getId());
            criteriaCodesById.put(c.getId(), c.getCode());
        });

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new ImportResult(0, 0, List.of());
            }

            Integer yearCol = null;
            Integer branchCol = null;
            Integer dateCol = null;
            Map<Integer, UUID> criteriaColumns = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim();
                if (header.isEmpty() || IGNORED_HEADERS.contains(header)) {
                    continue;
                }
                switch (header) {
                    case "Năm" -> yearCol = cell.getColumnIndex();
                    case "Chi Nhánh" -> branchCol = cell.getColumnIndex();
                    case "Ngày" -> dateCol = cell.getColumnIndex();
                    default -> {
                        UUID criteriaId = criteriaIdsByCode.get(header);
                        if (criteriaId != null) {
                            criteriaColumns.put(cell.getColumnIndex(), criteriaId);
                        }
                    }
                }
            }
            if (yearCol == null || branchCol == null) {
                throw new BusinessException("IMPORT_MISSING_REQUIRED", "File mau phai co cot Nam va Chi Nhanh");
            }

            // Nap truoc toan bo phan quyen cua user hien tai cho cac chi tieu xuat hien trong file -
            // tranh N+1 query khi duyet tung o.
            Set<UUID> fileCriteriaIds = new HashSet<>(criteriaColumns.values());
            Map<UUID, Set<String>> allowedBranchesByCriteria = new HashMap<>();
            for (UUID criteriaId : fileCriteriaIds) {
                if (isSuperAdmin) {
                    // SUPER_ADMIN khong bi rang buoc boi RiskUserAssignment - null = "toan bo chi nhanh"
                    // (xem ensureActorIsAssigneeOrAdmin trong WorkflowTaskService cho cung pattern).
                    allowedBranchesByCriteria.put(criteriaId, null);
                    continue;
                }
                Set<String> branches = new HashSet<>();
                boolean allBranches = false;
                for (RiskUserAssignment a : userAssignmentRepository.findByTenantIdAndUsernameAndCriteriaIdAndActiveTrue(tenantId, username, criteriaId)) {
                    if (a.getBranchCode() == null) {
                        allBranches = true;
                    } else {
                        branches.add(a.getBranchCode());
                    }
                }
                allowedBranchesByCriteria.put(criteriaId, allBranches ? null : branches);
            }

            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                int rowNumber = r + 1;
                try {
                    String branchCode = formatter.formatCellValue(row.getCell(branchCol)).trim();
                    Integer year = parseInt(formatter.formatCellValue(row.getCell(yearCol)).trim());
                    if (branchCode.isEmpty() || year == null) {
                        continue;
                    }
                    if (auditObjectUnitRepository.findByTenantIdAndCode(tenantId, branchCode).isEmpty()) {
                        throw new BusinessException("AUDIT_OBJECT_CODE_NOT_FOUND", "Khong tim thay chi nhanh: " + branchCode);
                    }
                    LocalDate entryDate = dateCol == null ? null : parseDate(row.getCell(dateCol), formatter);

                    // Cot bi bo qua vi khong duoc phan quyen (khac voi o trong/gia tri khong hop le)
                    // phai duoc bao loi ro rang - neu khong, 1 file upload day du nhung nguoi dung
                    // KHONG duoc mapping bat ky chi tieu nao se "thanh cong" voi 0 dong duoc luu,
                    // ma khong co bat ky canh bao nao ve viec bi thieu quyen.
                    List<String> unauthorizedCodes = new ArrayList<>();
                    for (Map.Entry<Integer, UUID> col : criteriaColumns.entrySet()) {
                        Cell cell = row.getCell(col.getKey());
                        if (isBlankCell(cell, formatter)) {
                            continue;
                        }
                        UUID criteriaId = col.getValue();
                        Set<String> allowedBranches = allowedBranchesByCriteria.get(criteriaId);
                        boolean authorized = allowedBranches == null || allowedBranches.contains(branchCode);
                        if (!authorized) {
                            unauthorizedCodes.add(criteriaCodesById.get(criteriaId));
                            continue;
                        }
                        BigDecimal value = parseDecimal(cell, formatter);
                        if (value == null) {
                            continue;
                        }
                        saveValue(tenantId, criteriaId, branchCode, year, entryDate, value);
                        success++;
                    }
                    if (!unauthorizedCodes.isEmpty()) {
                        throw new BusinessException("IMPORT_NOT_AUTHORIZED",
                                "Khong co quyen cap nhat chi tieu " + String.join(", ", unauthorizedCodes)
                                        + " cho chi nhanh " + branchCode);
                    }
                } catch (BusinessException e) {
                    errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        auditLogService.record("RiskCriteriaQuantitativeValue", null, AuditAction.CREATE,
                "Upload HSRR dinh luong: " + success + " gia tri, " + errors.size() + " dong loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void saveValue(UUID tenantId, UUID criteriaId, String branchCode, Integer year, LocalDate entryDate, BigDecimal value) {
        RiskCriteriaQuantitativeValue item = repository
                .findByTenantIdAndCriteriaIdAndBranchCodeAndYear(tenantId, criteriaId, branchCode, year)
                .orElseGet(() -> {
                    RiskCriteriaQuantitativeValue created = new RiskCriteriaQuantitativeValue();
                    created.setTenantId(tenantId);
                    created.setCriteriaId(criteriaId);
                    created.setBranchCode(branchCode);
                    created.setYear(year);
                    return created;
                });
        item.setEntryDate(entryDate);
        item.setValue(value);
        repository.save(item);
    }

    private List<ExportColumn> wideExportColumns() {
        UUID tenantId = TenantContext.getTenantId();
        List<ExportColumn> columns = new ArrayList<>();
        columns.add(new ExportColumn("stt", "STT"));
        columns.add(new ExportColumn("year", "Năm"));
        columns.add(new ExportColumn("branchCode", "Chi Nhánh"));
        columns.add(new ExportColumn("branchName", "Tên Chi Nhánh"));
        columns.add(new ExportColumn("entryDate", "Ngày"));
        List<RiskCriteriaQuantitative> ordered = RiskCriteriaQuantitativeOrder.sortByFsOrder(
                criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId), RiskCriteriaQuantitative::getCode);
        ordered.forEach(c -> columns.add(new ExportColumn(c.getCode(), c.getCode())));
        return columns;
    }

    private List<Map<String, Object>> wideExportRows(Integer year) {
        int stt = 1;
        List<Map<String, Object>> result = new ArrayList<>();
        for (RiskCriteriaQuantitativeWideRowResponse wideRow : listWide(year)) {
            Map<String, Object> row = new HashMap<>();
            row.put("stt", stt++);
            row.put("year", wideRow.year());
            row.put("branchCode", wideRow.branchCode());
            row.put("branchName", wideRow.branchName());
            row.put("entryDate", wideRow.entryDate());
            row.putAll(wideRow.valuesByCriteriaCode());
            result.add(row);
        }
        return result;
    }

    private void applyRequest(RiskCriteriaQuantitativeValue item, RiskCriteriaQuantitativeValueRequest request) {
        item.setCriteriaId(request.criteriaId());
        item.setBranchCode(request.branchCode());
        item.setYear(request.year());
        item.setEntryDate(request.entryDate());
        item.setValue(request.value());
    }

    private void checkNoDuplicate(UUID tenantId, UUID criteriaId, String branchCode, Integer year, UUID excludingId) {
        repository.findByTenantIdAndCriteriaIdAndBranchCodeAndYear(tenantId, criteriaId, branchCode, year)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_CRITERIA_QUANTITATIVE_VALUE_DUPLICATE",
                            "Da ton tai gia tri cho chi nhanh " + branchCode + " nam " + year + " voi chi tieu nay");
                });
    }

    private void validateCriteria(UUID tenantId, UUID criteriaId) {
        criteriaRepository.findById(criteriaId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_DL_NOT_FOUND", "Khong tim thay chi tieu dinh luong"));
    }

    private void validateBranch(UUID tenantId, String branchCode) {
        if (auditObjectUnitRepository.findByTenantIdAndCode(tenantId, branchCode).isEmpty()) {
            throw new BusinessException("AUDIT_OBJECT_CODE_NOT_FOUND", "Khong tim thay chi nhanh: " + branchCode);
        }
    }

    private RiskCriteriaQuantitativeValue getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_QUANTITATIVE_VALUE_NOT_FOUND", "Khong tim thay gia tri HSRR", HttpStatus.NOT_FOUND));
    }

    private LocalDate parseDate(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String raw = formatter.formatCellValue(cell).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            if (raw.length() == 8 && raw.chars().allMatch(Character::isDigit)) {
                return LocalDate.parse(raw, COMPACT_DATE);
            }
            return LocalDate.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** true neu o "khong co gia tri" (bo qua, khong tinh la loi/khong tinh la unauthorized). */
    private boolean isBlankCell(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return false;
        }
        return formatter.formatCellValue(cell).trim().isEmpty();
    }

    /** Doc truc tiep gia tri so cua o NUMERIC thay vi qua DataFormatter (formatCellValue ap dung
     * dinh dang hien thi cua cell - vd dau phan cach hang nghin "1,234" - khien BigDecimal parse
     * that bai va gia tri > ~1000 bi am tham bo qua khi cot duoc dinh dang co dau phan cach). */
    private BigDecimal parseDecimal(Cell cell, DataFormatter formatter) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String raw = formatter.formatCellValue(cell).trim().replace(",", "");
        try {
            return new BigDecimal(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<UUID, RiskCriteriaQuantitative> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaQuantitative> map = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> map.put(c.getId(), c));
        return map;
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }

    private RiskCriteriaQuantitativeValueResponse toResponse(RiskCriteriaQuantitativeValue item,
                                                               Map<UUID, RiskCriteriaQuantitative> criteria,
                                                               Map<String, AuditObjectUnit> units) {
        RiskCriteriaQuantitative criterion = criteria.get(item.getCriteriaId());
        AuditObjectUnit unit = units.get(item.getBranchCode());
        return new RiskCriteriaQuantitativeValueResponse(item.getId(), item.getYear(), item.getBranchCode(),
                unit != null ? unit.getName() : null,
                item.getCriteriaId(), criterion != null ? criterion.getCode() : null, criterion != null ? criterion.getName() : null,
                item.getEntryDate(), item.getValue());
    }
}
