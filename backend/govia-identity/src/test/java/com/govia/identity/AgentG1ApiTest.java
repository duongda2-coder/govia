package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.llm.FakeLlmProvider;
import com.govia.audit.agent.llm.ToolSpec;
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
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Giai doan G1 cua AI Agent: A0 dieu phoi chon agent, moi agent chi dung bo tool cua minh, hoi thoai
 * luu DB va chi chu so huu xem duoc, cong tac tat AI, AI loi khong lam hong gi ngoai khung chat, va
 * 5 tool moi (bien dong diem, xep hang chuyen gia, viec cua toi, tim man hinh, tim van ban).
 */
class AgentG1ApiTest extends AbstractApiTest {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private FakeLlmProvider fakeLlmProvider;

    @Autowired
    private AgentAuditLogRepository agentAuditLogRepository;

    @Autowired
    private AgentProperties agentProperties;

    @BeforeEach
    void resetFake() {
        fakeLlmProvider.reset();
        agentProperties.setEnabled(true);
        agentProperties.setDisabledAgents(new ArrayList<>());
    }

    @Test
    void router_sendsRiskQuestionsToA1_andGeneralQuestionsToA0_withOnlyTheirTools() throws Exception {
        finalAnswer("Rui ro cua chi nhanh.");
        JsonNode risk = chat(adminToken, UUID.randomUUID(), "Chi nhánh HN01 có rủi ro thế nào?", null, null);
        assertThat(risk.get("agentCode").asText()).isEqualTo("A1");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_branch_risk", "get_score_changes")
                .doesNotContain("search_documents", "get_my_tasks");

        finalAnswer("Ban chua co viec nao.");
        JsonNode general = chat(adminToken, UUID.randomUUID(), "Việc của tôi hôm nay là gì?", null, null);
        assertThat(general.get("agentCode").asText()).isEqualTo("A0");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_my_tasks", "search_screens", "search_documents")
                .doesNotContain("get_branch_risk");

        // "quy dinh ve cham diem rui ro" la cau hoi tra van ban -> A0 du co nhac "rui ro"
        finalAnswer("Van ban quy dinh.");
        JsonNode doc = chat(adminToken, UUID.randomUUID(), "Quy định về chấm điểm rủi ro là văn bản nào?", null, null);
        assertThat(doc.get("agentCode").asText()).isEqualTo("A0");
    }

    @Test
    void router_followUpWithoutKeywords_staysWithPreviousAgent_andPageContextIsUsedForFirstTurn() throws Exception {
        UUID conversationId = UUID.randomUUID();
        finalAnswer("Diem nam 2025.");
        chat(adminToken, conversationId, "Điểm rủi ro của HN01 năm 2025?", null, null);
        finalAnswer("Diem nam 2024.");
        JsonNode followUp = chat(adminToken, conversationId, "Còn năm 2024 thì sao?", null, null);
        assertThat(followUp.get("agentCode").asText()).isEqualTo("A1");

        finalAnswer("Tra loi theo man hinh.");
        JsonNode onRiskScreen = chat(adminToken, UUID.randomUUID(), "Giải thích giúp tôi",
                Map.of("path", "/audit/risk-scoring/scoring/dashboard", "screenLabel", "Dashboard", "groupLabel", "Chấm điểm rủi ro"), null);
        assertThat(onRiskScreen.get("agentCode").asText()).isEqualTo("A1");
        String systemPrompt = fakeLlmProvider.lastMessages().get(0).content();
        assertThat(systemPrompt).contains("Chấm điểm rủi ro / Dashboard");
    }

    @Test
    void disabledSpecialist_fallsBackToGeneralAgent() throws Exception {
        agentProperties.setDisabledAgents(new ArrayList<>(List.of("A1")));
        finalAnswer("A0 tra loi.");
        JsonNode data = chat(adminToken, UUID.randomUUID(), "Chi nhánh nào rủi ro cao nhất?", null, null);
        assertThat(data.get("agentCode").asText()).isEqualTo("A0");
    }

    @Test
    void toolOutsideAgentScope_isBlockedEvenIfModelCallsIt() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_branch_risk", Map.of("branchCode", "HN01", "year", 2025));
        finalAnswer("Khong lay duoc.");

