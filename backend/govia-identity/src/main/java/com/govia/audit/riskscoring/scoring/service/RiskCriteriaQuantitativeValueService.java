package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskUserAssignment;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskUserAssignmentRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQuantitativeValue;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaQuantitativeValueRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ImportResult;
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

    public RiskCriteriaQuantitativeValueService(RiskCriteriaQuantitativeValueRepository repository,
                                                 RiskCriteriaQuantitativeRepository criteriaRepository,
                                                 AuditObjectUnitRepository auditObjectUnitRepository,
                                                 RiskUserAssignmentRepository userAssignmentRepository,
                                                 AuditLogService auditLogService) {
        this.repository = repository;
        this.criteriaRepository = criteriaRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.userAssignmentRepository = userAssignmentRepository;
        this.auditLogService = auditLogService;
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

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        String username = TenantContext.getCurrentUser();

        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> criteriaIdsByCode.put(c.getCode(), c.getId()));

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
                Set<String> branches = new HashSet<>();
                boolean allBranches = false;
                for (RiskUserAssignment a : userAssignmentRepository.findByTenantIdAndUsernameAndCriteriaId(tenantId, username, criteriaId)) {
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

                    for (Map.Entry<Integer, UUID> col : criteriaColumns.entrySet()) {
                        Cell cell = row.getCell(col.getKey());
                        String raw = cell == null ? "" : formatter.formatCellValue(cell).trim();
                        if (raw.isEmpty()) {
                            continue;
                        }
                        UUID criteriaId = col.getValue();
                        Set<String> allowedBranches = allowedBranchesByCriteria.get(criteriaId);
                        boolean authorized = allowedBranches == null || allowedBranches.contains(branchCode);
                        if (!authorized) {
                            continue;
                        }
                        BigDecimal value = parseDecimal(raw);
                        if (value == null) {
                            continue;
                        }
                        saveValue(tenantId, criteriaId, branchCode, year, entryDate, value);
                        success++;
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

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
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
