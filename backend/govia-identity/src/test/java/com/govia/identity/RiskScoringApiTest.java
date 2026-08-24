package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.riskscoring.masterdata.dto.CriteriaQuantitativeRequest;
import com.govia.audit.riskscoring.masterdata.dto.Group1Request;
import com.govia.audit.riskscoring.masterdata.dto.ScoreRankRequest;
import com.govia.audit.riskscoring.masterdata.entity.ObjectType;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
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
 * Kiem chung sub-module "Master data CDRR" ben trong module "Cham diem rui ro" (song song voi Audit
 * Master Data) - 3 danh muc tieu bieu thay vi lap lai ca 10: Group1 (CRUD don gian), CriteriaQuantitative
 * (co FK sang Group1/Group2), ScoreRank (co logic tu dong dong ky cu khi them ky moi) - cong voi 2 test
 * phan quyen: admin (SUPER_ADMIN) phai co du 6 quyen AUDIT.RISK_SCORING.*, con user KHONG duoc gan quyen
 * nay phai bi chan (403).
 */
class RiskScoringApiTest extends AbstractApiTest {

    @Test
    void createUpdateDeleteGroup1() throws Exception {
        String createBody = mockMvc.perform(post("/api/audit/risk-scoring/master-data/group1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Group1Request(ObjectType.CNDL, "CM", "MÔI TRƯỜNG KIỂM SOÁT (ĐL)",
                                        new BigDecimal("0.1"), null, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CM"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("data").get("id").asText();

        mockMvc.perform(get("/api/audit/risk-scoring/master-data/group1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='CM')].name", org.hamcrest.Matchers.hasItem("MÔI TRƯỜNG KIỂM SOÁT (ĐL)")));

        mockMvc.perform(put("/api/audit/risk-scoring/master-data/group1/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Group1Request(ObjectType.CNDL, "CM", "MÔI TRƯỜNG KIỂM SOÁT (sửa)",
                                        new BigDecimal("0.2"), null, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("MÔI TRƯỜNG KIỂM SOÁT (sửa)"));

        mockMvc.perform(delete("/api/audit/risk-scoring/master-data/group1/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit/risk-scoring/master-data/group1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='CM')]", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void createCriteriaQuantitativeRequiresValidGroup1() throws Exception {
        String randomGroup1Id = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(post("/api/audit/risk-scoring/master-data/criteria-quantitative")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CriteriaQuantitativeRequest(ObjectType.CNDL, java.util.UUID.fromString(randomGroup1Id), null,
                                        "CEAT01", "Đã được kiểm tra toàn diện", 1, new BigDecimal("0.05"), null,
                                        new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"), new BigDecimal("5"),
                                        new BigDecimal("100"), null, true, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RISK_GROUP1_NOT_FOUND"));

        String group1Body = mockMvc.perform(post("/api/audit/risk-scoring/master-data/group1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Group1Request(ObjectType.CNDL, "CM", "Nhóm CM", new BigDecimal("0.1"), null, null, true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String group1Id = objectMapper.readTree(group1Body).get("data").get("id").asText();

        mockMvc.perform(post("/api/audit/risk-scoring/master-data/criteria-quantitative")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CriteriaQuantitativeRequest(ObjectType.CNDL, java.util.UUID.fromString(group1Id), null,
                                        "CEAT01", "Đã được kiểm tra toàn diện", 1, new BigDecimal("0.05"), null,
                                        new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4"), new BigDecimal("5"),
                                        new BigDecimal("100"), null, true, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CEAT01"))
                .andExpect(jsonPath("$.data.group1Code").value("CM"));
    }

    @Test
    void creatingNewRankPeriodAutoClosesPreviousOpenPeriod() throws Exception {
        mockMvc.perform(post("/api/audit/risk-scoring/master-data/rank")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ScoreRankRequest(new BigDecimal("0"), new BigDecimal("40.99"), "THẤP", 2019, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toYear").value(9999));

        mockMvc.perform(post("/api/audit/risk-scoring/master-data/rank")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ScoreRankRequest(new BigDecimal("0"), new BigDecimal("35.99"), "THẤP", 2024, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toYear").value(9999));

        mockMvc.perform(get("/api/audit/risk-scoring/master-data/rank").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.fromYear==2019 && @.rankLabel=='THẤP')].toYear", org.hamcrest.Matchers.hasItem(2023)))
                .andExpect(jsonPath("$.data[?(@.fromYear==2024 && @.rankLabel=='THẤP')].toYear", org.hamcrest.Matchers.hasItem(9999)));
    }

    @Test
    void adminHasAllRiskScoringPermissions() {
        LoginResponse login = authService.login(new LoginRequest("default", "admin", "Admin@123"));
        assertThat(login.permissions()).contains(
                "AUDIT.RISK_SCORING.VIEW", "AUDIT.RISK_SCORING.CREATE", "AUDIT.RISK_SCORING.EDIT",
                "AUDIT.RISK_SCORING.DELETE", "AUDIT.RISK_SCORING.EXPORT", "AUDIT.RISK_SCORING.IMPORT");
    }

    @Test
    void userWithoutRiskScoringPermissionIsForbidden() throws Exception {
        UUID roleId = ensureNoRiskScoringPermissionRoleId();
        createEmployeeWithAccount("NV-RS-NOPERM", "rsnopermuser", roleId);
        String plainUserToken = authService.login(new LoginRequest("default", "rsnopermuser", PASSWORD)).accessToken();

        mockMvc.perform(get("/api/audit/risk-scoring/master-data/group1").header("Authorization", "Bearer " + plainUserToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/audit/risk-scoring/master-data/group1")
                        .header("Authorization", "Bearer " + plainUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Group1Request(ObjectType.CNDL, "XX", "Khong duoc phep", null, null, null, true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit/risk-scoring/master-data/group1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private static final String PASSWORD = "Test@12345";

    private UUID ensureNoRiskScoringPermissionRoleId() throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("RS_NO_PERM_TEST", "Khong co quyen Cham diem rui ro (test)", null))))
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
                                        null, null, null, null, null, null, null, null))))
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
