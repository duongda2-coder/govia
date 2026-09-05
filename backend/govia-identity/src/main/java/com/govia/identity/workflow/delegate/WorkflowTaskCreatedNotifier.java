package com.govia.identity.workflow.delegate;

import com.govia.identity.notification.TaskAssignedNotification;
import com.govia.identity.notification.WorkflowNotificationService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * TaskListener DUNG CHUNG cho BAT KY userTask nao cua BAT KY quy trinh BPMN nao trong platform -
 * gan vao qua {@code <flowable:taskListener event="create" delegateExpression=
 * "${workflowTaskCreatedNotifier}"/>} tren userTask can bao khi co task moi (vd cac buoc phe
 * duyet). Khong gan mac dinh cho MOI userTask (co task noi bo khong can bao ai, vd subtask) - tuy
 * BPMN quyet dinh co gan hay khong.
 */
@Component("workflowTaskCreatedNotifier")
public class WorkflowTaskCreatedNotifier implements TaskListener {

    private final WorkflowNotificationService notificationService;
    private final RuntimeService runtimeService;

    public WorkflowTaskCreatedNotifier(WorkflowNotificationService notificationService, RuntimeService runtimeService) {
        this.notificationService = notificationService;
        this.runtimeService = runtimeService;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        if (delegateTask.getAssignee() == null) {
            // Task giao cho candidate group (vd SUPER_ADMIN) chua co 1 nguoi cu the nao - khong co
            // dia chi de bao, se duoc bao khi ai do claim() (khong nam trong pham vi dot nay).
            return;
        }
        String businessKey = null;
        if (delegateTask.getProcessInstanceId() != null) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(delegateTask.getProcessInstanceId())
                    .singleResult();
            businessKey = processInstance == null ? null : processInstance.getBusinessKey();
        }
        notificationService.notifyTaskAssigned(new TaskAssignedNotification(
                delegateTask.getId(),
                delegateTask.getName(),
                delegateTask.getAssignee(),
                delegateTask.getProcessDefinitionId(),
                businessKey,
                delegateTask.getTenantId()));
    }
}
