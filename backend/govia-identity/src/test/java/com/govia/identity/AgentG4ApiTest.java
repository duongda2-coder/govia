package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.entity.AgentAuditLog;
import com.govia.audit.agent.llm.FakeLlmProvider;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.repository.AgentAuditLogRepository;
import com.govia.audit.agent.service.AgentProfileRegistry;
import com.govia.audit.agent.service.AgentSuggestionScheduler;
import com.govia.audit.dgcl.AuditDgclDto.LineInput;
import com.govia.audit.dgcl.AuditDgclDto.SaveRequest;
import com.govia.audit.dgcl.AuditDgclService;
import com.govia.audit.dgcl.DgclAppendix;
import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.service.AuditEmployeeCapabilityService;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Giai doan G4: A6 Chat luong (DGCL) + "Gợi ý chấm ĐGCL" (chi goi y, khong luu phieu), muc "Gợi ý AI" do job theo lich
 * sinh (khong goi model, theo quyen tung nguoi), "Kiểm tra file trước khi import" (A7), gioi han agent theo vai tro, KPI.
 */
class AgentG4ApiTest extends AbstractApiTest {

    @Autowired private FakeLlmProvider fakeLlmProvider;
    @Autowired private AgentAuditLogRepository agentAuditLogRepository;
    @Autowired private AgentProperties agentProperties;
    @Autowired private AgentProfileRegistry profileRegistry;
    @Autowired private AgentSuggestionScheduler scheduler;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditObjectUnitRepository auditObjectUnitRepository;
    @Autowired private AuditDgclService dgclService;
    @Autowired private AuditEmployeeCapabilityService capabilityService;
    @Autowired private EmployeeService employeeService;
    @Autowired private AuditEngagementRepository engagementRepository;
    @Autowired private AuditEngagementGroupRepository groupRepository;
    @Autowired private AuditEngagementGroupMemberRepository memberRepository;
    @Autowired private com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository recommendationRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        fakeLlmProvider.reset();
        agentProperties.setEnabled(true);
        agentProperties.setDisabledAgents(new ArrayList<>());
        agentProperties.setAgentRoles("");
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        agentProperties.setAgentRoles("");
        agentProperties.setDisabledAgents(new ArrayList<>());
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ---------------------------------------------------------------- A6

    @Test
    void router_sendsQualityQuestionsToA6_withoutStealingRiskScoring() throws Exception {
        finalAnswer();
        assertThat(chat("Đánh giá chất lượng cuộc kiểm toán CN01 đã chấm xong chưa?", null).get("agentCode").asText()).isEqualTo("A6");
        assertThat(toolNames(fakeLlmProvider.lastTools()))
                .contains("get_dgcl_overview", "get_dgcl_sheet", "get_engagement_dossier", "get_dgcl_evaluator_variance")
                .doesNotContain("get_branch_risk", "get_ttss_records", "list_tdkp_items");

        finalAnswer();
        assertThat(chat("Người chấm nào chấm PL01A lệch nhiều?", null).get("agentCode").asText()).isEqualTo("A6");

        finalAnswer();
        assertThat(chat("Tóm tắt giúp tôi", Map.of("path", "/audit/dgcl", "screenLabel", "Đánh giá chất lượng")).get("agentCode").asText())
                .isEqualTo("A6");

        finalAnswer();
        assertThat(chat("Vì sao chi nhánh HN01 bị chấm điểm rủi ro cao?", null).get("agentCode").asText()).isEqualTo("A1");

        JsonNode health = getData("/api/audit/agent/health");
        assertThat(health.get("scheduledSuggestions").asBoolean()).isTrue();
        assertThat(health.get("agents").toString()).contains("\"code\":\"A6\"");
    }

