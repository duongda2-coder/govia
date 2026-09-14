package com.govia.audit.khkt.dtkhfile.service;

import com.govia.audit.khkt.dtkhfile.dto.AuditKhktDtkhFileResponse;
import com.govia.audit.khkt.dtkhfile.dto.AuditKhktDtkhFileUpdateRequest;
import com.govia.audit.khkt.dtkhfile.entity.AuditKhktDtkhFile;
import com.govia.audit.khkt.dtkhfile.repository.AuditKhktDtkhFileRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.attachment.Attachment;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** "Báo cáo tham khảo từ các bộ phận cho PKH lập dự thảo" (sheet ZTC_DTKH_FILE) - xem
 * AuditKhktDtkhFile. Cho phep them/sua/xoa day du (khac cac man hinh KHKT khac - dung dac ta
 * "cho phep thêm, sửa, xóa điều chỉnh bản ghi"). */
@Service
public class AuditKhktDtkhFileService {

    private final AuditKhktDtkhFileRepository repository;
    private final AttachmentService attachmentService;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    public AuditKhktDtkhFileService(AuditKhktDtkhFileRepository repository, AttachmentService attachmentService,
                                     AuditMasterDataItemRepository masterDataItemRepository, UserAccountRepository userAccountRepository,
                                     EmployeeRepository employeeRepository, AuditLogService auditLogService) {
        this.repository = repository;
        this.attachmentService = attachmentService;
        this.masterDataItemRepository = masterDataItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhktDtkhFileResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktDtkhFile> rows = repository.findByTenantIdAndYearOrderByCreatedAtDesc(tenantId, year);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> versions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.VERSION);
        return rows.stream().map(row -> toResponse(row, departments, versions)).toList();
    }

    @Transactional
    public AuditKhktDtkhFileResponse create(Integer year, UUID departmentId, UUID versionId, String note, MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("AUDIT_KHKT_DTKH_FILE_MISSING_FILE", "Vui long chon file dinh kem");
        }
        AuditKhktDtkhFile item = new AuditKhktDtkhFile();
        item.setTenantId(tenantId);
        item.setYear(year);
        item.setDepartmentId(departmentId);
        item.setVersionId(versionId);
        item.setNote(note);
        item = repository.save(item);
        attachmentService.upload(AuditKhktDtkhFile.ATTACHMENT_ENTITY_NAME, item.getId(), file);

        auditLogService.record("AuditKhktDtkhFile", item.getId(), AuditAction.CREATE,
                "Upload bao cao tham khao KHKT nam " + year + ": " + file.getOriginalFilename());
        return toResponse(item, masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT),
                masterDataItemsByCategory(tenantId, AuditMasterDataCategory.VERSION));
    }

    @Transactional
    public AuditKhktDtkhFileResponse update(UUID id, AuditKhktDtkhFileUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktDtkhFile item = getOwnedOrThrow(tenantId, id);
        item.setDepartmentId(request.departmentId());
        item.setVersionId(request.versionId());
        item.setNote(request.note());
        item = repository.save(item);

        auditLogService.record("AuditKhktDtkhFile", item.getId(), AuditAction.UPDATE, "Cap nhat bao cao tham khao KHKT");
        return toResponse(item, masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT),
                masterDataItemsByCategory(tenantId, AuditMasterDataCategory.VERSION));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktDtkhFile item = getOwnedOrThrow(tenantId, id);
        attachmentService.listByEntity(AuditKhktDtkhFile.ATTACHMENT_ENTITY_NAME, item.getId())
                .forEach(a -> attachmentService.delete(a.getId()));
        repository.delete(item);
        auditLogService.record("AuditKhktDtkhFile", id, AuditAction.DELETE, "Xoa bao cao tham khao KHKT");
    }

    private AuditKhktDtkhFile getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_DTKH_FILE_NOT_FOUND", "Khong tim thay bao cao", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private AuditKhktDtkhFileResponse toResponse(AuditKhktDtkhFile row, Map<UUID, AuditMasterDataItem> departments,
                                                  Map<UUID, AuditMasterDataItem> versions) {
        AuditMasterDataItem department = departments.get(row.getDepartmentId());
        AuditMasterDataItem version = row.getVersionId() == null ? null : versions.get(row.getVersionId());
        Attachment attachment = attachmentService.listByEntity(AuditKhktDtkhFile.ATTACHMENT_ENTITY_NAME, row.getId())
                .stream().findFirst().orElse(null);
        String uploaderName = resolveUploaderName(row.getTenantId(), row.getCreatedBy());
        return new AuditKhktDtkhFileResponse(row.getId(), row.getYear(), department == null ? null : department.getCode(),
                version == null ? null : version.getCode(), row.getCreatedBy(), uploaderName,
                attachment == null ? null : attachment.getId(), attachment == null ? null : attachment.getFileName(),
                attachment == null ? null : attachment.getCreatedAt(), row.getNote());
    }

    private String resolveUploaderName(UUID tenantId, String username) {
        if (username == null) {
            return null;
        }
        Optional<UserAccount> account = userAccountRepository.findByTenantIdAndUsername(tenantId, username);
        return account.flatMap(a -> a.getEmployeeId() == null ? Optional.empty() : employeeRepository.findById(a.getEmployeeId()))
                .map(Employee::getFullName).orElse(null);
    }
}
