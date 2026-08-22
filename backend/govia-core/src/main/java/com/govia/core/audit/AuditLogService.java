package com.govia.core.audit;

import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * DLL ghi audit log dung chung - service nghiep vu chi can goi 1 dong
 * thay vi tu insert vao bang audit_log.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String entityName, UUID entityId, AuditAction action, String detail) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetail(detail);
        repository.save(log);
    }
}