    @Test
    void qualityTools_readSavedSheetsThroughDgclScreenPermissions() throws Exception {
        Fixture fx = dgclFixture("CKT-G4-01");
        dgclService.saveSheet(fx.engagementId, fx.memberKey, DgclAppendix.PL01A,
                new SaveRequest(List.of(ok("A002", true), ok("A003", true), ok("A004", true), ok("A005", false))), fx.evaluator);

        UUID conv = UUID.randomUUID();
        fakeLlmProvider.enqueueToolCall("get_dgcl_overview", Map.of("engagement", "CKT-G4-01"));
        fakeLlmProvider.enqueueToolCall("get_dgcl_sheet", Map.of("engagement", "ckt-g4-01", "subject", fx.memberCode, "appendix", "PL01A"));
        fakeLlmProvider.enqueueToolCall("get_engagement_dossier", Map.of("engagement", "CKT-G4-01"));
        fakeLlmProvider.enqueueToolCall("get_dgcl_evaluator_variance", Map.of("year", 2026));
        fakeLlmProvider.enqueueToolCall("get_dgcl_sheet", Map.of("engagement", "CKT-KHONG-CO", "subject", "TEAM", "appendix", "PL01A"));
        finalAnswer();
        chatIn(conv, "Đánh giá chất lượng CKT-G4-01 thế nào?");

        String overview = log(conv, "get_dgcl_overview", 0).getResponseSummary();
        assertThat(overview).contains("CKT-G4-01").contains("75 (chưa xác nhận)").contains("notSavedPl01f");
        String sheet = log(conv, "get_dgcl_sheet", 0).getResponseSummary();
        assertThat(sheet).contains("\"key\":\"A005\"").contains("KHONG_TUAN_THU").contains("\"score\":75.0")
                .doesNotContain("\"key\":\"A002\""); // issuesOnly mac dinh: dong tuan thu khong lap lai
        assertThat(log(conv, "get_engagement_dossier", 0).getResponseSummary()).contains("QD-CKT-G4-01").contains("timeline");
        assertThat(log(conv, "get_dgcl_evaluator_variance", 0).getResponseSummary()).contains(fx.evaluatorName).contains("PL01A");
        assertThat(log(conv, "get_dgcl_sheet", 1).getToolStatus()).isEqualTo("ERROR");
    }

