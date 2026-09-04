package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.riskscoring.scoring.dto.GroupHORequest;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung sub-module "Cham Diem" ben trong module "Cham diem rui ro" (song song voi Master Data
 * CDRR) - sheet dau tien ZTC_Nhom_DGRR_HO: CRUD + trung ma + phan quyen (admin co du quyen, user
 * khong duoc gan quyen AUDIT.RISK_SCORING_EXEC bi chan 403).
 */
class RiskScoringExecApiTest extends AbstractApiTest {

    private static final String PASSWORD = "Test@12345";

    @Test
    void createUpdateDeleteGroupHO() throws Exception {
        String createBody = mockMvc.perform(post("/api/audit/risk-scoring/scoring/group-ho")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GroupHORequest("10", "Nhóm I các đơn vị hỗ trợ nghiệp vụ tín dụng",
                                        "Nhóm I các đơn vị hỗ trợ nghiệp vụ tín dụng", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("10"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("data").get("id").asText();

        mockMvc.perform(get("/api/audit/risk-scoring/scoring/group-ho").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='10')].name", org.hamcrest.Matchers.hasItem("Nhóm I các đơn vị hỗ trợ nghiệp vụ tín dụng")));

        mockMvc.perform(put("/api/audit/risk-scoring/scoring/group-ho/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GroupHORequest("10", "Nhóm I (sửa)", null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nhóm I (sửa)"));

        mockMvc.perform(delete("/api/audit/risk-scoring/scoring/group-ho/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit/risk-scoring/scoring/group-ho").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='10')]", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void duplicateCodeIsRejected() throws Exception {
        mockMvc.perform(post("/api/audit/risk-scoring/scoring/group-ho")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GroupHORequest("20", "Nhóm II", null, true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audit/risk-scoring/scoring/group-ho")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GroupHORequest("20", "Nhóm II (trùng)", null, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RISK_GROUP_HO_CODE_DUPLICATE"));
    }

    @Test
    void adminHasAllRiskScoringExecPermissions() {
        LoginResponse login = loginAsUser("admin", "Admin@123");
        assertThat(login.permissions()).contains(
                "AUDIT.RISK_SCORING_EXEC.VIEW", "AUDIT.RISK_SCORING_EXEC.CREATE", "AUDIT.RISK_SCORING_EXEC.EDIT",
                "AUDIT.RISK_SCORING_EXEC.DELETE", "AUDIT.RISK_SCORING_EXEC.EXPORT", "AUDIT.RISK_SCORING_EXEC.IMPORT");
    }

    @Test
    void userWithoutPermissionIsForbidden() throws Exception {
        UUID roleId = ensureNoPermissionRoleId();
        createEmployeeWithAccount("NV-RSE-NOPERM", "rsenopermuser", roleId);
        String plainUserToken = authService.login(new LoginRequest("default", "rsenopermuser", PASSWORD), null, null).login().accessToken();

        mockMvc.perform(get("/api/audit/risk-scoring/scoring/group-ho").header("Authorization", "Bearer " + plainUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit/risk-scoring/scoring/group-ho").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private UUID ensureNoPermissionRoleId() throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("RSE_NO_PERM_TEST", "Khong co quyen Cham diem (test)", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID roleId = UUID.fromString(objectMapper.readTree(roleBody).get("data").get("id").asText());

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RolePermissionsRequest(List.of("PEOPLE.EMPLOYEE.VIEW")))))
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
                                        null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null))))
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
