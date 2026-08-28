package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.scoring.dto.RiskTypeHORequest;
import com.govia.audit.riskscoring.scoring.dto.RiskTypeHOResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskGroupHO;
import com.govia.audit.riskscoring.scoring.entity.RiskTypeHO;
import com.govia.audit.riskscoring.scoring.repository.RiskGroupHORepository;
import com.govia.audit.riskscoring.scoring.repository.RiskTypeHORepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD + Import/Export cho danh muc "Loai rui ro HO" (sheet ZTC_RR_HO), con cua RiskGroupHO. */
@Service
public class RiskTypeHOService {

    private final RiskTypeHORepository repository;
    private final RiskGroupHORepository groupHoRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public RiskTypeHOService(RiskTypeHORepository repository, RiskGroupHORepository groupHoRepository,
                              AuditLogService auditLogService, ExcelExportService excelExportService,
                              WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.groupHoRepository = groupHoRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<RiskTypeHOResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskGroupHO> groups = groupsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, groups)).toList();
    }

    @Transactional
    public RiskTypeHOResponse create(RiskTypeHORequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.groupHoId(), request.code(), null);
        validateGroupHo(tenantId, request.groupHoId());

        RiskTypeHO item = new RiskTypeHO();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskTypeHO", item.getId(), AuditAction.CREATE, "Tao loai rui ro HO: " + item.getCode());
        return toResponse(item, groupsById(tenantId));
    }

    @Transactional
    public RiskTypeHOResponse update(UUID id, RiskTypeHORequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskTypeHO item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.groupHoId(), request.code(), id);
        validateGroupHo(tenantId, request.groupHoId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskTypeHO", item.getId(), AuditAction.UPDATE, "Cap nhat loai rui ro HO: " + item.getCode());
        return toResponse(item, groupsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskTypeHO item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskTypeHO", id, AuditAction.DELETE, "Xoa loai rui ro HO: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_type_ho", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Loại rủi ro HO", exportColumns(), exportRows());
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        Map<String, UUID> groupIdsByCode = new HashMap<>();
        groupHoRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> groupIdsByCode.put(g.getCode(), g.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String groupHoCode = row.get("groupHoCode");
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(groupHoCode) || isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma nhom, Ma hoac Ten loai rui ro");
                }
                UUID groupHoId = groupIdsByCode.get(groupHoCode.trim());
                if (groupHoId == null) {
                    throw new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO: " + groupHoCode);
                }
                create(new RiskTypeHORequest(groupHoId, code.trim(), name.trim(), parseDecimal(row.get("weight")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskTypeHO", null, AuditAction.CREATE,
                "Import Excel loai rui ro HO: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskTypeHO item, RiskTypeHORequest request) {
        item.setGroupHoId(request.groupHoId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWeight(request.weight());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, UUID groupHoId, String code, UUID excludingId) {
        repository.findByTenantIdAndGroupHoIdAndCode(tenantId, groupHoId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_TYPE_HO_CODE_DUPLICATE", "Ma loai rui ro da ton tai: " + code);
                });
    }

    private void validateGroupHo(UUID tenantId, UUID groupHoId) {
        groupHoRepository.findById(groupHoId)
                .filter(g -> g.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO"));
    }

    private RiskTypeHO getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_TYPE_HO_NOT_FOUND", "Khong tim thay loai rui ro HO", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, RiskGroupHO> groupsById(UUID tenantId) {
        Map<UUID, RiskGroupHO> map = new HashMap<>();
        for (RiskGroupHO g : groupHoRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(g.getId(), g);
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("groupHoCode", "Ma nhom"),
                new ExportColumn("code", "Ma loai rui ro"),
                new ExportColumn("name", "Ten loai rui ro"),
                new ExportColumn("weight", "Ti trong"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskGroupHO> groups = groupsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    RiskGroupHO group = groups.get(item.getGroupHoId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("groupHoCode", group != null ? group.getCode() : null);
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("weight", item.getWeight());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RiskTypeHOResponse toResponse(RiskTypeHO item, Map<UUID, RiskGroupHO> groups) {
        RiskGroupHO group = groups.get(item.getGroupHoId());
        return new RiskTypeHOResponse(item.getId(), item.getGroupHoId(),
                group != null ? group.getCode() : null, group != null ? group.getName() : null,
                item.getCode(), item.getName(), item.getWeight(), item.isActive());
    }
}
