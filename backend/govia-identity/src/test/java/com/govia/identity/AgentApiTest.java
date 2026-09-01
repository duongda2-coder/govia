package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.llm.FakeLlmProvider;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test Audit AI Agent voi {@link FakeLlmProvider} (khong goi Ollama that - xem
 * application-test.yml: govia.llm.provider=fake). Moi test tu script 1 kich ban tool_calls cu the
 * roi kiem tra Agent thuc su goi dung Audit Tool that (qua AuditToolExecutor goi thang method cua
 * AuditToolsController - van bi @PreAuthorize cua controller do chan dung), khong chi kiem tra cau
 * tra loi cuoi "trong co ve dung".
 */
class AgentApiTest extends AbstractApiTest {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private FakeLlmProvider fakeLlmProvider;

    @Autowired
    private AgentAuditLogRepository agentAuditLogRepository;

    @BeforeEach
    void resetFake() {
        fakeLlmProvider.reset();
    }

    @Test
    void riskQuestion_executesRealToolAndReturnsFinalAnswer() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "NOPE", "year", 2025));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Chi nhanh NOPE chua co diem cham cho nam 2025.",
                "facts", List.of("Khong tim thay diem rui ro cho chi nhanh NOPE nam 2025")));

        JsonNode data = chat(adminToken, conversationId, "Chi nhanh NOPE co rui ro bao nhieu?");
        assertThat(data.get("answer").asText()).contains("chua co diem");
        assertThat(toolsUsed(data)).containsExactly("get_branch_risk");

        List<AgentAuditLog> logs = logsFor(conversationId);
        assertThat(logs).anyMatch(l -> "get_branch_risk".equals(l.getToolName()) && "SUCCESS".equals(l.getToolStatus()));
    }

    @Test
    void comparisonQuestion_callsMultipleTools() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "A1", "year", 2025));
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "B2", "year", 2025));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Ca hai chi nhanh A1 va B2 deu chua co diem cham cho nam 2025.",
                "facts", List.of("A1: chua co diem", "B2: chua co diem")));

        JsonNode data = chat(adminToken, conversationId, "So sanh chi nhanh A1 va B2");
        assertThat(toolsUsed(data)).containsExactly("get_branch_risk"); // toolsUsed la TAP ten tool, khong lap ten trung
        assertThat(data.get("facts")).hasSize(2);

        long callCount = logsFor(conversationId).stream().filter(l -> "get_branch_risk".equals(l.getToolName())).count();
        assertThat(callCount).isEqualTo(2); // that su goi 2 lan (A1 va B2), khong chi 1 lan
    }

    @Test
    void historyQuestion_callsRiskHistoryTool() throws Exception {
        UUID conversationId = UUID.randomUUID();
        // fromYear/toYear tuong minh - tranh nhanh "khong truyen nam" phai tra cuu danh muc Nam,
        // von khong duoc seed san tren H2 test (BusinessException AUDIT_TOOLS_NO_YEAR).
        fakeLlmProvider.enqueueToolCall("get_risk_history", Map.of("branchCode", "A1", "fromYear", 2023, "toYear", 2025));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Chua co lich su diem nao duoc ghi nhan cho chi nhanh A1.",
                "facts", List.of()));

        JsonNode data = chat(adminToken, conversationId, "Rui ro cua A1 thay doi the nao qua cac nam?");
        assertThat(toolsUsed(data)).containsExactly("get_risk_history");
    }

    @Test
    void auditFindingQuestion_callsAuditFindingsTool() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_audit_findings", Map.of("branchCode", "A1"));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Hien chua co phat hien kiem toan nao duoc ghi nhan cho chi nhanh A1.",
                "facts", List.of()));

        JsonNode data = chat(adminToken, conversationId, "Chi nhanh A1 co phat hien kiem toan nao chua?");
        assertThat(toolsUsed(data)).containsExactly("get_audit_findings");
        assertThat(data.get("answer").asText()).contains("chua co phat hien");
    }

    @Test
    void missingData_nullToolResultDoesNotBecomeAFabricatedFact() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "KHONGTONTAI", "year", 1999));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Hien chua co du lieu trong he thong de ket luan.",
                "facts", List.of()));

        JsonNode data = chat(adminToken, conversationId, "Chi nhanh KHONGTONTAI nam 1999 the nao?");
        assertThat(data.get("answer").asText()).isEqualTo("Hien chua co du lieu trong he thong de ket luan.");
        assertThat(data.get("facts")).isEmpty();
    }

    @Test
    void toolError_invalidArgumentIsReportedNotFabricated() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_risk_criteria", Map.of("kind", "bogus"));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Khong lay duoc du lieu tieu chi do loai khong hop le.",
                "facts", List.of()));

        JsonNode data = chat(adminToken, conversationId, "Xem tieu chi loai bogus");
        assertThat(data.get("answer").asText()).contains("Khong lay duoc du lieu");

        List<AgentAuditLog> logs = logsFor(conversationId);
        assertThat(logs).anyMatch(l -> "get_risk_criteria".equals(l.getToolName()) && "ERROR".equals(l.getToolStatus()));
    }

    @Test
    void unauthorizedUser_forbiddenIsReflectedNotBypassed() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID roleId = createRoleWithOnlyAgentPermission();
        createEmployeeWithAccount("NV-AGENT-NOPERM", "agentnopermuser", roleId);
        String restrictedToken = authService.login(new LoginRequest("default", "agentnopermuser", PASSWORD), null, null).login().accessToken();

        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "A1", "year", 2025));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Ban khong co quyen xem du lieu chi nhanh nay.",
                "facts", List.of()));

        JsonNode data = chat(restrictedToken, conversationId, "Chi nhanh A1 the nao?");
        assertThat(data.get("answer").asText()).contains("khong co quyen");

        List<AgentAuditLog> logs = logsFor(conversationId);
        assertThat(logs).anyMatch(l -> "get_branch_risk".equals(l.getToolName()) && "FORBIDDEN".equals(l.getToolStatus()));
    }

    @Test
    void outOfScopeQuestion_noToolCalled() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Toi chi ho tro cac cau hoi ve du lieu cham diem rui ro/audit finding.",
                "facts", List.of()));

        JsonNode data = chat(adminToken, conversationId, "Thoi tiet hom nay the nao?");
        assertThat(toolsUsed(data)).isEmpty();
    }

    @Test
    void evidenceGuard_dropsEvidenceForToolNeverCalled() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "A1", "year", 2025));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Chi nhanh A1 co rui ro cao.",
                "facts", List.of("Diem rui ro A1 la 82"),
                "evidence", List.of(Map.of("tool", "get_evidence", "args", Map.of(), "keyData", Map.of()))));

        JsonNode data = chat(adminToken, conversationId, "Chi nhanh A1 rui ro the nao?");
        assertThat(data.get("evidence")).isEmpty();
        assertThat(data.get("metadata").get("grounded").asBoolean()).isFalse();
    }

    @Test
    void evidenceGuard_keepsEvidenceForToolActuallyCalled() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_audit_findings", Map.of("branchCode", "A1"));
        fakeLlmProvider.enqueueFinalAnswer(Map.of(
                "answer", "Chua co phat hien nao cho A1.",
                "facts", List.of(),
                "evidence", List.of(Map.of("tool", "get_audit_findings", "args", Map.of("branchCode", "A1"), "keyData", Map.of()))));

        JsonNode data = chat(adminToken, conversationId, "A1 co phat hien gi khong?");
        assertThat(data.get("evidence")).hasSize(1);
        assertThat(data.get("metadata").get("grounded").asBoolean()).isTrue();
    }

    @Test
    void loopProtection_stopsAtMaxRoundsInsteadOfLoopingForever() throws Exception {
        UUID conversationId = UUID.randomUUID();
        for (int i = 0; i < 8; i++) {
            fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "A1", "year", 2020 + i));
        }

        JsonNode data = chat(adminToken, conversationId, "Kiem tra rat nhieu nam cho A1");
        assertThat(data.get("metadata").get("truncated").asBoolean()).isTrue();

        long toolCallLogs = logsFor(conversationId).stream().filter(l -> "get_branch_risk".equals(l.getToolName())).count();
        assertThat(toolCallLogs).isEqualTo(5); // MAX_TOOL_ROUNDS trong AgentOrchestratorService
    }

    private JsonNode chat(String token, UUID conversationId, String message) throws Exception {
        String body = mockMvc.perform(post("/api/audit/agent/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("conversationId", conversationId.toString(), "message", message))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data");
    }

    private List<AgentAuditLog> logsFor(UUID conversationId) {
        return agentAuditLogRepository.findAll().stream()
                .filter(l -> conversationId.equals(l.getConversationId()))
                .toList();
    }

    private List<String> toolsUsed(JsonNode data) {
        List<String> result = new ArrayList<>();
        data.get("metadata").get("toolsUsed").forEach(n -> result.add(n.asText()));
        return result;
    }

    private UUID createRoleWithOnlyAgentPermission() throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("AGENT_ONLY_TEST", "Chi co quyen dung Agent (test)", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID roleId = UUID.fromString(objectMapper.readTree(roleBody).get("data").get("id").asText());

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RolePermissionsRequest(List.of("AUDIT.AGENT.VIEW")))))
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
