package com.govia.identity.workflow.config;

import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import org.flowable.engine.RepositoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Trien khai cac quy trinh BPMN rieng cho module Audit (tach khoi WorkflowSampleProcessSeeder - quy
 * trinh "mau/dung chung" - de moi module co the them quy trinh cua minh ma khong phai sua 1 seeder
 * dung chung ngay cang phinh to). Dung enableDuplicateFiltering() giong het WorkflowSampleProcessSeeder:
 * sua noi dung file BPMN se tu dong tao VERSION MOI, khong tao version rac khi noi dung khong doi.
 * @Order(2): phai chay SAU DataSeeder (can tenant "default" da duoc tao san) - giong seeder mau.
 */
@Component
@Order(2)
public class AuditWorkflowProcessSeeder implements ApplicationRunner {

    private static final List<String> RESOURCE_NAMES = List.of(
            "audit-workitem-approval.bpmn20.xml",
            "audit-progress-report-approval.bpmn20.xml",
            "audit-recommendation-approval.bpmn20.xml");

    private final TenantRepository tenantRepository;
    private final RepositoryService repositoryService;

    public AuditWorkflowProcessSeeder(TenantRepository tenantRepository, RepositoryService repositoryService) {
        this.tenantRepository = tenantRepository;
        this.repositoryService = repositoryService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Tenant tenant : tenantRepository.findAll()) {
            for (String resourceName : RESOURCE_NAMES) {
                deploy(tenant.getId().toString(), resourceName);
            }
        }
    }

    private void deploy(String tenantId, String resourceName) throws IOException {
        Resource resource = new ClassPathResource("processes-templates/" + resourceName);
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .tenantId(tenantId)
                    .name(resourceName)
                    .enableDuplicateFiltering()
                    .addInputStream(resourceName, inputStream)
                    .deploy();
        }
    }
}
