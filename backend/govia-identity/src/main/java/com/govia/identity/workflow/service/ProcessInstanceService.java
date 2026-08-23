package com.govia.identity.workflow.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.workflow.dto.ActivityHistoryEntry;
import com.govia.identity.workflow.dto.ProcessInstanceHistoryDetail;
import com.govia.identity.workflow.dto.ProcessInstanceSummary;
import com.govia.identity.workflow.dto.StartProcessRequest;
import com.govia.identity.workflow.dto.VariableHistoryEntry;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boc RuntimeService/HistoryService cua Flowable: moi thao tac deu tu loc theo
 * TenantContext.getTenantId() (Flowable ho tro san tenantId tren deployment/instance/task,
 * xem docs/kien-truc-ky-thuat muc 3 ve ky luat loc tenant tuong minh o tang service).
 */
@Service
public class ProcessInstanceService {

    private static final String DEFAULT_ESCALATION_DURATION = "PT1H";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final AuditLogService auditLogService;

    public ProcessInstanceService(RuntimeService runtimeService, HistoryService historyService,
                                   AuditLogService auditLogService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.auditLogService = auditLogService;
    }

    public ProcessInstanceSummary start(StartProcessRequest request, CurrentUserPrincipal principal) {
        String tenantId = TenantContext.getTenantId().toString();

        Map<String, Object> variables = new HashMap<>();
        if (request.variables() != null) {
            variables.putAll(request.variables());
        }
        variables.put("initiatorUserId", principal.userId().toString());
        variables.putIfAbsent("escalationDuration", DEFAULT_ESCALATION_DURATION);

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(request.processDefinitionKey())
                .tenantId(tenantId)
                .businessKey(request.businessKey())
                .variables(variables)
                .start();

        auditLogService.record("WorkflowInstance", null, AuditAction.CREATE,
                "Bat dau quy trinh '" + request.processDefinitionKey() + "', processInstanceId=" + instance.getId()
                        + (request.businessKey() != null ? ", businessKey=" + request.businessKey() : ""));

        return toSummary(findHistoric(instance.getId(), tenantId));
    }

    public List<ProcessInstanceSummary> list() {
        String tenantId = TenantContext.getTenantId().toString();
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .orderByProcessInstanceStartTime().desc()
                .list()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public void cancel(String processInstanceId, String reason) {
        String tenantId = TenantContext.getTenantId().toString();
        boolean exists = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(tenantId)
                .count() > 0;
        if (!exists) {
            throw new BusinessException("WORKFLOW_INSTANCE_NOT_FOUND", "Khong tim thay quy trinh dang chay",
                    HttpStatus.NOT_FOUND);
        }
        runtimeService.deleteProcessInstance(processInstanceId, reason != null ? reason : "Huy boi nguoi dung");
        auditLogService.record("WorkflowInstance", null, AuditAction.DELETE,
                "Huy quy trinh processInstanceId=" + processInstanceId + (reason != null ? ", ly do=" + reason : ""));
    }

    /** History + Audit: chi tiet toan bo hoat dong da chay qua va bien luu lai cua 1 process instance. */
    public ProcessInstanceHistoryDetail history(String processInstanceId) {
        String tenantId = TenantContext.getTenantId().toString();
        HistoricProcessInstance instance = findHistoric(processInstanceId, tenantId);
        if (instance == null) {
            throw new BusinessException("WORKFLOW_INSTANCE_NOT_FOUND", "Khong tim thay quy trinh", HttpStatus.NOT_FOUND);
        }

        List<ActivityHistoryEntry> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list()
                .stream()
                .map(this::toActivityEntry)
                .toList();

        List<VariableHistoryEntry> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(v -> new VariableHistoryEntry(v.getVariableName(), v.getValue()))
                .toList();

        return new ProcessInstanceHistoryDetail(toSummary(instance), activities, variables);
    }

    private ActivityHistoryEntry toActivityEntry(HistoricActivityInstance activity) {
        return new ActivityHistoryEntry(activity.getActivityId(), activity.getActivityName(), activity.getActivityType(),
                activity.getAssignee(), toInstant(activity.getStartTime()), toInstant(activity.getEndTime()));
    }

    private HistoricProcessInstance findHistoric(String processInstanceId, String tenantId) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(tenantId)
                .singleResult();
    }

    private ProcessInstanceSummary toSummary(HistoricProcessInstance instance) {
        return new ProcessInstanceSummary(
                instance.getId(),
                instance.getProcessDefinitionKey(),
                instance.getBusinessKey(),
                toInstant(instance.getStartTime()),
                toInstant(instance.getEndTime()));
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
