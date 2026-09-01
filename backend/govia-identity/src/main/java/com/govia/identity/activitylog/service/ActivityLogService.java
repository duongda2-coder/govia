package com.govia.identity.activitylog.service;

import com.govia.core.audit.AuditLog;
import com.govia.core.audit.AuditLogRepository;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.activitylog.dto.ActivityLogFilter;
import com.govia.identity.activitylog.dto.ActivityLogResponse;
import com.govia.identity.activitylog.spec.ActivityLogSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Man hinh "Nhat ky thao tac" (Admin) - CHI DOC bang audit_log dung chung toan platform, ghi nhan
 * moi CREATE/UPDATE/DELETE tren bat ky man hinh nao (xem AuditLogService o govia-core - noi ghi
 * log, con class nay chi phuc vu doc lai/loc/xuat). Khong co create/update/delete/import vi day la
 * nhat ky he thong, khong phai du lieu nghiep vu do NSD nhap. */
@Service
public class ActivityLogService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AuditLogRepository repository;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;

    public ActivityLogService(AuditLogRepository repository, ExcelExportService excelExportService, WordExportService wordExportService) {
        this.repository = repository;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> list(ActivityLogFilter filter, Pageable pageable) {
        return repository.findAll(buildSpec(filter), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(ActivityLogFilter filter) {
        List<AuditLog> logs = repository.findAll(buildSpec(filter));
        return excelExportService.export("activity_log", exportColumns(), exportRows(logs));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(ActivityLogFilter filter) {
        List<AuditLog> logs = repository.findAll(buildSpec(filter));
        return wordExportService.export("Nhật ký thao tác", exportColumns(), exportRows(logs));
    }

    private Specification<AuditLog> buildSpec(ActivityLogFilter filter) {
        return Specification.where(ActivityLogSpecifications.tenantId(TenantContext.getTenantId()))
                .and(ActivityLogSpecifications.entityNameContains(filter.entityName()))
                .and(ActivityLogSpecifications.action(filter.action()))
                .and(ActivityLogSpecifications.performedByContains(filter.performedBy()))
                .and(ActivityLogSpecifications.entityId(filter.entityId()))
                .and(ActivityLogSpecifications.dateBetween(filter.dateFrom(), filter.dateTo()))
                .and(ActivityLogSpecifications.keyword(filter.keyword()));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("performedAt", "Thời điểm"),
                new ExportColumn("performedBy", "Người thực hiện"),
                new ExportColumn("action", "Hành động"),
                new ExportColumn("entityName", "Đối tượng"),
                new ExportColumn("entityId", "Mã đối tượng"),
                new ExportColumn("detail", "Chi tiết"));
    }

    private List<Map<String, Object>> exportRows(List<AuditLog> logs) {
        return logs.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).map(log -> {
            Map<String, Object> row = new HashMap<>();
            row.put("performedAt", log.getCreatedAt() == null ? null : DATETIME_FORMAT.format(log.getCreatedAt()));
            row.put("performedBy", log.getCreatedBy());
            row.put("action", log.getAction());
            row.put("entityName", log.getEntityName());
            row.put("entityId", log.getEntityId());
            row.put("detail", log.getDetail());
            return row;
        }).toList();
    }

    private ActivityLogResponse toResponse(AuditLog log) {
        return new ActivityLogResponse(log.getId(), log.getEntityName(), log.getEntityId(), log.getAction(),
                log.getDetail(), log.getCreatedBy(), log.getCreatedAt());
    }
}
