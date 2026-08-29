package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProcess;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProject;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectSubsidiary;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProcessRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProjectRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectSubsidiaryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Diem tra cuu DUY NHAT tu 1 AuditObjectCategory ("Loai doi tuong kiem toan") sang dung 1 trong 4
 * danh muc "Doi tuong kiem toan" cu the (Unit/Subsidiary/Process/Project), dua tren
 * category.getObjectSource() thay vi hard-code theo ma code (HO/CTC/KTQT) nhu truoc - moi noi dung
 * "Ma doi tuong KT" (Cham diem rui ro khac, Bang xep hang, Xep hang theo YKCG...) deu goi qua day de
 * tranh lech logic giua cac noi.
 */
@Service
public class AuditObjectResolverService {

    private final AuditObjectUnitRepository unitRepository;
    private final AuditObjectSubsidiaryRepository subsidiaryRepository;
    private final AuditObjectProcessRepository processRepository;
    private final AuditObjectProjectRepository projectRepository;

    public AuditObjectResolverService(AuditObjectUnitRepository unitRepository,
                                       AuditObjectSubsidiaryRepository subsidiaryRepository,
                                       AuditObjectProcessRepository processRepository,
                                       AuditObjectProjectRepository projectRepository) {
        this.unitRepository = unitRepository;
        this.subsidiaryRepository = subsidiaryRepository;
        this.processRepository = processRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID tenantId, AuditObjectCategory category, String code) {
        return switch (category.getObjectSource()) {
            case UNIT -> unitRepository.findByTenantIdAndCode(tenantId, code).isPresent();
            case SUBSIDIARY -> subsidiaryRepository.findByTenantIdAndCode(tenantId, code).isPresent();
            case PROCESS -> processRepository.findByTenantIdAndCode(tenantId, code).isPresent();
            case PROJECT -> projectRepository.findByTenantIdAndCode(tenantId, code).isPresent();
        };
    }

    @Transactional(readOnly = true)
    public String resolveName(UUID tenantId, AuditObjectCategory category, String code) {
        if (category == null || code == null) {
            return null;
        }
        return switch (category.getObjectSource()) {
            case UNIT -> unitRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectUnit::getName).orElse(null);
            case SUBSIDIARY -> subsidiaryRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectSubsidiary::getName).orElse(null);
            case PROCESS -> processRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectProcess::getName).orElse(null);
            case PROJECT -> projectRepository.findByTenantIdAndCode(tenantId, code).map(AuditObjectProject::getName).orElse(null);
        };
    }
}
