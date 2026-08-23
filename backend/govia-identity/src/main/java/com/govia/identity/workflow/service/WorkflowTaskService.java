package com.govia.identity.workflow.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.workflow.dto.CompleteTaskRequest;
import com.govia.identity.workflow.dto.TaskSummary;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowTaskService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final AuditLogService auditLogService;

    public WorkflowTaskService(TaskService taskService, RuntimeService runtimeService, AuditLogService auditLogService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.auditLogService = auditLogService;
    }

    /**
     * "Viec cua toi": task da giao truc tiep cho nguoi goi, GOP voi task dang mo cho candidate group
     * trung 1 trong cac vai tro (role code) cua nguoi goi ma CHUA co ai nhan (assignee null) - vd
     * buoc "Super Admin" cua employee_approval dung flowable:candidateGroups="SUPER_ADMIN".
     */
    public List<TaskSummary> myTasks(CurrentUserPrincipal principal) {
        String tenantId = TenantContext.getTenantId().toString();

        List<Task> assigned = taskService.createTaskQuery()
                .taskTenantId(tenantId)
                .taskAssignee(principal.userId().toString())
                .orderByTaskCreateTime().desc()
                .list();

        List<Task> candidate = principal.roles() == null || principal.roles().isEmpty()
                ? List.of()
                : taskService.createTaskQuery()
                        .taskTenantId(tenantId)
                        .taskCandidateGroupIn(principal.roles())
                        .taskUnassigned()
                        .orderByTaskCreateTime().desc()
                        .list();

        Map<String, Task> merged = new LinkedHashMap<>();
        assigned.forEach(t -> merged.put(t.getId(), t));
        candidate.forEach(t -> merged.putIfAbsent(t.getId(), t));

        return toSummaries(merged.values());
    }

    /** Toan bo task dang mo trong tenant (danh cho vai tro giam sat/quan tri). */
    public List<TaskSummary> allTasks() {
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(TenantContext.getTenantId().toString())
                .orderByTaskCreateTime().desc()
                .list();
        return toSummaries(tasks);
    }

    /** SLA: task da qua han (dueDate) va van chua hoan tat, trong toan tenant. */
    public List<TaskSummary> overdueTasks() {
        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId(TenantContext.getTenantId().toString())
                .taskDueBefore(new Date())
                .orderByTaskDueDate().asc()
                .list();
        return toSummaries(tasks);
    }

    public void claim(String taskId, CurrentUserPrincipal principal) {
        Task task = getOwnedOrThrow(taskId);
        taskService.claim(task.getId(), principal.userId().toString());
    }

    public void complete(String taskId, CompleteTaskRequest request, CurrentUserPrincipal principal) {
        Task task = getOwnedOrThrow(taskId);
        Map<String, Object> variables = request.variables() != null ? request.variables() : Map.of();
        taskService.complete(task.getId(), variables);

        auditLogService.record("WorkflowTask", null, resolveAction(variables),
                "Hoan tat task '" + task.getName() + "' (id=" + task.getId() + ") boi user=" + principal.username());
    }

    /** SLA: dat/xoa han xu ly cho 1 task (null de bo han). */
    public void setDueDate(String taskId, Instant dueDate) {
        Task task = getOwnedOrThrow(taskId);
        taskService.setDueDate(task.getId(), dueDate == null ? null : Date.from(dueDate));
    }

    /** Chuyen tiep (forward): gan han task nay cho nguoi khac, khong quay lai nguoi cu. Chi chinh
     * nguoi dang duoc giao (hoac Super Admin) moi chuyen tiep duoc. */
    public void reassign(String taskId, String newAssigneeUserId, CurrentUserPrincipal principal) {
        Task task = getOwnedOrThrow(taskId);
        ensureActorIsAssigneeOrAdmin(task, principal);
        taskService.setAssignee(task.getId(), newAssigneeUserId);
        auditLogService.record("WorkflowTask", null, AuditAction.UPDATE,
                "Chuyen tiep task '" + task.getName() + "' (id=" + task.getId() + ") tu user="
                        + principal.username() + " sang userId=" + newAssigneeUserId);
    }

    /** Uy quyen (delegate): giao TAM THOI cho nguoi khac xu ly, ban than van la "owner". Nguoi duoc
     * uy quyen goi resolve() (khong phai complete()) khi xong - task se tu quay lai cho owner. */
    public void delegate(String taskId, String delegateUserId, CurrentUserPrincipal principal) {
        Task task = getOwnedOrThrow(taskId);
        ensureActorIsAssigneeOrAdmin(task, principal);
        taskService.delegateTask(task.getId(), delegateUserId);
        auditLogService.record("WorkflowTask", null, AuditAction.UPDATE,
                "Uy quyen task '" + task.getName() + "' (id=" + task.getId() + ") tu user="
                        + principal.username() + " cho userId=" + delegateUserId);
    }

    /** Nguoi duoc uy quyen bao "da lam xong phan cua minh" - task quay ve owner (delegationState=RESOLVED),
     * owner van phai tu goi complete() de thuc su ket thuc task. */
    public void resolve(String taskId, Map<String, Object> variables, CurrentUserPrincipal principal) {
        Task task = getOwnedOrThrow(taskId);
        ensureActorIsAssigneeOrAdmin(task, principal);
        taskService.resolveTask(task.getId(), variables != null ? variables : Map.of());
        auditLogService.record("WorkflowTask", null, AuditAction.UPDATE,
                "Hoan tat phan uy quyen task '" + task.getName() + "' (id=" + task.getId() + ") boi user=" + principal.username());
    }

    /** Chi chinh nguoi dang duoc giao (assignee hien tai) hoac SUPER_ADMIN moi duoc chuyen tiep/uy
     * quyen/resolve 1 task - tranh nguoi khong lien quan (chi co quyen COMPLETE noi chung) can thiep
     * vao task cua nguoi khac. */
    private void ensureActorIsAssigneeOrAdmin(Task task, CurrentUserPrincipal principal) {
        boolean isCurrentAssignee = principal.userId().toString().equals(task.getAssignee());
        boolean isSuperAdmin = principal.roles() != null && principal.roles().contains("SUPER_ADMIN");
        if (!isCurrentAssignee && !isSuperAdmin) {
            throw new BusinessException("WORKFLOW_TASK_NOT_ASSIGNEE",
                    "Chi nguoi dang duoc giao task nay (hoac Super Admin) moi thao tac duoc", HttpStatus.FORBIDDEN);
        }
    }

    /** Subtask: tao 1 task con gan voi task cha - dung khi 1 buoc duyet can chia nho thanh nhieu
     * viec phu (vd "Kiem tra ho so" sinh ra subtask "Xac minh CCCD", "Xac minh dia chi"...). */
    public TaskSummary createSubtask(String parentTaskId, String name, CurrentUserPrincipal principal) {
        Task parent = getOwnedOrThrow(parentTaskId);

        Task subtask = taskService.newTask();
        subtask.setName(name);
        subtask.setParentTaskId(parent.getId());
        subtask.setTenantId(TenantContext.getTenantId().toString());
        subtask.setAssignee(principal.userId().toString());
        taskService.saveTask(subtask);

        auditLogService.record("WorkflowTask", null, AuditAction.CREATE,
                "Tao subtask '" + name + "' cho task cha '" + parent.getName() + "' (id=" + parent.getId() + ")");

        return toSummary(subtask, businessKeyOf(parent.getProcessInstanceId()));
    }

    public List<TaskSummary> listSubtasks(String parentTaskId) {
        getOwnedOrThrow(parentTaskId);
        // TaskQuery cua Flowable khong co ham loc truc tiep theo parentTaskId (chi co theo scope) -
        // loc lai o tang ung dung tren tap task trong tenant.
        List<Task> subtasks = taskService.createTaskQuery()
                .taskTenantId(TenantContext.getTenantId().toString())
                .list()
                .stream()
                .filter(t -> parentTaskId.equals(t.getParentTaskId()))
                .toList();
        return toSummaries(subtasks);
    }

    private Task getOwnedOrThrow(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskTenantId(TenantContext.getTenantId().toString())
                .singleResult();
        if (task == null) {
            throw new BusinessException("WORKFLOW_TASK_NOT_FOUND", "Khong tim thay task", HttpStatus.NOT_FOUND);
        }
        return task;
    }

    private AuditAction resolveAction(Map<String, Object> variables) {
        Object approved = variables.get("approved");
        if (Boolean.TRUE.equals(approved)) {
            return AuditAction.APPROVE;
        }
        if (Boolean.FALSE.equals(approved)) {
            return AuditAction.REJECT;
        }
        return AuditAction.UPDATE;
    }

    private List<TaskSummary> toSummaries(Collection<Task> tasks) {
        Set<String> processInstanceIds = tasks.stream()
                .map(Task::getProcessInstanceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // Collections.emptyMap() (khong phai Map.of()): .get(null) phai tra ve null thay vi nem NPE,
        // vi subtask (task.getProcessInstanceId() == null) van di qua nhanh nay.
        Map<String, String> businessKeys = processInstanceIds.isEmpty() ? java.util.Collections.emptyMap()
                : runtimeService.createProcessInstanceQuery()
                        .processInstanceIds(processInstanceIds)
                        .list()
                        .stream()
                        .collect(Collectors.toMap(ProcessInstance::getId, pi -> pi.getBusinessKey() == null ? "" : pi.getBusinessKey()));

        return tasks.stream()
                .map(task -> toSummary(task, businessKeys.get(task.getProcessInstanceId())))
                .toList();
    }

    /** Tra ve businessKey cua process instance dang chua 1 task - dung rieng cho subtask (chi 1 task). */
    private String businessKeyOf(String processInstanceId) {
        if (processInstanceId == null) {
            return null;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        return instance == null ? null : instance.getBusinessKey();
    }

    private TaskSummary toSummary(Task task, String businessKey) {
        return new TaskSummary(task.getId(), task.getName(), task.getProcessInstanceId(),
                task.getProcessDefinitionId(), businessKey, task.getAssignee(), task.getOwner(),
                task.getDelegationState() == null ? null : task.getDelegationState().name(),
                task.getParentTaskId(), toInstant(task.getCreateTime()), toInstant(task.getDueDate()));
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
