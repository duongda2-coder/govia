package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.llm.FakeLlmProvider;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Giai doan G2: A3 Tac nghiep KT, A4 Phat hien & Kien nghi, A7 Danh muc - dinh tuyen, 8 tool doc moi,
 * va soan nhap (kien nghi, chuan hoa cau chu) KHONG ghi gi vao du lieu nghiep vu.
 */
class AgentG2ApiTest extends AbstractApiTest {

    @Autowired
    private FakeLlmProvider fakeLlmProvider;
    @Autowired
    private AgentAuditLogRepository agentAuditLogRepository;
    @Autowired
    private AgentProperties agentProperties;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired
    private AuditEngagementRepository engagementRepository;
    @Autowired
    private AuditTtssRecordRepository ttssRecordRepository;
    @Autowired
    private AuditRecommendationService recommendationService;
    @Autowired
    private EmployeeService employeeService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        fakeLlmProvider.reset();
        agentProperties.setEnabled(true);
        agentProperties.setDisabledAgents(new ArrayList<>());
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void router_sendsTtssToA4_workToA3_catalogsToA7() throws Exception {
        finalAnswer("ok");
        assertThat(chat("TTSS trọng yếu của cuộc kiểm toán CKT-X có bao nhiêu?", null).get("agentCode").asText()).isEqualTo("A4");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_ttss_summary", "search_similar_findings").doesNotContain("get_branch_risk");

        finalAnswer("ok");
        assertThat(chat("Tiến độ công việc THKT của đợt kiểm toán CKT-X?", null).get("agentCode").asText()).isEqualTo("A3");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_engagement_work_items", "search_catalog");

        finalAnswer("ok");
        assertThat(chat("Điểm kiểm soát nào áp dụng cho nghiệp vụ cho vay?", null).get("agentCode").asText()).isEqualTo("A3");

        finalAnswer("ok");
        assertThat(chat("Danh mục loại ngoại lệ có tên nào bị trùng lặp không?", null).get("agentCode").asText()).isEqualTo("A7");
        assertThat(toolNames(fakeLlmProvider.lastTools())).containsExactlyInAnyOrder("search_catalog", "find_catalog_duplicates", "submit_final_answer");

        finalAnswer("ok");
        assertThat(chat("Tổng hợp giúp tôi", Map.of("path", "/audit/plan/execution/work-management/ttss", "screenLabel", "Quản lý TTSS")).get("agentCode").asText())
                .isEqualTo("A4");
        finalAnswer("ok");
        assertThat(chat("Giúp tôi xem", Map.of("path", "/audit/plan/master-data/exception-type", "screenLabel", "Loại ngoại lệ")).get("agentCode").asText())
                .isEqualTo("A7");
    }

    @Test
    void ttssTools_summarizeFilterAndFindSimilarAcrossEngagements() throws Exception {
        AuditEngagement current = createEngagement("CKTG2A01");
        AuditEngagement past = createEngagement("CKTG2A02");
        saveTtss(current, "PH01", "Hồ sơ vay thiếu tài sản bảo đảm", true, "120000000");
        saveTtss(current, "PH01", "Hồ sơ vay thiếu tài sản bảo đảm", false, "30000000");
        saveTtss(current, "PH02", "Chậm cập nhật thông tin khách hàng", false, null);
        saveTtss(past, "PH09", "Hồ sơ vay thiếu tài sản bảo đảm theo quy định", true, null);
        recommendationService.create(past.getId(), new AuditRecommendationRequest(null, "Đề nghị chi nhánh bổ sung tài sản bảo đảm cho hồ sơ vay"));

        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_ttss_summary", Map.of("engagement", "CKTG2A01"));
        fakeLlmProvider.enqueueToolCall("get_ttss_records", Map.of("engagement", "cktg2a01", "materialOnly", true));
        fakeLlmProvider.enqueueToolCall("search_similar_findings", Map.of("query", "hồ sơ vay thiếu tài sản bảo đảm", "excludeEngagement", "CKTG2A01"));
        fakeLlmProvider.enqueueToolCall("list_engagement_recommendations", Map.of("engagement", "CKTG2A01"));
        finalAnswer("Tong hop TTSS.");
        chatIn(conv, "TTSS của cuộc kiểm toán CKTG2A01 thế nào?");

        AgentAuditLog summary = log(conv, "get_ttss_summary");
        assertThat(summary.getToolStatus()).isEqualTo("SUCCESS");
        JsonNode summaryJson = objectMapper.readTree(summary.getResponseSummary());
        assertThat(summaryJson.get("total").asInt()).isEqualTo(3);
        assertThat(summaryJson.get("material").asInt()).isEqualTo(1);
        assertThat(summaryJson.get("totalAmount").decimalValue()).isEqualByComparingTo("150000000");
        assertThat(summaryJson.get("topFindings").get(0).get("findingCode").asText()).isEqualTo("PH01");

        JsonNode records = objectMapper.readTree(log(conv, "get_ttss_records").getResponseSummary());
        assertThat(records.get("total").asInt()).isEqualTo(1);

        AgentAuditLog similar = log(conv, "search_similar_findings");
        assertThat(similar.getResponseSummary()).contains("CKTG2A02").contains("PH09").contains("bổ sung tài sản bảo đảm")
                .doesNotContain("CKTG2A01");
        assertThat(log(conv, "list_engagement_recommendations").getResponseSummary()).contains("KNKT000");
    }

