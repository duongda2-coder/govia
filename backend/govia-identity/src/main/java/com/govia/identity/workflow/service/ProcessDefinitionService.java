package com.govia.identity.workflow.service;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.workflow.dto.ProcessDefinitionSummary;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProcessDefinitionService {

    private final RepositoryService repositoryService;

    public ProcessDefinitionService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /** Chi phien ban MOI NHAT cua moi process key - dung cho man hinh danh sach/start instance. */
    public List<ProcessDefinitionSummary> list() {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(currentTenantId())
                .latestVersion()
                .orderByProcessDefinitionKey().asc()
                .list()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /** Toan bo phien ban (versioning) da tung deploy cho 1 process key, moi nhat truoc. */
    public List<ProcessDefinitionSummary> listVersions(String key) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .processDefinitionTenantId(currentTenantId())
                .orderByProcessDefinitionVersion().desc()
                .list()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public ProcessDefinitionSummary deploy(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn"))) {
            throw new BusinessException("WORKFLOW_INVALID_BPMN_FILE", "File phai co duoi .bpmn20.xml hoac .bpmn");
        }
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .tenantId(currentTenantId())
                    .name(fileName)
                    .addInputStream(fileName, file.getInputStream())
                    .deploy();
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            return toSummary(definition);
        } catch (IOException e) {
            throw new BusinessException("WORKFLOW_DEPLOY_FAILED", "Khong doc duoc file BPMN: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /** Tam ngung 1 phien ban dinh nghia - khong the start instance moi tu phien ban nay nua (cac
     * instance dang chay cua no van tiep tuc, khong bi dung). */
    public void suspend(String definitionId) {
        getOwnedOrThrow(definitionId);
        repositoryService.suspendProcessDefinitionById(definitionId);
    }

    public void activate(String definitionId) {
        getOwnedOrThrow(definitionId);
        repositoryService.activateProcessDefinitionById(definitionId);
    }

    private ProcessDefinition getOwnedOrThrow(String definitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .processDefinitionTenantId(currentTenantId())
                .singleResult();
        if (definition == null) {
            throw new BusinessException("WORKFLOW_DEFINITION_NOT_FOUND", "Khong tim thay dinh nghia quy trinh",
                    HttpStatus.NOT_FOUND);
        }
        return definition;
    }

    private ProcessDefinitionSummary toSummary(ProcessDefinition definition) {
        return new ProcessDefinitionSummary(definition.getId(), definition.getKey(), definition.getName(),
                definition.getVersion(), definition.isSuspended());
    }

    private String currentTenantId() {
        return TenantContext.getTenantId().toString();
    }
}