    @Test
    void dgclScoreDraft_onlySuggests_validatesKeysAndNumbers_neverSavesTheSheet() throws Exception {
        Fixture fx = dgclFixture("CKT-G4-02");
        AuditEngagement e = engagementRepository.findById(fx.engagementId).orElseThrow();
        e.setObjective("Đánh giá tuân thủ quy trình cấp tín dụng");
        engagementRepository.saveAndFlush(e);
        dgclService.saveSheet(fx.engagementId, fx.memberKey, DgclAppendix.PL01A,
                new SaveRequest(List.of(ok("A002", true), ok("A005", false))), fx.evaluator);

        // Ho so co: quyet dinh + muc tieu. He thong chi hoi model ve tieu chi co can cu PHU HOP: A002 (muc tieu), A014 (quyet dinh)
        fakeLlmProvider.enqueueToolCall("submit_dgcl_suggestions", Map.of(
                "items", List.of(
                        // nhac can cu nhung bia noi dung khong co trong ho so -> ha
                        Map.of("key", "A014", "suggestion", "COMPLIANT", "reason", "Đã thấy biên bản triển khai", "evidence", "quyetDinh"),
                        Map.of("key", "A014", "suggestion", "COMPLIANT", "reason", "Triển khai theo quyết định QD-CKT-G4-02", "evidence", "quyetDinh"),
                        // "chua thay" -> thieu du lieu, khong phai vi pham -> ha
                        Map.of("key", "A002", "suggestion", "NON_COMPLIANT", "reason", "Chưa thấy mục tiêu cụ thể", "evidence", "mucTieu"),
                        // dan can cu khong phu hop voi tieu chi (quyet dinh cho tieu chi muc tieu) -> ha
                        Map.of("key", "A002", "suggestion", "COMPLIANT", "reason", "Mục tiêu nêu trong quyết định", "evidence", "quyetDinh"),
                        Map.of("key", "A002", "suggestion", "COMPLIANT", "reason", "Mục tiêu kiểm toán: đánh giá tuân thủ, kế hoạch 987", "evidence", "mucTieu"),
                        // tieu chi khong duoc hoi (khong co can cu phu hop) / key la -> bo
                        Map.of("key", "A025", "suggestion", "COMPLIANT", "reason", "Họp đoàn theo quyết định", "evidence", "quyetDinh"),
                        Map.of("key", "ZZZ99", "suggestion", "COMPLIANT", "reason", "x", "evidence", "quyetDinh")),
                "overall", "Đoàn cơ bản tuân thủ trình tự."));
        Map<String, Object> body = Map.of("engagementId", fx.engagementId.toString(), "subjectKey", fx.memberKey, "appendix", "PL01A");
        JsonNode data = postData("/api/audit/agent/drafts/dgcl-score", body);

        assertThat(data.get("grounded").asBoolean()).as("so 987 khong co trong ho so + key la").isFalse();
        assertThat(data.get("criteriaCount").asInt()).isEqualTo(data.get("items").size()).isGreaterThan(3);
        assertThat(data.toString()).doesNotContain("ZZZ99");
        assertThat(item(data, "A014").get("suggestion").asText()).isEqualTo("COMPLIANT");
        assertThat(item(data, "A002").get("suggestion").asText()).isEqualTo("COMPLIANT");
        assertThat(item(data, "A002").get("current").asText()).isEqualTo("COMPLIANT");
        assertThat(item(data, "A025").get("suggestion").asText()).as("khong co can cu phu hop -> khong hoi model").isEqualTo("NEED_REVIEW");
        JsonNode a005 = item(data, "A005");
        assertThat(a005.get("current").asText()).isEqualTo("NON_COMPLIANT");
        assertThat(a005.get("suggestion").asText()).isEqualTo("NEED_REVIEW");
        assertThat(a005.get("differsFromCurrent").asBoolean()).isFalse();
        assertThat(data.get("suggestedPositive").asInt()).isEqualTo(2);
        assertThat(data.get("suggestedNegative").asInt()).isZero();
        assertThat(data.get("downgraded").asInt()).as("A014 bia noi dung, A002 'chua thay', A002 can cu khong phu hop").isEqualTo(3);
        // Model chi thay cac tieu chi co can cu phu hop + chi chon duoc can cu co du lieu
        String sent = fakeLlmProvider.lastMessages().get(1).content();
        assertThat(sent).contains("A014").contains("A002").doesNotContain("\"A025\"").doesNotContain("\"A005\"");
        assertThat(fakeLlmProvider.lastTools().get(0).parametersJsonSchema().toString()).contains("quyetDinh").contains("mucTieu").doesNotContain("congViec");
        assertThat(data.get("dossierFacts").toString()).contains("Kiến nghị đã tạo").contains("Thực địa: chưa có").doesNotContain("null");

        // Phieu van y nguyen: A005 van "khong tuan thu", diem 50
        var after = dgclService.getSheet(fx.engagementId, fx.memberKey, DgclAppendix.PL01A, fx.evaluator);
        assertThat(after.summary().score()).isEqualTo(50.0);
        assertThat(after.lines().stream().filter(l -> l.key().equals("A005")).findFirst().orElseThrow().nonCompliant()).isTrue();

        // PL01F: chi dong F008 (nhac "muc tieu kiem toan") co can cu phu hop -> hoi model 1 lo duy nhat chi gom dong do
        int calls = fakeLlmProvider.callCount();
        fakeLlmProvider.enqueueToolCall("submit_dgcl_suggestions", Map.of("items", List.of()));
        JsonNode f = postData("/api/audit/agent/drafts/dgcl-score",
                Map.of("engagementId", fx.engagementId.toString(), "subjectKey", fx.memberKey, "appendix", "PL01F"));
        assertThat(fakeLlmProvider.callCount()).isEqualTo(calls + 1);
        assertThat(fakeLlmProvider.lastMessages().get(1).content()).contains("F008").doesNotContain("\"F013\"");
        assertThat(f.get("needReview").asInt()).isEqualTo(f.get("criteriaCount").asInt());
        assertThat(f.get("suggestedRatio").isNull()).isTrue();

        // Model khong tra dung tool -> 502 than thien; tat A6 -> 503
        fakeLlmProvider.enqueueFreeText("khong goi tool");
        fakeLlmProvider.enqueueFreeText("van khong goi tool");
        expect("/api/audit/agent/drafts/dgcl-score", body, status().isBadGateway());
        agentProperties.setDisabledAgents(new ArrayList<>(List.of("A6")));
        expect("/api/audit/agent/drafts/dgcl-score", body, status().isServiceUnavailable());
    }