    @Test
    void engagementTools_resolveByCodeAndRejectUnknownEngagement() throws Exception {
        createEngagement("CKTG2B01");
        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("list_my_engagements", Map.of("search", "CKTG2B01"));
        fakeLlmProvider.enqueueToolCall("get_engagement_work_items", Map.of("engagement", "CKTG2B01"));
        fakeLlmProvider.enqueueToolCall("get_engagement_work_items", Map.of("engagement", "KHONG-CO"));
        finalAnswer("ok");
        chatIn(conv, "Tiến độ công việc đợt kiểm toán CKTG2B01?");

        assertThat(log(conv, "list_my_engagements").getResponseSummary()).contains("CKTG2B01");
        List<AgentAuditLog> workLogs = logsFor(conv).stream().filter(l -> "get_engagement_work_items".equals(l.getToolName())).toList();
        assertThat(workLogs).anyMatch(l -> "SUCCESS".equals(l.getToolStatus()) && l.getResponseSummary().contains("\"total\":0"));
        assertThat(workLogs).anyMatch(l -> "ERROR".equals(l.getToolStatus()) && l.getResponseSummary().contains("KHONG-CO"));
    }

    @Test
    void catalogTools_searchAndFindNameDuplicates() throws Exception {
        createExceptionType("NLG2-01", "Thiếu chữ ký phê duyệt");
        createExceptionType("NLG2-02", "THIẾU CHỮ KÝ  phê duyệt");
        createExceptionType("NLG2-03", "Sai lệch số dư");

        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("search_catalog", Map.of("catalog", "exception_type", "query", "chữ ký"));
        fakeLlmProvider.enqueueToolCall("find_catalog_duplicates", Map.of("catalog", "exception_type"));
        fakeLlmProvider.enqueueToolCall("search_catalog", Map.of("catalog", "khong_ton_tai"));
        finalAnswer("ok");
        chatIn(conv, "Danh mục loại ngoại lệ có bị trùng lặp không?");

        assertThat(log(conv, "search_catalog").getResponseSummary()).contains("NLG2-01").contains("NLG2-02").doesNotContain("NLG2-03");
        JsonNode duplicates = objectMapper.readTree(log(conv, "find_catalog_duplicates").getResponseSummary()).get("duplicateGroups");
        assertThat(duplicates.toString()).contains("NLG2-01").contains("NLG2-02").doesNotContain("NLG2-03");
        assertThat(logsFor(conv)).anyMatch(l -> "search_catalog".equals(l.getToolName()) && "ERROR".equals(l.getToolStatus()));
    }

    @Test
    void draftRecommendations_returnsGroundedDrafts_flagsInventedOnes_andSavesNothing() throws Exception {
        AuditEngagement engagement = createEngagement("CKTG2C01");
        saveTtss(engagement, "PH01", "Hồ sơ vay thiếu tài sản bảo đảm", true, "120000000");
        int recommendationsBefore = recommendationService.list(engagement.getId()).size();

        fakeLlmProvider.enqueueToolCall("submit_recommendation_drafts", Map.of("drafts", List.of(
                Map.of("content", "Đề nghị chi nhánh bổ sung tài sản bảo đảm cho các hồ sơ vay còn thiếu (120.000.000 đồng).",
                        "findingCodes", List.of("PH01"), "rationale", "PH01 trọng yếu"),
                Map.of("content", "Đề nghị xử lý khoản 999.999.999 đồng.", "findingCodes", List.of("PH01")),
                Map.of("content", "Đề nghị rà soát phát hiện khác.", "findingCodes", List.of("PH77")))));

        JsonNode data = postData("/api/audit/agent/drafts/recommendations", Map.of("engagementId", engagement.getId().toString()));
        assertThat(data.get("engagementCode").asText()).isEqualTo("CKTG2C01");
        assertThat(data.get("sourceTtssCount").asInt()).isEqualTo(1);
        JsonNode drafts = data.get("drafts");
        assertThat(drafts).hasSize(3);
        assertThat(drafts.get(0).get("grounded").asBoolean()).isTrue();
        assertThat(drafts.get(1).get("grounded").asBoolean()).isFalse(); // so tien tu bia
        assertThat(drafts.get(2).get("grounded").asBoolean()).isFalse(); // ma phat hien khong co
        assertThat(drafts.get(2).get("findingCodes")).isEmpty();

        // Du lieu TTSS that da duoc dua vao prompt
        assertThat(fakeLlmProvider.lastMessages().get(1).content()).contains("PH01").contains("Hồ sơ vay thiếu tài sản bảo đảm");
        assertThat(recommendationService.list(engagement.getId())).hasSize(recommendationsBefore);
    }

