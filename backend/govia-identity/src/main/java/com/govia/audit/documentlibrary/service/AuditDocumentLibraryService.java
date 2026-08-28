package com.govia.audit.documentlibrary.service;

import com.govia.audit.documentlibrary.dto.AuditDocumentLibraryRequest;
import com.govia.audit.documentlibrary.dto.AuditDocumentLibraryResponse;
import com.govia.audit.documentlibrary.entity.AuditDocumentLibrary;
import com.govia.audit.documentlibrary.repository.AuditDocumentLibraryRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD + Import/Export cho danh muc "Thu vien tai lieu" (sheet ZTC_TVTL). "Nguoi ban hanh" link
 * sang danh muc Chuc vu (AuditMasterDataItem, category=POSITION) da co san thay vi nhap tay tu do.
 */
@Service
public class AuditDocumentLibraryService {

    private final AuditDocumentLibraryRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditDocumentLibraryService(AuditDocumentLibraryRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                        AuditLogService auditLogService, ExcelExportService excelExportService,
                                        WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditDocumentLibraryResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> positionNames = positionNamesById(tenantId);
        return repository.findByTenantIdOrderByDocumentNumberAsc(tenantId).stream()
                .map(item -> toResponse(item, positionNames)).toList();
    }

    @Transactional
    public AuditDocumentLibraryResponse create(AuditDocumentLibraryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateNumber(tenantId, request.documentNumber(), null);
        validateIssuerPosition(tenantId, request.issuerPositionId());

        AuditDocumentLibrary item = new AuditDocumentLibrary();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditDocumentLibrary", item.getId(), AuditAction.CREATE, "Tao van ban tai lieu: " + item.getDocumentNumber());
        return toResponse(item, positionNamesById(tenantId));
    }

    @Transactional
    public AuditDocumentLibraryResponse update(UUID id, AuditDocumentLibraryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditDocumentLibrary item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateNumber(tenantId, request.documentNumber(), id);
        validateIssuerPosition(tenantId, request.issuerPositionId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditDocumentLibrary", item.getId(), AuditAction.UPDATE, "Cap nhat van ban tai lieu: " + item.getDocumentNumber());
        return toResponse(item, positionNamesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditDocumentLibrary item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditDocumentLibrary", id, AuditAction.DELETE, "Xoa van ban tai lieu: " + item.getDocumentNumber());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_document_library", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Thư viện tài liệu", exportColumns(), exportRows());
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
        Map<String, UUID> positionIdsByName = new HashMap<>();
        masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.POSITION)
                .forEach(p -> positionIdsByName.put(p.getName(), p.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String documentNumber = row.get("documentNumber");
                String documentName = row.get("documentName");
                if (isBlank(documentNumber) || isBlank(documentName)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu So van ban hoac Ten van ban");
                }
                String issuerPositionName = row.get("issuerPositionName");
                UUID issuerPositionId = isBlank(issuerPositionName) ? null : positionIdsByName.get(issuerPositionName.trim());
                create(new AuditDocumentLibraryRequest(documentNumber.trim(), documentName.trim(),
                        parseDate(row.get("issueDate")), parseDate(row.get("effectiveDate")), issuerPositionId,
                        emptyToNull(row.get("businessActivity")), emptyToNull(row.get("topic")),
                        emptyToNull(row.get("replacedDocument")), emptyToNull(row.get("amendedDocument")),
                        emptyToNull(row.get("legalBasis")), parseBoolean(row.get("expired")),
                        parseDate(row.get("expiryDate")), emptyToNull(row.get("content"))));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditDocumentLibrary", null, AuditAction.CREATE,
                "Import Excel thu vien tai lieu: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditDocumentLibrary item, AuditDocumentLibraryRequest request) {
        item.setDocumentNumber(request.documentNumber());
        item.setDocumentName(request.documentName());
        item.setIssueDate(request.issueDate());
        item.setEffectiveDate(request.effectiveDate());
        item.setIssuerPositionId(request.issuerPositionId());
        item.setBusinessActivity(request.businessActivity());
        item.setTopic(request.topic());
        item.setReplacedDocument(request.replacedDocument());
        item.setAmendedDocument(request.amendedDocument());
        item.setLegalBasis(request.legalBasis());
        item.setExpired(request.expired());
        item.setExpiryDate(request.expiryDate());
        item.setContent(request.content());
    }

    private void checkNoDuplicateNumber(UUID tenantId, String documentNumber, UUID excludingId) {
        repository.findByTenantIdAndDocumentNumber(tenantId, documentNumber)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_DOCUMENT_LIBRARY_NUMBER_DUPLICATE", "So van ban da ton tai: " + documentNumber);
                });
    }

    private void validateIssuerPosition(UUID tenantId, UUID issuerPositionId) {
        if (issuerPositionId == null) {
            return;
        }
        masterDataItemRepository.findById(issuerPositionId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.POSITION)
                .orElseThrow(() -> new BusinessException("POSITION_NOT_FOUND", "Khong tim thay chuc vu nguoi ban hanh"));
    }

    private AuditDocumentLibrary getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_DOCUMENT_LIBRARY_NOT_FOUND", "Khong tim thay van ban tai lieu", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> positionNamesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        for (AuditMasterDataItem p : masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.POSITION)) {
            map.put(p.getId(), p.getName());
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("documentNumber", "Số văn bản"),
                new ExportColumn("documentName", "Tên văn bản"),
                new ExportColumn("issueDate", "Ngày ban hành VB"),
                new ExportColumn("effectiveDate", "Ngày hiệu lực VB"),
                new ExportColumn("issuerPositionName", "Người ban hành"),
                new ExportColumn("businessActivity", "Hoạt động nghiệp vụ"),
                new ExportColumn("topic", "Chi tiết nghiệp vụ"),
                new ExportColumn("replacedDocument", "Văn bản đã thay thế"),
                new ExportColumn("amendedDocument", "Văn bản được sửa đổi bổ sung"),
                new ExportColumn("legalBasis", "Căn cứ ban hành VB"),
                new ExportColumn("expired", "Hết hiệu lực"),
                new ExportColumn("expiryDate", "Ngày hết hiệu lực"),
                new ExportColumn("content", "Nội dung chính VB"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> positionNames = positionNamesById(tenantId);
        return repository.findByTenantIdOrderByDocumentNumberAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("documentNumber", item.getDocumentNumber());
                    row.put("documentName", item.getDocumentName());
                    row.put("issueDate", item.getIssueDate());
                    row.put("effectiveDate", item.getEffectiveDate());
                    row.put("issuerPositionName", positionNames.get(item.getIssuerPositionId()));
                    row.put("businessActivity", item.getBusinessActivity());
                    row.put("topic", item.getTopic());
                    row.put("replacedDocument", item.getReplacedDocument());
                    row.put("amendedDocument", item.getAmendedDocument());
                    row.put("legalBasis", item.getLegalBasis());
                    row.put("expired", item.isExpired());
                    row.put("expiryDate", item.getExpiryDate());
                    row.put("content", item.getContent());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    private AuditDocumentLibraryResponse toResponse(AuditDocumentLibrary item, Map<UUID, String> positionNames) {
        return new AuditDocumentLibraryResponse(item.getId(), item.getDocumentNumber(), item.getDocumentName(),
                item.getIssueDate(), item.getEffectiveDate(), item.getIssuerPositionId(),
                positionNames.get(item.getIssuerPositionId()), item.getBusinessActivity(), item.getTopic(),
                item.getReplacedDocument(), item.getAmendedDocument(), item.getLegalBasis(), item.isExpired(),
                item.getExpiryDate(), item.getContent());
    }
}
