package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.identity.dto.ApprovalMatrixRuleRequest;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.entity.EmployeeRankLevel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung quy trinh Flowable "employee_approval" (xem employee-approval.bpmn20.xml) gan vao
 * nghiep vu tao moi Employee: nhan vien co quan ly se PENDING_APPROVAL va cho duyet lan luot qua
 * DAY QUAN LY DONG (Multi-Instance, do dai phu thuoc ApprovalMatrixRule - xem EmployeeApprovalService)
 * roi den buoc Super Admin (candidate group, co the tat qua rule); tu choi o bat ky buoc nao thi
 * REJECTED ngay.
 */
class EmployeeApprovalWorkflowTest extends AbstractApiTest {

    private static final String PASSWORD = "Test@12345";

    @Test
    void fullManagerChainApprovalReachesActive() throws Exception {
        UUID approverRoleId = ensureApproverRoleId();
        UUID managerC = createEmployee("NV-MGR-C", "Quan ly C", null);
        createAccount(managerC, "mgrc-full", approverRoleId);
        UUID managerB = createEmployee("NV-MGR-B", "Quan ly B", managerC);
        createAccount(managerB, "mgrb-full", approverRoleId);
        UUID managerA = createEmployee("NV-MGR-A", "Quan ly A", managerB);
        createAccount(managerA, "mgra-full", approverRoleId);

        String newEmployeeBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-NEW-FULL", "Nhan vien moi", managerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        UUID newEmployeeId = UUID.fromString(objectMapper.readTree(newEmployeeBody).get("data").get("id").asText());

        approveOnlyTask("mgra-full", "Duyệt theo dây quản lý");
        approveOnlyTask("mgrb-full", "Duyệt theo dây quản lý");
        approveOnlyTask("mgrc-full", "Duyệt theo dây quản lý");

        // Buoc cuoi: candidate group SUPER_ADMIN - admin thay task trong "Viec cua toi" du chua claim,
        // phai claim truoc khi complete.
        String adminTasksBody = mockMvc.perform(get("/api/workflow/tasks/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Duyệt cuối (Super Admin)"))
                .andReturn().getResponse().getContentAsString();
        String finalTaskId = objectMapper.readTree(adminTasksBody).get("data").get(0).get("id").asText();

        mockMvc.perform(post("/api/workflow/tasks/" + finalTaskId + "/claim")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workflow/tasks/" + finalTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variables", Map.of("approved", true)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/employees/" + newEmployeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void rejectionAtFirstLevelStopsChainImmediately() throws Exception {
        UUID approverRoleId = ensureApproverRoleId();
        UUID manager = createEmployee("NV-MGR-REJ", "Quan ly Tu Choi", null);
        createAccount(manager, "mgr-rej", approverRoleId);

        String newEmployeeBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-NEW-REJ", "Nhan vien bi tu choi", manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        UUID newEmployeeId = UUID.fromString(objectMapper.readTree(newEmployeeBody).get("data").get("id").asText());

        rejectOnlyTask("mgr-rej", "Duyệt theo dây quản lý");

        mockMvc.perform(get("/api/employees/" + newEmployeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void shortManagerChainSkipsStraightToSuperAdmin() throws Exception {
        // Quan ly duy nhat KHONG co UserAccount rieng -> approverChain rong, di thang toi Super Admin.
        UUID manager = createEmployee("NV-MGR-NOACC", "Quan ly Khong Tai Khoan", null);

        String newEmployeeBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-NEW-SHORT", "Nhan vien chuoi ngan", manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        UUID newEmployeeId = UUID.fromString(objectMapper.readTree(newEmployeeBody).get("data").get("id").asText());

        String adminTasksBody = mockMvc.perform(get("/api/workflow/tasks/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Duyệt cuối (Super Admin)"))
                .andReturn().getResponse().getContentAsString();
        String finalTaskId = objectMapper.readTree(adminTasksBody).get("data").get(0).get("id").asText();

        mockMvc.perform(post("/api/workflow/tasks/" + finalTaskId + "/claim")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workflow/tasks/" + finalTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variables", Map.of("approved", true)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/employees/" + newEmployeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void approvalMatrixRuleStopsAtConfiguredRankAndSkipsFinalStep() throws Exception {
        // Cau hinh ma tran: dung o cap N2, KHONG can them buoc Super Admin sau cung.
        mockMvc.perform(post("/api/workflow/approval-matrix")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalMatrixRuleRequest(null, EmployeeRankLevel.N2, false, true))))
                .andExpect(status().isOk());

        UUID approverRoleId = ensureApproverRoleId();
        UUID managerC = createEmployeeWithRank("NV-MTX-C", "Quan ly N4", null, "N4");
        createAccount(managerC, "mtx-mgrc", approverRoleId);
        UUID managerB = createEmployeeWithRank("NV-MTX-B", "Quan ly N2", managerC, "N2");
        createAccount(managerB, "mtx-mgrb", approverRoleId);
        UUID managerA = createEmployeeWithRank("NV-MTX-A", "Quan ly N1", managerB, "N1");
        createAccount(managerA, "mtx-mgra", approverRoleId);

        String newEmployeeBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-NEW-MTX", "Nhan vien theo ma tran", managerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        UUID newEmployeeId = UUID.fromString(objectMapper.readTree(newEmployeeBody).get("data").get("id").asText());

        approveOnlyTask("mtx-mgra", "Duyệt theo dây quản lý");
        approveOnlyTask("mtx-mgrb", "Duyệt theo dây quản lý");

        // Manager C (N4) khong bao gio co task - dây duyet dung lai dung o Manager B (N2) va KHONG
        // co buoc Super Admin (requireFinalSuperAdminStep=false) - nhan vien phai ACTIVE ngay.
        mockMvc.perform(get("/api/workflow/tasks/my").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.empty()));

        mockMvc.perform(get("/api/employees/" + newEmployeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private UUID createEmployeeWithRank(String code, String fullName, UUID managerId, String rankLevel) throws Exception {
        EmployeeRequest request = new EmployeeRequest(code, fullName, null, null, null, null, null, null, null, null,
                null, managerId, EmployeeRankLevel.valueOf(rankLevel));
        String body = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("data").get("id").asText());
    }

    private void approveOnlyTask(String username, String expectedTaskName) throws Exception {
        completeOnlyTask(username, expectedTaskName, true);
    }

    private void rejectOnlyTask(String username, String expectedTaskName) throws Exception {
        completeOnlyTask(username, expectedTaskName, false);
    }

    private void completeOnlyTask(String username, String expectedTaskName, boolean approved) throws Exception {
        String token = authService.login(new LoginRequest("default", username, PASSWORD)).accessToken();

        String tasksBody = mockMvc.perform(get("/api/workflow/tasks/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value(expectedTaskName))
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(tasksBody).get("data").get(0).get("id").asText();

        mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variables", Map.of("approved", approved)))))
                .andExpect(status().isOk());
    }

    private UUID createEmployee(String code, String fullName, UUID managerId) throws Exception {
        String body = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest(code, fullName, managerId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("data").get("id").asText());
    }

    /** Tao role dung chung, mang du 2 quyen can de xem/hoan tat task workflow, dung cho cac tai
     * khoan quan ly trong test (tai khoan moi tao mac dinh KHONG co quyen gi ca). */
    private UUID ensureApproverRoleId() throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("WORKFLOW_APPROVER", "Nguoi duyet quy trinh", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID roleId = UUID.fromString(objectMapper.readTree(roleBody).get("data").get("id").asText());

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RolePermissionsRequest(List.of("WORKFLOW.TASK.VIEW", "WORKFLOW.TASK.COMPLETE")))))
                .andExpect(status().isOk());
        return roleId;
    }

    private void createAccount(UUID employeeId, String username, UUID roleId) throws Exception {
        mockMvc.perform(post("/api/employees/" + employeeId + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserAccountRequest(username, PASSWORD))))
                .andExpect(status().isOk());

        String accountsBody = mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
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
    }

    private EmployeeRequest employeeRequest(String code, String fullName, UUID managerId) {
        return new EmployeeRequest(code, fullName, null, null, null, null, null, null, null, null, null, managerId, null);
    }
}