    @Test
    void draftRecommendations_withoutTtss_isRejected_andRespectsKillSwitchAndLlmFailure() throws Exception {
        AuditEngagement empty = createEngagement("CKTG2D01");
        expectPost("/api/audit/agent/drafts/recommendations", Map.of("engagementId", empty.getId().toString()), status().isBadRequest());

        AuditEngagement engagement = createEngagement("CKTG2D02");
        saveTtss(engagement, "PH01", "Thiếu hồ sơ", false, null);
        fakeLlmProvider.failNextCall();
        expectPost("/api/audit/agent/drafts/recommendations", Map.of("engagementId", engagement.getId().toString()), status().isServiceUnavailable());

        agentProperties.setDisabledAgents(new ArrayList<>(List.of("A4")));
        expectPost("/api/audit/agent/drafts/recommendations", Map.of("engagementId", engagement.getId().toString()), status().isServiceUnavailable());
        expectPost("/api/audit/agent/drafts/rewrite", Map.of("text", "abc", "purpose", "RECOMMENDATION"), status().isServiceUnavailable());
    }

    @Test
    void rewrite_keepsFacts_andFlagsInventedNumbers() throws Exception {
        fakeLlmProvider.enqueueToolCall("submit_rewrite", Map.of("text", "Đề nghị chi nhánh bổ sung 15 hồ sơ vay còn thiếu tài sản bảo đảm.",
                "changes", List.of("Chuẩn hoá câu chữ")));
        JsonNode ok = postData("/api/audit/agent/drafts/rewrite",
                Map.of("text", "de nghi cn bo sung 15 ho so vay thieu tsbd", "purpose", "RECOMMENDATION"));
        assertThat(ok.get("text").asText()).startsWith("Đề nghị chi nhánh");
        assertThat(ok.get("grounded").asBoolean()).isTrue();

        fakeLlmProvider.enqueueToolCall("submit_rewrite", Map.of("text", "Đề nghị thu hồi 250.000.000 đồng."));
        JsonNode invented = postData("/api/audit/agent/drafts/rewrite", Map.of("text", "de nghi thu hoi tien", "purpose", "RECOMMENDATION"));
        assertThat(invented.get("grounded").asBoolean()).isFalse();
    }

    // ---------------------------------------------------------------- helpers

    private AuditEngagement createEngagement(String code) {
        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("U" + code.substring(code.length() - 6));
        unit.setName("Chi nhanh " + code);
        unit.setUnitType("CN");
        unit = auditObjectUnitRepository.save(unit);
        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode(code);
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(10);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setDecisionNumber("QD-" + code);
        engagement.setTeamLeadEmployeeId(employeeService.create(new EmployeeRequest("TL-" + code, "Truong doan " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null)).id());
        return engagementRepository.save(engagement);
    }

    private void saveTtss(AuditEngagement engagement, String findingCode, String findingName, boolean material, String amount) {
        AuditTtssRecord record = new AuditTtssRecord();
        record.setTenantId(tenantId);
        record.setEngagementId(engagement.getId());
        record.setFindingCode(findingCode);
        record.setFindingName(findingName);
        record.setTtssContent(findingName + " - chi tiết");
        record.setMaterial(material);
        record.setAmount(amount == null ? null : new BigDecimal(amount));
        ttssRecordRepository.save(record);
    }

    private void createExceptionType(String code, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", name);
        body.put("active", true);
        expectPost("/api/audit/plan/master-data/exception-type", body, status().isOk());
    }

    private void finalAnswer(String answer) {
        fakeLlmProvider.enqueueFinalAnswer(Map.of("answer", answer, "facts", List.of()));
    }

    private JsonNode chat(String message, Map<String, String> pageContext) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", UUID.randomUUID().toString());
        body.put("message", message);
        if (pageContext != null) {
            body.put("pageContext", pageContext);
        }
        return postData("/api/audit/agent/chat", body);
    }

    private void chatIn(UUID conversationId, String message) throws Exception {
        postData("/api/audit/agent/chat", Map.of("conversationId", conversationId.toString(), "message", message));
    }

    private JsonNode postData(String url, Object body) throws Exception {
        String response = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId); // filter JWT xoa TenantContext cua thread sau moi request
        return objectMapper.readTree(response).get("data");
    }

    private void expectPost(String url, Object body, ResultMatcher expected) throws Exception {
        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(expected);
        TenantContext.setTenantId(tenantId);
    }

    private List<AgentAuditLog> logsFor(UUID conversationId) {
        return agentAuditLogRepository.findAll().stream().filter(l -> conversationId.equals(l.getConversationId())).toList();
    }

    private AgentAuditLog log(UUID conversationId, String toolName) {
        return logsFor(conversationId).stream().filter(l -> toolName.equals(l.getToolName())).findFirst().orElseThrow();
    }

    private static List<String> toolNames(List<ToolSpec> tools) {
        return tools.stream().map(ToolSpec::name).toList();
    }
}