        JsonNode data = chat(adminToken, conversationId, "Tìm văn bản quy định chọn mẫu", null, null);
        assertThat(data.get("agentCode").asText()).isEqualTo("A0");
        assertThat(data.get("metadata").get("toolsUsed")).isEmpty();
        assertThat(logsFor(conversationId)).anyMatch(l -> "get_branch_risk".equals(l.getToolName())
                && "ERROR".equals(l.getToolStatus()) && l.getResponseSummary().contains("khong thuoc pham vi"));
    }

    @Test
    void conversation_isPersisted_listedForOwner_andHiddenFromOthers() throws Exception {
        UUID conversationId = UUID.randomUUID();
        finalAnswer("Cau tra loi 1.");
        chat(adminToken, conversationId, "Điểm rủi ro HN01 năm 2025?", null, null);
        finalAnswer("Cau tra loi 2.");
        chat(adminToken, conversationId, "Còn năm 2024?", null, null);

        JsonNode list = getData(adminToken, "/api/audit/agent/conversations");
        JsonNode mine = find(list, "id", conversationId.toString());
        assertThat(mine).isNotNull();
        assertThat(mine.get("messageCount").asInt()).isEqualTo(4);
        assertThat(mine.get("title").asText()).isEqualTo("Điểm rủi ro HN01 năm 2025?");

        JsonNode messages = getData(adminToken, "/api/audit/agent/conversations/" + conversationId + "/messages");
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(messages.get(1).get("role").asText()).isEqualTo("ASSISTANT");
        assertThat(messages.get(1).get("response").get("answer").asText()).isEqualTo("Cau tra loi 1.");
        assertThat(messages.get(1).get("agentCode").asText()).isEqualTo("A1");

        // Luot 2 da dua lich su luot 1 cho model
        assertThat(fakeLlmProvider.lastMessages()).anyMatch(m -> "Cau tra loi 1.".equals(m.content()));

        String otherToken = otherUserToken("agentg1other");
        mockMvc.perform(get("/api/audit/agent/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        finalAnswer("Khong duoc.");
        postChat(otherToken, conversationId, "Còn năm 2023?", status().isForbidden());

        mockMvc.perform(delete("/api/audit/agent/conversations/" + conversationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(find(getData(adminToken, "/api/audit/agent/conversations"), "id", conversationId.toString())).isNull();
    }

    @Test
    void killSwitch_disablesChat_andHealthReportsDisabled() throws Exception {
        agentProperties.setEnabled(false);
        JsonNode health = getData(adminToken, "/api/audit/agent/health");
        assertThat(health.get("enabled").asBoolean()).isFalse();
        assertThat(health.get("llmReachable").asBoolean()).isFalse();
        postChat(adminToken, UUID.randomUUID(), "Chi nhánh nào rủi ro cao?", status().isServiceUnavailable());

        agentProperties.setEnabled(true);
        health = getData(adminToken, "/api/audit/agent/health");
        assertThat(health.get("enabled").asBoolean()).isTrue();
        assertThat(health.get("agents")).hasSize(2);
    }

    @Test
    void llmFailure_returns503_andIsLogged_withoutSavingAHalfConversation() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.failNextCall();
        postChat(adminToken, conversationId, "Chi nhánh nào rủi ro cao?", status().isServiceUnavailable());
        assertThat(logsFor(conversationId)).anyMatch(l -> l.getEventType() == AgentAuditLog.EventType.ERROR);
        assertThat(find(getData(adminToken, "/api/audit/agent/conversations"), "id", conversationId.toString())).isNull();
    }

    @Test
    void searchDocuments_findsLibraryDocumentByMeaningWords_andSkipsExpiredByDefault() throws Exception {
        createDocument("QD-G1-001", "Quy định chọn mẫu kiểm toán tín dụng", "Chọn mẫu hồ sơ vay theo rủi ro", false);
        createDocument("QD-G1-002", "Quy định cũ về chọn mẫu tín dụng", "Đã thay thế", true);
        createDocument("QD-G1-003", "Quy chế chi tiêu nội bộ", "Công tác phí", false);

        JsonNode hits = getData(adminToken, "/api/audit/agent/tools/documents?query={q}", "chọn mẫu tín dụng");
        List<String> numbers = new ArrayList<>();
        hits.forEach(h -> numbers.add(h.get("documentNumber").asText()));
        assertThat(numbers).contains("QD-G1-001").doesNotContain("QD-G1-002", "QD-G1-003");
        assertThat(hits.get(0).get("matchMode").asText()).isEqualTo("keyword");

        JsonNode withExpired = getData(adminToken, "/api/audit/agent/tools/documents?includeExpired=true&query={q}", "chon mau tin dung");
        List<String> all = new ArrayList<>();
        withExpired.forEach(h -> all.add(h.get("documentNumber").asText()));
        assertThat(all).contains("QD-G1-001", "QD-G1-002");

        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("search_documents", Map.of("query", "chọn mẫu tín dụng"));
        finalAnswer("Van ban QD-G1-001.");
        chat(adminToken, conversationId, "Văn bản nào quy định chọn mẫu tín dụng?", null, null);
        assertThat(logsFor(conversationId)).anyMatch(l -> "search_documents".equals(l.getToolName())
                && "SUCCESS".equals(l.getToolStatus()) && l.getResponseSummary().contains("QD-G1-001"));
    }

    @Test
    void searchScreens_onlyReturnsScreensTheUserSentFromTheirOwnMenu() throws Exception {
        UUID conversationId = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("search_screens", Map.of("query", "kế hoạch kiểm toán tháng"));
        finalAnswer("Vao man hinh KHKT thang.");
        List<Map<String, String>> screens = List.of(
                Map.of("label", "KHKT tháng", "group", "Kế hoạch kiểm toán", "path", "/audit/plan/khkt-thang"),
                Map.of("label", "Nhân viên", "group", "Nhân sự", "path", "/people/employees"));

        chat(adminToken, conversationId, "Màn hình kế hoạch kiểm toán tháng ở đâu?", null, screens);
        AgentAuditLog log = logsFor(conversationId).stream().filter(l -> "search_screens".equals(l.getToolName())).findFirst().orElseThrow();
        assertThat(log.getToolStatus()).isEqualTo("SUCCESS");
        assertThat(log.getResponseSummary()).contains("/audit/plan/khkt-thang").doesNotContain("/people/employees");
    }

    @Test
    void newReadOnlyTools_runThroughTheirControllers() throws Exception {
        UUID riskConversation = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_score_changes", Map.of("year", 2025));
        fakeLlmProvider.enqueueToolCall("get_expert_rank_overrides", Map.of("year", 2025));
        finalAnswer("Chua co du lieu bien dong.");
        chat(adminToken, riskConversation, "Chi nhánh nào biến động điểm nhiều nhất năm 2025?", null, null);
        assertThat(logsFor(riskConversation)).anyMatch(l -> "get_score_changes".equals(l.getToolName()) && "SUCCESS".equals(l.getToolStatus()));
        assertThat(logsFor(riskConversation)).anyMatch(l -> "get_expert_rank_overrides".equals(l.getToolName()) && "SUCCESS".equals(l.getToolStatus()));

        UUID taskConversation = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_my_tasks", Map.of());
        finalAnswer("Ban khong co viec nao cho xu ly.");
        chat(adminToken, taskConversation, "Việc cần xử lý của tôi?", null, null);
        assertThat(logsFor(taskConversation)).anyMatch(l -> "get_my_tasks".equals(l.getToolName()) && "SUCCESS".equals(l.getToolStatus()));
    }

    // ---------------------------------------------------------------- helpers

    private void finalAnswer(String answer) {
        fakeLlmProvider.enqueueFinalAnswer(Map.of("answer", answer, "facts", List.of()));
    }

    private JsonNode chat(String token, UUID conversationId, String message, Map<String, String> pageContext,
                          List<Map<String, String>> screens) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId.toString());
        body.put("message", message);
        if (pageContext != null) {
            body.put("pageContext", pageContext);
        }
        if (screens != null) {
            body.put("screens", screens);
        }
        String response = mockMvc.perform(post("/api/audit/agent/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).get("data");
    }

    private void postChat(String token, UUID conversationId, String message, ResultMatcher expected) throws Exception {
        mockMvc.perform(post("/api/audit/agent/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("conversationId", conversationId.toString(), "message", message))))
                .andExpect(expected);
    }

    private JsonNode getData(String token, String url, Object... uriVars) throws Exception {
        String body = mockMvc.perform(get(url, uriVars).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data");
    }

    private void createDocument(String number, String name, String content, boolean expired) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("documentNumber", number);
        body.put("documentName", name);
        body.put("content", content);
        body.put("expired", expired);
        mockMvc.perform(post("/api/audit/master-data/document-library")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    /** User thu 2 chi co quyen dung AI - de kiem tra hoi thoai chi chu so huu doc/ghi tiep duoc. */
    private String otherUserToken(String username) throws Exception {
        String roleBody = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest("AGENT_G1_OTHER", "Chi dung Agent (test G1)", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID roleId = UUID.fromString(objectMapper.readTree(roleBody).get("data").get("id").asText());
        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RolePermissionsRequest(List.of("AUDIT.AGENT.VIEW")))))
                .andExpect(status().isOk());

        String empBody = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeRequest("NV-" + username.toUpperCase(), "Nhan vien " + username, null, null, null,
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

        UUID accountId = null;
        for (JsonNode account : getData(adminToken, "/api/accounts")) {
            JsonNode empId = account.get("employeeId");
            if (empId != null && !empId.isNull() && employeeId.toString().equals(empId.asText())) {
                accountId = UUID.fromString(account.get("id").asText());
            }
        }
        mockMvc.perform(put("/api/accounts/" + accountId + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignRolesRequest(List.of(roleId)))))
                .andExpect(status().isOk());
        return authService.login(new LoginRequest("default", username, PASSWORD), null, null).login().accessToken();
    }

    private List<AgentAuditLog> logsFor(UUID conversationId) {
        return agentAuditLogRepository.findAll().stream().filter(l -> conversationId.equals(l.getConversationId())).toList();
    }

    private static List<String> toolNames(List<ToolSpec> tools) {
        return tools.stream().map(ToolSpec::name).toList();
    }

    private static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode node : array) {
            if (value.equals(node.get(field).asText())) {
                return node;
            }
        }
        return null;
    }
}
