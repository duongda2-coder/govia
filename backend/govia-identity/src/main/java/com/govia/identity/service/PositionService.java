package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.PositionRequest;
import com.govia.identity.dto.PositionResponse;
import com.govia.identity.entity.Position;
import com.govia.identity.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD chuc danh (master-data) - cung mo hinh voi OrganizationUnitService nhung don gian hon (khong co cay). */
@Service
public class PositionService {

    private final PositionRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public PositionService(PositionRepository repository,
                            AuditLogService auditLogService,
                            ExcelExportService excelExportService,
                            WordExportService wordExportService,
                            ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return repository.findByTenantId(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PositionResponse getById(UUID id) {
        return toResponse(getOwnedOrThrow(TenantContext.getTenantId(), id));
    }

    @Transactional
    public PositionResponse create(PositionRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        repository.findByTenantIdAndCode(tenantId, request.code()).ifPresent(p -> {
            throw new BusinessException("POSITION_CODE_DUPLICATE", "Ma chuc danh da ton tai: " + request.code());
        });

        Position position = new Position();
        position.setTenantId(tenantId);
        position.setCode(request.code());
        position.setName(request.name());
        position.setActive(true);
        position = repository.save(position);

        auditLogService.record("Position", position.getId(), AuditAction.CREATE, "Tao chuc danh " + position.getCode());
        return toResponse(position);
    }

    @Transactional
    public PositionResponse update(UUID id, PositionRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Position position = getOwnedOrThrow(tenantId, id);

        repository.findByTenantIdAndCode(tenantId, request.code())
                .filter(p -> !p.getId().equals(id))
                .ifPresent(p -> {
                    throw new BusinessException("POSITION_CODE_DUPLICATE", "Ma chuc danh da ton tai: " + request.code());
                });

        position.setCode(request.code());
        position.setName(request.name());
        position = repository.save(position);

        auditLogService.record("Position", position.getId(), AuditAction.UPDATE, "Cap nhat chuc danh " + position.getCode());
        return toResponse(position);
    }

    @Transactional
    public PositionResponse setActive(UUID id, boolean active) {
        UUID tenantId = TenantContext.getTenantId();
        Position position = getOwnedOrThrow(tenantId, id);
        position.setActive(active);
        position = repository.save(position);

        auditLogService.record("Position", position.getId(),
                active ? AuditAction.UPDATE : AuditAction.DELETE,
                (active ? "Kich hoat" : "Vo hieu hoa") + " chuc danh " + position.getCode());
        return toResponse(position);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("Positions", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sach chuc danh", exportColumns(), exportRows());
    }

    /** Import Excel theo DUNG mau da xuat (exportColumns) - tao moi tung dong, dong loi (trung ma, thieu du lieu) duoc bao rieng, khong lam hong ca file. */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma hoac Ten chuc danh");
                }
                create(new PositionRequest(code.trim(), name.trim()));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("Position", null, AuditAction.CREATE,
                "Import Excel chuc danh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma"),
                new ExportColumn("name", "Ten chuc danh"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantId(TenantContext.getTenantId()).stream().map(p -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", p.getCode());
            row.put("name", p.getName());
            return row;
        }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Position getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("POSITION_NOT_FOUND", "Khong tim thay chuc danh", HttpStatus.NOT_FOUND));
    }

    private PositionResponse toResponse(Position position) {
        return new PositionResponse(position.getId(), position.getCode(), position.getName(), position.isActive());
    }
}
