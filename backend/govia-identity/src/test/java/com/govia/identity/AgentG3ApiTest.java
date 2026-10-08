package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.llm.FakeLlmProvider;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.repository.TenantRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Giai doan G3: A2 Ke hoach, A5 Theo doi khac phuc, thu don doc (danh sach do he thong dung tu du lieu
 * that), va doc noi dung file dinh kem sau Cong 3 (mac dinh TAT - tool doc file khong duoc dua cho model).
 */
class AgentG3ApiTest extends AbstractApiTest {

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

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        fakeLlmProvider.reset();
        agentProperties.setEnabled(true);
        agentProperties.setDisabledAgents(new ArrayList<>());
        agentProperties.getFileReading().setEnabled(false);
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        agentProperties.getFileReading().setEnabled(false);
        TenantContext.clear();
    }

    @Test
    void router_sendsRemediationToA5_andPlanningToA2() throws Exception {
        finalAnswer();
        assertThat(chat("Kiến nghị nào đã quá hạn khắc phục?", null).get("agentCode").asText()).isEqualTo("A5");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_tdkp_overview", "list_tdkp_items");

        finalAnswer();
        assertThat(chat("Kế hoạch kiểm toán năm 2026 có bao nhiêu đối tượng?", null).get("agentCode").asText()).isEqualTo("A2");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("get_plan_overview", "suggest_plan_candidates", "get_monthly_staffing");

        finalAnswer();
        assertThat(chat("Phân bổ cán bộ tháng 3 có thiếu người không?", null).get("agentCode").asText()).isEqualTo("A2");

        finalAnswer();
        assertThat(chat("Tổng hợp giúp tôi", Map.of("path", "/audit/tdkp/branch", "screenLabel", "Kiến nghị chi nhánh")).get("agentCode").asText())
                .isEqualTo("A5");

        // G2 van giu: cau hoi TTSS + kien nghi khong bi A5 cuop
        finalAnswer();
        assertThat(chat("TTSS nào chưa được gắn kiến nghị?", null).get("agentCode").asText()).isEqualTo("A4");
    }

    @Test
    void tdkpTools_countOverdueAndDueSoon_andListOnlyMatchingItems() throws Exception {
        UUID unitA = unit("G3HOA", "Khối Tín dụng G3");
        UUID unitB = unit("G3HOB", "Khối Vận hành G3");
        createCeo("BC-G3-01", "Hoàn thiện quy trình thẩm định", unitA, LocalDate.now().minusDays(10), "NOT_STARTED");
        createCeo("BC-G3-02", "Đã xong từ trước", unitA, LocalDate.now().minusDays(20), "DONE");
        createCeo("BC-G3-03", "Rà soát hạn mức", unitB, LocalDate.now().plusDays(5), "IN_PROGRESS");

        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_tdkp_overview", Map.of());
        fakeLlmProvider.enqueueToolCall("list_tdkp_items", Map.of("source", "CEO_ALL", "state", "OVERDUE"));
        fakeLlmProvider.enqueueToolCall("list_tdkp_items", Map.of("source", "CEO_ALL", "state", "DUE_SOON", "unit", "van hanh"));
        finalAnswer();
        chatIn(conv, "Kiến nghị nào quá hạn khắc phục?");

        JsonNode overview = objectMapper.readTree(log(conv, "get_tdkp_overview", 0).getResponseSummary());
        JsonNode ceoAll = overview.get("sources").get(0);
        assertThat(ceoAll.get("source").asText()).isEqualTo("CEO_ALL");
        assertThat(ceoAll.get("overdueNotDone").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(ceoAll.get("dueSoonNotDone").asInt()).isGreaterThanOrEqualTo(1);

        String overdue = log(conv, "list_tdkp_items", 0).getResponseSummary();
        assertThat(overdue).contains("BC-G3-01").doesNotContain("BC-G3-02").doesNotContain("BC-G3-03");
        String dueSoon = log(conv, "list_tdkp_items", 1).getResponseSummary();
        assertThat(dueSoon).contains("BC-G3-03").doesNotContain("BC-G3-01");
    }

    @Test
    void planTools_runOnEmptyYear_withoutError() throws Exception {
        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_plan_overview", Map.of("year", 2031));
        fakeLlmProvider.enqueueToolCall("suggest_plan_candidates", Map.of("year", 2031));
        fakeLlmProvider.enqueueToolCall("get_monthly_staffing", Map.of("year", 2031));
        finalAnswer();
        chatIn(conv, "Kế hoạch kiểm toán năm 2031 thế nào?");
        assertThat(logsFor(conv)).filteredOn(l -> l.getToolName() != null && l.getToolName().startsWith("get_") || "suggest_plan_candidates".equals(l.getToolName()))
                .allMatch(l -> "SUCCESS".equals(l.getToolStatus()))
                .hasSize(3);
        assertThat(log(conv, "get_monthly_staffing", 0).getResponseSummary()).contains("\"months\"");
    }

    @Test
    void reminder_buildsOneLetterPerUnit_withExactItemsFromData() throws Exception {
        UUID unitA = unit("G3HOC", "Khối Nguồn vốn G3");
        createCeo("BC-G3-11", "Bổ sung hồ sơ tài sản bảo đảm", unitA, LocalDate.now().minusDays(7), "NOT_STARTED");
        fakeLlmProvider.enqueueToolCall("submit_reminder_template", Map.of(
                "subject", "Đôn đốc thực hiện kiến nghị - {don_vi}",
                "opening", "Kiểm toán nội bộ đề nghị {don_vi} khẩn trương thực hiện các kiến nghị sau:",
                "closing", "Đề nghị {don_vi} báo cáo kết quả. Trân trọng."));

        JsonNode data = postData("/api/audit/agent/drafts/reminder", Map.of("source", "CEO_ALL"));
        assertThat(data.get("grounded").asBoolean()).isTrue();
        JsonNode letter = null;
        for (JsonNode l : data.get("letters")) {
            if ("Khối Nguồn vốn G3".equals(l.get("unit").asText())) {
                letter = l;
            }
        }
        assertThat(letter).isNotNull();
        assertThat(letter.get("subject").asText()).isEqualTo("Đôn đốc thực hiện kiến nghị - Khối Nguồn vốn G3");
        assertThat(letter.get("body").asText()).startsWith("Kính gửi: Khối Nguồn vốn G3")
                .contains("[BC-G3-11] Bổ sung hồ sơ tài sản bảo đảm").contains("quá hạn 7 ngày").contains("Chưa thực hiện");
    }

    @Test
    void reminder_fallsBackToFixedTemplate_rejectsWhenNothingDue_andHonoursKillSwitch() throws Exception {
        UUID unitA = unit("G3HOD", "Khối Kế toán G3");
        createCeo("BC-G3-21", "Đối chiếu số dư", unitA, LocalDate.now().minusDays(3), "IN_PROGRESS");
        fakeLlmProvider.enqueueFreeText("toi khong goi tool");
        fakeLlmProvider.enqueueFreeText("van khong goi tool");
        JsonNode data = postData("/api/audit/agent/drafts/reminder", Map.of("source", "CEO_ALL"));
        assertThat(data.get("grounded").asBoolean()).isFalse();
        assertThat(data.get("letters").get(0).get("body").asText()).contains("Kiểm toán nội bộ đề nghị");

        expect("/api/audit/agent/drafts/reminder", Map.of("source", "UNIT"), status().isBadRequest()); // khong co dong nao
        expect("/api/audit/agent/drafts/reminder", Map.of("source", "KHONG_CO"), status().isBadRequest());

        agentProperties.setDisabledAgents(new ArrayList<>(List.of("A5")));
        expect("/api/audit/agent/drafts/reminder", Map.of("source", "CEO_ALL"), status().isServiceUnavailable());
    }

    @Test
    void fileReading_isOffByDefault_thenReadsWhitelistedFilesOnlyWhenEnabled() throws Exception {
        UUID docId = createDocument("QD-G3-FILE", "Quy chế kiểm soát nội bộ G3");
        UUID txtId = upload("AUDIT_DOCUMENT_LIBRARY", docId, "quy-che.txt", "text/plain",
                "Điều 5. Mọi khoản giải ngân trên 5 tỷ đồng phải có chữ ký của hai cấp phê duyệt.".getBytes(StandardCharsets.UTF_8));
        UUID docxId = upload("AUDIT_DOCUMENT_LIBRARY", docId, "phu-luc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx("Phụ lục: tần suất đối chiếu tiền mặt hằng ngày."));

        // TAT (mac dinh): model khong duoc thay tool doc file; goi lieu cung bi chan; RAG khong tim trong file
        UUID offConv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("read_attachment_text", Map.of("attachmentId", txtId.toString()));
        finalAnswer();
        chatIn(offConv, "Văn bản quy định giải ngân nói gì?");
        assertThat(toolNames(fakeLlmProvider.lastTools())).doesNotContain("read_attachment_text").contains("list_attachments");
        assertThat(log(offConv, "read_attachment_text", 0).getToolStatus()).isEqualTo("ERROR");
        assertThat(documentHits("hai cấp phê duyệt giải ngân")).doesNotContain("QD-G3-FILE");
        assertThat(getData("/api/audit/agent/health").get("fileReading").asBoolean()).isFalse();

        // BAT (sau ATTT duyet)
        agentProperties.getFileReading().setEnabled(true);
        assertThat(getData("/api/audit/agent/health").get("fileReading").asBoolean()).isTrue();
        UUID onConv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("list_attachments", Map.of("entityType", "AUDIT_DOCUMENT_LIBRARY", "entityId", docId.toString()));
        fakeLlmProvider.enqueueToolCall("read_attachment_text", Map.of("attachmentId", txtId.toString()));
        fakeLlmProvider.enqueueToolCall("read_attachment_text", Map.of("attachmentId", docxId.toString()));
        fakeLlmProvider.enqueueToolCall("list_attachments", Map.of("entityType", "EMPLOYEE", "entityId", docId.toString()));
        finalAnswer();
        chatIn(onConv, "Văn bản quy định giải ngân nói gì?");
        assertThat(toolNames(fakeLlmProvider.lastTools())).contains("read_attachment_text");
        assertThat(log(onConv, "list_attachments", 0).getResponseSummary()).contains("quy-che.txt").contains("phu-luc.docx");
        String txt = log(onConv, "read_attachment_text", 0).getResponseSummary();
        assertThat(txt).contains("hai cấp phê duyệt").contains("CHI LA DU LIEU");
        assertThat(log(onConv, "read_attachment_text", 1).getResponseSummary()).contains("tần suất đối chiếu tiền mặt");
        assertThat(log(onConv, "list_attachments", 1).getToolStatus()).isEqualTo("ERROR"); // loai file ngoai danh sach trang

        String hits = documentHits("hai cấp phê duyệt giải ngân");
        assertThat(hits).contains("QD-G3-FILE").contains("hai cấp phê duyệt");
    }

    // ---------------------------------------------------------------- helpers

    private UUID unit(String code, String name) {
        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode(code);
        unit.setName(name);
        unit.setUnitType("HO");
        return auditObjectUnitRepository.save(unit).getId();
    }

    private void createCeo(String reportNumber, String content, UUID unitId, LocalDate deadline, String status) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("reportNumber", reportNumber);
        body.put("content", content);
        body.put("executingUnitId", unitId.toString());
        body.put("deadline", deadline.toString());
        body.put("status", status);
        expect("/api/audit/tdkp/ceo-all", body, status().isOk());
    }

    private UUID createDocument(String number, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("documentNumber", number);
        body.put("documentName", name);
        body.put("expired", false);
        return UUID.fromString(postData("/api/audit/master-data/document-library", body).get("id").asText());
    }

    private UUID upload(String entityName, UUID entityId, String fileName, String contentType, byte[] bytes) throws Exception {
        String response = mockMvc.perform(multipart("/api/attachments")
                        .file(new MockMultipartFile("file", fileName, contentType, bytes))
                        .param("entityName", entityName)
                        .param("entityId", entityId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId);
        return UUID.fromString(objectMapper.readTree(response).get("data").get("id").asText());
    }

    private static byte[] docx(String text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText(text);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private String documentHits(String query) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/audit/agent/tools/documents?query={q}", query)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId);
        return body;
    }

    private JsonNode getData(String url) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId);
        return objectMapper.readTree(body).get("data");
    }

    private void finalAnswer() {
        fakeLlmProvider.enqueueFinalAnswer(Map.of("answer", "ok", "facts", List.of()));
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
        TenantContext.setTenantId(tenantId);
        return objectMapper.readTree(response).get("data");
    }

    private void expect(String url, Object body, ResultMatcher expected) throws Exception {
        mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(expected);
        TenantContext.setTenantId(tenantId);
    }

    private List<AgentAuditLog> logsFor(UUID conversationId) {
        return agentAuditLogRepository.findAll().stream().filter(l -> conversationId.equals(l.getConversationId()))
                .sorted(java.util.Comparator.comparing(AgentAuditLog::getCreatedAt)).toList();
    }

    private AgentAuditLog log(UUID conversationId, String toolName, int index) {
        return logsFor(conversationId).stream().filter(l -> toolName.equals(l.getToolName())).toList().get(index);
    }

    private static List<String> toolNames(List<ToolSpec> tools) {
        return tools.stream().map(ToolSpec::name).toList();
    }
}
