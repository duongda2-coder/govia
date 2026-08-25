package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProcess;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProject;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectSubsidiary;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectType;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProcessRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProjectRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectSubsidiaryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.web.BusinessException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Validate/resolve tham chieu "Doi tuong kiem toan" (AuditObjectType + UUID) dung chung cho
 * Group1/CriteriaQualitative/CriteriaQuantitative - thay the enum "Loai doi tuong" cu (CNDT/CNDL/
 * HO/CNTT/DA fix cung) bang tham chieu that toi 1 ban ghi cu the trong 4 danh muc ZTC_DTKT1-4.
 */
@Service
public class AuditObjectReferenceService {

    private final AuditObjectUnitRepository unitRepository;
    private final AuditObjectSubsidiaryRepository subsidiaryRepository;
    private final AuditObjectProjectRepository projectRepository;
    private final AuditObjectProcessRepository processRepository;

    public AuditObjectReferenceService(AuditObjectUnitRepository unitRepository,
                                        AuditObjectSubsidiaryRepository subsidiaryRepository,
                                        AuditObjectProjectRepository projectRepository,
                                        AuditObjectProcessRepository processRepository) {
        this.unitRepository = unitRepository;
        this.subsidiaryRepository = subsidiaryRepository;
        this.projectRepository = projectRepository;
        this.processRepository = processRepository;
    }

    public record Ref(String code, String name) {
    }

    public void validateExists(UUID tenantId, AuditObjectType type, UUID id) {
        boolean exists = switch (type) {
            case UNIT -> unitRepository.findById(id).filter(u -> u.getTenantId().equals(tenantId)).isPresent();
            case SUBSIDIARY -> subsidiaryRepository.findById(id).filter(s -> s.getTenantId().equals(tenantId)).isPresent();
            case PROJECT -> projectRepository.findById(id).filter(p -> p.getTenantId().equals(tenantId)).isPresent();
            case PROCESS -> processRepository.findById(id).filter(p -> p.getTenantId().equals(tenantId)).isPresent();
        };
        if (!exists) {
            throw new BusinessException("AUDIT_OBJECT_REFERENCE_NOT_FOUND", "Khong tim thay doi tuong kiem toan da chon");
        }
    }

    /** Nap toan bo 4 danh muc thanh map (id -> Ref) de resolve hang loat khi list()/export. */
    public Map<AuditObjectType, Map<UUID, Ref>> loadAllRefs(UUID tenantId) {
        Map<AuditObjectType, Map<UUID, Ref>> all = new EnumMap<>(AuditObjectType.class);

        Map<UUID, Ref> units = new HashMap<>();
        unitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> units.put(u.getId(), new Ref(u.getCode(), u.getName())));
        all.put(AuditObjectType.UNIT, units);

        Map<UUID, Ref> subsidiaries = new HashMap<>();
        subsidiaryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(s -> subsidiaries.put(s.getId(), new Ref(s.getCode(), s.getName())));
        all.put(AuditObjectType.SUBSIDIARY, subsidiaries);

        Map<UUID, Ref> projects = new HashMap<>();
        projectRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(p -> projects.put(p.getId(), new Ref(p.getCode(), p.getName())));
        all.put(AuditObjectType.PROJECT, projects);

        Map<UUID, Ref> processes = new HashMap<>();
        processRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(p -> processes.put(p.getId(), new Ref(p.getCode(), p.getName())));
        all.put(AuditObjectType.PROCESS, processes);

        return all;
    }

    public Ref lookup(Map<AuditObjectType, Map<UUID, Ref>> allRefs, AuditObjectType type, UUID id) {
        if (id == null) {
            return null;
        }
        Map<UUID, Ref> map = allRefs.get(type);
        return map == null ? null : map.get(id);
    }

    /** Tim UUID theo (type, code) - dung cho import Excel (nguoi dung nhap ma, khong nhap UUID). */
    public UUID resolveIdByCode(UUID tenantId, AuditObjectType type, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (type) {
            case UNIT -> unitRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectUnit::getId).orElse(null);
            case SUBSIDIARY -> subsidiaryRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectSubsidiary::getId).orElse(null);
            case PROJECT -> projectRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectProject::getId).orElse(null);
            case PROCESS -> processRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectProcess::getId).orElse(null);
        };
    }
}
