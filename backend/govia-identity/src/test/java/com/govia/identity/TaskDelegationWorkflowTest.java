package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung Forward (reassign) va Uy quyen (delegate/resolve) tren task cua quy trinh simple_approval:
 * dung task "Submit" (assignee = initiatorUserId ngay luc start) lam task de thao tac.
 */
class TaskDelegationWorkflowTest extends AbstractApiTest {

    private static final String PASSWORD = "Test@12345";

    @Test
    void reassignMovesTaskToNewAssignee() throws Exception {
        UUID roleId = ensureApproverRoleId();
        UUID targetAccountId = createEmployeeWithAccount("NV-FWD-TARGET", "targetuser", roleId);

        String processInstanceId = startSimpleApproval();
        String taskId = firstMyTaskId(adminToken);

        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/reassign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeUserId", targetAccountId.toString()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + taskId + "')]", org.hamcrest.Matchers.hasSize(0)));

        String targetToken = authService.login(new LoginRequest("default", "targetuser", PASSWORD)).accessToken();
        mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + taskId + "')]", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void delegateThenResolveReturnsTaskToOwnerForFinalComplete() throws Exception {
        UUID roleId = ensureApproverRoleId();
        UUID delegateAccountId = createEmployeeWithAccount("NV-DELEGATE", "delegateuser", roleId);

        startSimpleApproval();
        String taskId = firstMyTaskId(adminToken);

        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/delegate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("delegateUserId", delegateAccountId.toString()))))
                .andExpect(status().isOk());

        String delegateToken = authService.login(new LoginRequest("default", "delegateuser", PASSWORD)).accessToken();
        String delegateTasksBody = mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + delegateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + taskId + "')]", org.hamcrest.Matchers.hasSize(1)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode delegatedTask = findTaskById(delegateTasksBody, taskId);
        org.assertj.core.api.Assertions.assertThat(delegatedTask.get("delegationState").asText()).isEqualTo("PENDING");

        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/resolve")
                        .header("Authorization", "Bearer " + delegateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String adminTasksBody = mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + taskId + "')]", org.hamcrest.Matchers.hasSize(1)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resolvedTask = findTaskById(adminTasksBody, taskId);
        org.assertj.core.api.Assertions.assertThat(resolvedTask.get("delegationState").asText()).isEqualTo("RESOLVED");

        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyCurrentAssigneeOrSuperAdminCanReassign() throws Exception {
        UUID roleId = ensureApproverRoleId();
        UUID bystanderAccountId = createEmployeeWithAccount("NV-BYSTANDER", "bystanderuser", roleId);

        startSimpleApproval();
        String taskId = firstMyTaskId(adminToken); // assignee = admin, khong phai bystander

        String bystanderToken = authService.login(new LoginRequest("default", "bystanderuser", PASSWORD)).accessToken();
        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/reassign")
                        .header("Authorization", "Bearer " + bystanderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeUserId", bystanderAccountId.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("WORKFLOW_TASK_NOT_ASSIGNEE"));
    }

    private JsonNode findTaskById(String body, String taskId) throws Exception {
        for (JsonNode task : objectMapper.readTree(body).get("data")) {
            if (taskId.equals(task.get("id").asText())) {
                return task;
            }
        }
        throw new AssertionError("Khong tim thay task " + taskId);
    }

    private String startSimpleApproval() throws Exception {
        String body = mockMvc.perform(post("/api/workflow/instances/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "processDefinitionKey", "simple_approval",
                                "variables", Map.of("approverUserId", "00000000-0000-0000-0000-000000000000")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("id").asText();
    }

    private String firstMyTaskId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get(0).get("id").asText();
    }

    private UUID ensureApproverRoleId() throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("WORKFLOW_APPROVER_DELEG", "Nguoi duyet quy trinh (delegation test)", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID roleId = UUID.fromString(objectMapper.readTree(roleBody).get("data").get("id").asText());

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RolePermissionsRequest(List.of("WORKFLOW.TASK.VIEW", "WORKFLOW.TASK.COMPLETE")))))
                .andExpect(status().isOk());
        return roleId;
    }

    private UUID createEmployeeWithAccount(String employeeCode, String username, UUID roleId) throws Exception {
        String empBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeRequest(employeeCode, "Nhan vien " + employeeCode, null, null, null,
                                        null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID employeeId = UUID.fromString(objectMapper.readTree(empBody).get("data").get("id").asText());

        mockMvc.perform(post("/api/employees/" + employeeId + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserAccountRequest(username, PASSWORD))))
                .andExpect(status().isOk());

        String accountsBody = mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID accountId = null;
        for (JsonNode account : objectMapper.readTree(accountsBody).get("data")) {
            JsonNode empId = account.get("employeeId");
            if (empId != null && !empId.isNull() && employeeId.toString().equals(empId.asText())) {
                accountId = UUID.fromString(account.get("id").asText());
                break;
            }
        }
        if (accountId == null) {
            throw new IllegalStateException("Khong tim thay account vua tao cho employee " + employeeId);
        }

        mockMvc.perform(put("/api/accounts/" + accountId + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignRolesRequest(List.of(roleId)))))
                .andExpect(status().isOk());
        return accountId;
    }
}
