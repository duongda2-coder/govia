package com.govia.audit.tdkp.report;

import com.govia.core.attachment.Attachment;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Danh sách báo cáo TDKP đã xuất và upload lưu trữ. */
@Service
public class AuditTdkpReportArchiveService {

    private final AuditTdkpReportArchiveRepository repository;
    private final AttachmentService attachmentService;
    private final AuditLogService auditLogService;

    public AuditTdkpReportArchiveService(AuditTdkpReportArchiveRepository repository, AttachmentService attachmentService, AuditLogService auditLogService) {
        this.repository = repository;
        this.attachmentService = attachmentService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpReportDto.ArchiveResponse> list() {
        List<AuditTdkpReportArchive> items = repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
        Map<UUID, Long> counts = attachmentService.countByEntity(AuditTdkpReportArchive.ATTACHMENT_ENTITY, items.stream().map(AuditTdkpReportArchive::getId).toList());
        return items.stream().map(i -> toResponse(i, counts.getOrDefault(i.getId(), 0L))).toList();
    }

    @Transactional
    public AuditTdkpReportDto.ArchiveResponse create(AuditTdkpReportDto.ArchiveRequest request) {
        AuditTdkpReportArchive item = new AuditTdkpReportArchive();
        item.setTenantId(TenantContext.getTenantId());
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpReportArchive", item.getId(), AuditAction.CREATE, "Tao bao cao TDKP luu tru: " + item.getTitle());
        return toResponse(item, 0L);
    }

    @Transactional
    public AuditTdkpReportDto.ArchiveResponse update(UUID id, AuditTdkpReportDto.ArchiveRequest request) {
        AuditTdkpReportArchive item = getOwnedOrThrow(id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpReportArchive", item.getId(), AuditAction.UPDATE, "Cap nhat bao cao TDKP luu tru: " + item.getTitle());
        return toResponse(item, attachmentService.listByEntity(AuditTdkpReportArchive.ATTACHMENT_ENTITY, id).size());
    }

    /** Xóa báo cáo lưu trữ kèm các file đính kèm của nó. */
    @Transactional
    public void delete(UUID id) {
        AuditTdkpReportArchive item = getOwnedOrThrow(id);
        for (Attachment attachment : attachmentService.listByEntity(AuditTdkpReportArchive.ATTACHMENT_ENTITY, id)) {
            attachmentService.delete(attachment.getId());
        }
        repository.delete(item);
        auditLogService.record("AuditTdkpReportArchive", id, AuditAction.DELETE, "Xoa bao cao TDKP luu tru: " + item.getTitle());
    }

    private void apply(AuditTdkpReportArchive item, AuditTdkpReportDto.ArchiveRequest request) {
        item.setReportType(request.reportType());
        item.setTitle(request.title().trim());
        item.setAsOfDate(request.asOfDate());
        item.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
    }

    private AuditTdkpReportArchive getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_REPORT_ARCHIVE_NOT_FOUND", "Khong tim thay bao cao luu tru", HttpStatus.NOT_FOUND));
    }

    private AuditTdkpReportDto.ArchiveResponse toResponse(AuditTdkpReportArchive item, long attachmentCount) {
        return new AuditTdkpReportDto.ArchiveResponse(item.getId(), item.getReportType(), item.getReportType().title(), item.getTitle(), item.getAsOfDate(),
                item.getNote(), item.getCreatedBy(), item.getCreatedAt(), attachmentCount);
    }
}