    @Test
    void dgclScoreDraft_withEmptyDossier_doesNotCallTheModel() throws Exception {
        Fixture fx = dgclFixture("CKT-G4-03");
        AuditEngagement e = engagementRepository.findById(fx.engagementId).orElseThrow();
        e.setDecisionNumber(null);
        e.setDecisionDate(null);
        engagementRepository.saveAndFlush(e);
        TenantContext.setTenantId(tenantId);
        int calls = fakeLlmProvider.callCount();
        JsonNode data = postData("/api/audit/agent/drafts/dgcl-score",
                Map.of("engagementId", fx.engagementId.toString(), "subjectKey", fx.memberKey, "appendix", "PL01A"));
        assertThat(fakeLlmProvider.callCount()).as("ho so khong co du lieu -> khong hoi AI").isEqualTo(calls);
        assertThat(data.get("needReview").asInt()).isEqualTo(data.get("criteriaCount").asInt());
        assertThat(data.get("overall").asText()).contains("chưa có dữ liệu");
        assertThat(recommendationRepository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, fx.engagementId))
                .as("AI doc ho so khong duoc lam man hinh kien nghi tu tao dong mac dinh").isEmpty();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.claimsMissingData("Hồ sơ chưa có thông tin về biên bản")).isTrue();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.claimsMissingData("Đã lập biên bản ngày 01/09")).isFalse();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.claimsMissingData("Chưa thấy lập báo cáo điều chỉnh mẫu")).isTrue();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.relevantTo("Đã có quyết định kiểm toán", "Chọn mẫu kiểm toán")).isFalse();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.mentionsEvidence("Đã thấy biên bản ghi nhận kết quả", "quyetDinh", "QD-TEST-01 ngày 2026-09-04")).isFalse();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.mentionsEvidence("Triển khai theo QD-TEST-01", "quyetDinh", "QD-TEST-01 ngày 2026-09-04")).isTrue();
        // 6 goi y THAT cua qwen2.5:7b (test giao dien 10/10/2026, ho so chi co quyet dinh + 1 kien nghi): chi giu cau dau
        java.util.function.BiPredicate<String, String> kept = (criterion, reason) ->
                com.govia.audit.agent.service.AgentQualityDraftService.relevantTo(reason, criterion)
                        && !com.govia.audit.agent.service.AgentQualityDraftService.claimsMissingData(reason)
                        && com.govia.audit.agent.service.AgentQualityDraftService.mentionsEvidence(reason, "quyetDinh", "QD-TEST-01 ngày 2026-09-04");
        assertThat(kept.test("Triển khai quyết định kiểm toán, tổ chức thực hiện kiểm toán", "Đã có quyết định kiểm toán")).isTrue();
        assertThat(kept.test("Thành viên đoàn kiểm toán hoàn thành Biên bản ghi nhận kết quả kiểm toán", "Đã thấy biên bản ghi nhận kết quả kiểm toán")).isFalse();
        assertThat(kept.test("Lập dự thảo Biên bản ghi nhận kết quả kiểm toán", "Đã thấy trưởng đoàn tổng hợp dự thảo biên bản")).isFalse();
        assertThat(kept.test("Trưởng đoàn kiểm toán tổ chức họp Đoàn để thống nhất dự thảo biên bản", "Đã thấy họp thống nhất dự thảo biên bản")).isFalse();
        assertThat(kept.test("Trưởng Đoàn kiểm toán gửi dự thảo Biên bản cho người đứng đầu đơn vị", "Đã thấy gửi dự thảo biên bản cho đơn vị được kiểm toán")).isFalse();
        assertThat(kept.test("Biên bản ghi nhận kết quả kiểm toán được lập và thông qua", "Đã thấy biên bản ghi nhận kết quả kiểm toán được lập và thông qua")).isFalse();
        assertThat(kept.test("Chọn mẫu kiểm toán", "Đã có quyết định kiểm toán")).isFalse();
        assertThat(kept.test("Lập báo cáo kèm lý do điều chỉnh mẫu chọn", "Chưa thấy lập báo cáo kèm lý do điều chỉnh mẫu chọn")).isFalse();
        // Lan 4: "Da thay ghi nhan trong quyet dinh" cho tieu chi bien ban - he thong chan bang bang tuong thich can cu<->tieu chi
        Map<String, Object> ev = Map.of("quyetDinh", "QD-TEST-01 ngày 2026-09-04", "kienNghi", "1 kiến nghị đã tạo");
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.fittingEvidence(ev,
                "Biên bản ghi nhận kết quả kiểm toán được lập và thông qua khi kết thúc cuộc kiểm toán")).isEmpty();
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.fittingEvidence(ev,
                "Triển khai quyết định kiểm toán, tổ chức thực hiện kiểm toán")).containsExactly("quyetDinh");
        assertThat(com.govia.audit.agent.service.AgentQualityDraftService.relevantTo("Đã có quyết định kiểm toán",
                "Triển khai quyết định kiểm toán, tổ chức thực hiện kiểm toán")).isTrue();
    }

    @Test
    void agentRoles_restrictSpecialistAgents_butSuperAdminAlwaysAllowed() {
        agentProperties.setAgentRoles("A6=TRUONG_DOAN|KSCL; A2=KE_HOACH");
        assertThat(agentProperties.rolesFor("a6")).containsExactlyInAnyOrder("TRUONG_DOAN", "KSCL");
        assertThat(agentProperties.rolesFor("A5")).isEmpty();

        as(List.of("CBKT"));
        assertThat(profileRegistry.isEnabled("A6")).isFalse();
        assertThat(profileRegistry.isEnabled("A2")).isFalse();
        assertThat(profileRegistry.isEnabled("A5")).isTrue();
        as(List.of("kscl"));
        assertThat(profileRegistry.isEnabled("A6")).isTrue();
        as(List.of("SUPER_ADMIN"));
        assertThat(profileRegistry.isEnabled("A2")).isTrue();
    }

    // ---------------------------------------------------------------- Goi y AI (M3)

    @Test
    void suggestions_refreshFromRealData_dismissSticksForTheDay_andScheduledJobRunsPerUser() throws Exception {
        UUID unit = unit("G4HOA", "Khối Tín dụng G4");
        createCeo("BC-G4-01", "Hoàn thiện hồ sơ thẩm định", unit, LocalDate.now().minusDays(12), "NOT_STARTED");
        createCeo("BC-G4-02", "Rà soát hạn mức", unit, LocalDate.now().plusDays(3), "IN_PROGRESS");
        int llmCallsBefore = fakeLlmProvider.callCount();

        JsonNode list = postData("/api/audit/agent/suggestions/refresh", Map.of());
        JsonNode overdue = byCategory(list, "TDKP_OVERDUE:CEO_ALL");
        assertThat(overdue).isNotNull();
        assertThat(overdue.get("severity").asText()).isEqualTo("HIGH");
        assertThat(overdue.get("detail").asText()).contains("BC-G4-01").contains("quá hạn 12 ngày").doesNotContain("BC-G4-02");
        assertThat(overdue.get("linkPath").asText()).isEqualTo("/audit/tdkp/ceo-all");
        assertThat(byCategory(list, "TDKP_DUE_SOON:CEO_ALL").get("detail").asText()).contains("BC-G4-02");
        assertThat(fakeLlmProvider.callCount()).as("job goi y khong goi model").isEqualTo(llmCallsBefore);

        // An -> khong hien lai trong ngay du lam moi
        mockMvc.perform(post("/api/audit/agent/suggestions/" + overdue.get("id").asText() + "/dismiss")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        TenantContext.setTenantId(tenantId);
        assertThat(byCategory(getData("/api/audit/agent/suggestions"), "TDKP_OVERDUE:CEO_ALL")).isNull();
        assertThat(byCategory(postData("/api/audit/agent/suggestions/refresh", Map.of()), "TDKP_OVERDUE:CEO_ALL")).isNull();

        postData("/api/audit/agent/suggestions/read", Map.of());
        JsonNode read = getData("/api/audit/agent/suggestions");
        assertThat(read).allMatch(s -> s.get("read").asBoolean());

        // Job theo lich: chay bang quyen cua tung nguoi dung, khong lam mat ngu canh cua thread goi
        var before = SecurityContextHolder.getContext();
        assertThat(scheduler.runAll()).isGreaterThanOrEqualTo(1);
        assertThat(SecurityContextHolder.getContext()).isSameAs(before);
        assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
        assertThat(byCategory(getData("/api/audit/agent/suggestions"), "TDKP_DUE_SOON:CEO_ALL")).isNotNull();

        // Tat AI -> khong sinh gi, API bao tat
        agentProperties.setEnabled(false);
        assertThat(scheduler.runAll()).isZero();
        mockMvc.perform(get("/api/audit/agent/suggestions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isServiceUnavailable());
        agentProperties.setEnabled(true);
    }

    @Test
    void scheduler_buildsPrincipalWithSameRolesAndPermissionsAsLogin() {
        CurrentUserPrincipal p = scheduler.principalOf(tenantId, adminUserId);
        assertThat(p.userId()).isEqualTo(adminUserId);
        assertThat(p.roles()).contains("SUPER_ADMIN");
        // wildcard "*" cua SUPER_ADMIN = moi quyen trong catalog, gom ca quyen moi cua G4
        assertThat(p.permissions()).contains("AUDIT.AGENT.VIEW", "AUDIT.AGENT.ADMIN", "AUDIT.DGCL.VIEW", "AUDIT.TDKP_CEO_ALL.VIEW");
    }

    // ---------------------------------------------------------------- Kiem tra file truoc import (A7)

    @Test
    void importCheck_comparesWithTheRealImportTemplate_andFlagsRowProblems_withoutImporting() throws Exception {
        createControlPoint("CKS-G4-01", "Kiểm soát phê duyệt giải ngân G4");
        List<String> headers = List.of("Mảng nghiệp vụ", "Mã CKS", "Tên CKS", "Rủi ro có thể xảy ra");
        byte[] file = xlsx(headers, List.of(
                List.of("", "CKS-G4-10", "Kiểm soát mới A", ""),
                List.of("", "CKS-G4-10", "Kiểm soát mới B", ""),
                List.of("", "", "Thiếu mã", ""),
                List.of("", "CKS-G4-01", "Tên khác", ""),
                List.of("", "CKS-G4-11", "kiem soat phe duyet giai ngan g4", "")));
        JsonNode r = importCheck("control_point", file);
        assertThat(r.get("totalRows").asInt()).isEqualTo(5);
        assertThat(r.get("codeColumn").asText()).isEqualTo("Mã CKS");
        assertThat(r.get("missingHeaders").toString()).contains("Thủ tục kiểm toán");
        assertThat(r.get("unknownHeaders").size()).isZero();
        assertThat(r.get("errors").toString()).contains("Trùng Mã CKS \\\"CKS-G4-10\\\"").contains("với dòng 2").contains("Thiếu Mã CKS");
        assertThat(r.get("warnings").toString()).contains("CKS-G4-01").contains("đã có trong danh mục")
                .contains("Tên trùng với mã \\\"CKS-G4-01\\\"");
        assertThat(r.get("readyToImport").asBoolean()).isFalse();
        assertThat(r.get("errorRows").asInt()).isEqualTo(2);

        // Tieu de go sai dau -> Import se bo qua cot do: bao ro
        JsonNode typo = importCheck("control_point", xlsx(List.of("Ma CKS", "Tên CKS"), List.of(List.of("X1", "Y1"))));
        assertThat(typo.get("missingHeaders").toString()).contains("Mã CKS");
        assertThat(typo.get("headerHints").toString()).contains("Ma CKS").contains("Mã CKS");

        // Khong import gi: danh muc khong co CKS-G4-10
        String list = mockMvc.perform(get("/api/audit/master-data/control-point").header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId);
        assertThat(list).doesNotContain("CKS-G4-10");

        mockMvc.perform(multipart("/api/audit/agent/drafts/import-check")
                        .file(new MockMultipartFile("file", "x.xlsx", "application/octet-stream", "khong phai excel".getBytes()))
                        .param("catalog", "control_point").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/audit/agent/drafts/import-check")
                        .file(new MockMultipartFile("file", "x.xlsx", "application/octet-stream", file))
                        .param("catalog", "khong_co").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
        TenantContext.setTenantId(tenantId);
    }

    // ---------------------------------------------------------------- KPI

    @Test
    void kpi_reportsUsageAndPlanTargets() throws Exception {
        fakeLlmProvider.enqueueFinalAnswer(Map.of("answer", "ok", "facts", List.of()));
        chat("Việc cần xử lý của tôi là gì?", null);
        JsonNode kpi = getData("/api/audit/agent/kpi?days=7");
        assertThat(kpi.get("periodDays").asInt()).isEqualTo(7);
        assertThat(kpi.get("questions").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(kpi.get("answersByAgent").has("A0")).isTrue();
        assertThat(kpi.get("groundedRate").isNumber()).isTrue();
        assertThat(kpi.get("latencyP90Ms").isNumber()).isTrue();
        assertThat(kpi.get("targets").size()).isEqualTo(3);
        assertThat(kpi.get("drafts").has("draft_dgcl_score")).isTrue();
    }

    // ---------------------------------------------------------------- helpers

    private record Fixture(UUID engagementId, String memberKey, String memberCode, CurrentUserPrincipal evaluator, String evaluatorName) {
    }

    private Fixture dgclFixture(String code) {
        EmployeeResponse lead = createEmployee("NV-" + code + "-TL");
        EmployeeResponse member = createEmployee("NV-" + code + "-TV");
        capabilityService.bulkUpdate(List.of(new AuditEmployeeCapabilityItemRequest(lead.id(),
                false, false, false, false, false, false, false, false, false, false, false, false, false, true, false)));

        AuditObjectUnit u = new AuditObjectUnit();
        u.setTenantId(tenantId);
        u.setCode("U" + code.replace("-", ""));
        u.setName("Chi nhánh " + code);
        u.setUnitType("CN");
        u = auditObjectUnitRepository.save(u);
        AuditEngagement e = new AuditEngagement();
        e.setTenantId(tenantId);
        e.setCode(code);
        e.setAuditObjectUnitId(u.getId());
        e.setYear(2026);
        e.setExpectedMonth(9);
        e.setDecisionDate(LocalDate.now());
        e.setTeamLeadEmployeeId(lead.id());
        e.setDecisionNumber("QD-" + code);
        e = engagementRepository.save(e);
        AuditEngagementGroup g = new AuditEngagementGroup();
        g.setTenantId(tenantId);
        g.setAuditEngagementId(e.getId());
        g.setGroupCode("N01");
        g.setLeaderEmployeeId(lead.id());
        g = groupRepository.save(g);
        AuditEngagementGroupMember m = new AuditEngagementGroupMember();
        m.setTenantId(tenantId);
        m.setGroupId(g.getId());
        m.setEmployeeId(member.id());
        memberRepository.save(m);
        CurrentUserPrincipal ev = new CurrentUserPrincipal(UUID.randomUUID(), "u-" + lead.employeeCode(), tenantId, lead.employeeCode(),
                List.of(), List.of(), UUID.randomUUID().toString());
        return new Fixture(e.getId(), member.id().toString(), member.employeeCode(), ev, lead.fullName());
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }

    private static LineInput ok(String key, boolean compliant) {
        return new LineInput(key, true, compliant, !compliant, false, null, null, null, null, null);
    }

    private static JsonNode item(JsonNode data, String key) {
        for (JsonNode i : data.get("items")) {
            if (key.equals(i.get("key").asText())) {
                return i;
            }
        }
        throw new AssertionError("Khong co tieu chi " + key);
    }

    private void as(List<String> roles) {
        CurrentUserPrincipal p = new CurrentUserPrincipal(UUID.randomUUID(), "u", tenantId, null, roles, List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, List.of()));
    }

    private static JsonNode byCategory(JsonNode list, String category) {
        for (JsonNode s : list) {
            if (category.equals(s.get("category").asText())) {
                return s;
            }
        }
        return null;
    }

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

    private void createControlPoint(String code, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", name);
        body.put("active", true);
        expect("/api/audit/master-data/control-point", body, status().isOk());
    }

    private static byte[] xlsx(List<String> headers, List<List<String>> rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Data");
            XSSFRow h = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                h.createCell(c).setCellValue(headers.get(c));
            }
            for (int r = 0; r < rows.size(); r++) {
                XSSFRow row = sheet.createRow(r + 1);
                for (int c = 0; c < rows.get(r).size(); c++) {
                    row.createCell(c).setCellValue(rows.get(r).get(c));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private JsonNode importCheck(String catalog, byte[] file) throws Exception {
        String body = mockMvc.perform(multipart("/api/audit/agent/drafts/import-check")
                        .file(new MockMultipartFile("file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file))
                        .param("catalog", catalog)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        TenantContext.setTenantId(tenantId);
        return objectMapper.readTree(body).get("data");
    }

    private JsonNode getData(String url) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", "Bearer " + adminToken))
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
