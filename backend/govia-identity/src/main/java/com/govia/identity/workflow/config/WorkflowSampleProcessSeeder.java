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
 * Trien khai san cac quy trinh mau/dung chung cho MOI tenant hien co luc khoi dong. Khong dung co
 * che auto-deploy classpath:/processes/ mac dinh cua Flowable (flowable.check-process-definitions=false
 * o application.yml) vi co che do deploy 1 lan duy nhat luc boot va KHONG gan tenantId cu the nao -
 * trong khi moi truy van trong ProcessDefinitionService/ProcessInstanceService deu loc tuong minh
 * theo TenantContext.getTenantId() (dung ky luat multi-tenant chung cua platform).
 * Dung enableDuplicateFiltering() cua Flowable (khong tu viet check ton tai): moi lan boot deu thu
 * deploy, nhung Flowable tu so sanh noi dung file voi ban deploy gan nhat cung ten - giong het thi
 * bo qua (khong tao version rac), khac thi tu dong tao VERSION MOI (vd khi sua BPMN nhu employee-approval).
 * @Order(2): phai chay SAU DataSeeder (can tenant "default" da duoc tao san).
 */
@Component
@Order(2)
public class WorkflowSampleProcessSeeder implements ApplicationRunner {

    private record ProcessTemplate(String resourceName) {
        String resourcePath() {
            return "processes-templates/" + resourceName;
        }
    }

    private static final List<ProcessTemplate> TEMPLATES = List.of(
            new ProcessTemplate("simple-approval.bpmn20.xml"),
            new ProcessTemplate("employee-approval.bpmn20.xml"),
            new ProcessTemplate("framework-showcase.bpmn20.xml"));

    private final TenantRepository tenantRepository;
    private final RepositoryService repositoryService;

    public WorkflowSampleProcessSeeder(TenantRepository tenantRepository, RepositoryService repositoryService) {
        this.tenantRepository = tenantRepository;
        this.repositoryService = repositoryService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Tenant tenant : tenantRepository.findAll()) {
            for (ProcessTemplate template : TEMPLATES) {
                deploy(tenant.getId().toString(), template);
            }
        }
    }

    private void deploy(String tenantId, ProcessTemplate template) throws IOException {
        Resource resource = new ClassPathResource(template.resourcePath());
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .tenantId(tenantId)
                    .name(template.resourceName())
                    .enableDuplicateFiltering()
                    .addInputStream(template.resourceName(), inputStream)
                    .deploy();
        }
    }
}
