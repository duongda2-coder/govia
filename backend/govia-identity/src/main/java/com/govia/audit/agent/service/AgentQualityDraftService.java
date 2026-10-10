package com.govia.audit.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.dto.DgclScoreDraftRequest;
import com.govia.audit.agent.dto.DgclScoreDraftResponse;
import com.govia.audit.agent.llm.LlmProvider;
import com.govia.audit.agent.llm.ToolSpec;
import com.govia.audit.agent.tools.AgentQualityToolsService;
import com.govia.audit.agent.tools.ToolExecutionResult;
import com.govia.audit.dgcl.AuditDgclDto.Line;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.dgcl.DgclAppendix;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Gợi ý chấm ĐGCL" (A6 Chat luong, G4, muc M2): AI doc tieu chi dang ap dung cua 1 phieu + ho so doan (moc thoi
 * gian, tien do cong viec, TTSS, kien nghi - do HE THONG lay qua controller san co) roi goi y tung tieu chi kem ly do.
 * KHONG ghi gi vao phieu: diem DGCL van do nguoi cham tu nhap, Lưu, Xác nhận tren man hinh hien co (dung nguyen tac
 * "diem DGCL van do nguoi cham nhap" cua phuong an). Moi goi y duoc kiem lai: ma tieu chi phai co trong phieu, so
 * lieu trong ly do phai co trong du lieu dua vao - khong thi grounded=false.
 */
@Service
public class AgentQualityDraftService {

    private static final String TOOL = "submit_dgcl_suggestions";
    private static final int MAX_CRITERIA = 80;
    /** So tieu chi moi lan goi model. */
    static final int BATCH = 12;
    private static final int CONTENT = 260;
    private static final Set<String> AB_VALUES = Set.of("COMPLIANT", "NON_COMPLIANT", "NEED_REVIEW");
    private static final Set<String> F_VALUES = Set.of("APPLIES", "NOT_APPLIES", "NEED_REVIEW");

    private final AgentQualityToolsService qualityTools;
    private final AgentDraftService draftService;
    private final AgentProfileRegistry registry;
    private final AgentAuditLogService auditLogService;
    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public AgentQualityDraftService(AgentQualityToolsService qualityTools, AgentDraftService draftService, AgentProfileRegistry registry,
                                    AgentAuditLogService auditLogService, LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.qualityTools = qualityTools;
        this.draftService = draftService;
        this.registry = registry;
        this.auditLogService = auditLogService;
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public DgclScoreDraftResponse suggest(DgclScoreDraftRequest request, CurrentUserPrincipal principal) {
        if (!registry.isEnabled(AgentProfileRegistry.QUALITY)) {
            throw new BusinessException("AGENT_DISABLED", "Tro ly AI Chat luong dang duoc tat theo cau hinh", HttpStatus.SERVICE_UNAVAILABLE);
        }
        long start = System.currentTimeMillis();
        DgclAppendix appendix = AgentQualityToolsService.appendix(request.appendix());
        AuditEngagementResponse engagement = qualityTools.resolveEngagement(principal, request.engagementId().toString());
        List<SubjectRow> subjects = qualityTools.subjects(principal, engagement);
        SubjectRow subject = qualityTools.resolveSubject(subjects, request.subjectKey());
        Sheet sheet = qualityTools.sheet(principal, engagement, subject.subjectKey(), appendix);
        List<Line> applicable = AgentQualityToolsService.applicableLines(sheet);
        if (applicable.isEmpty()) {
            throw new BusinessException("AGENT_DGCL_NO_CRITERIA", "Phieu nay khong co tieu chi nao dang ap dung de AI goi y", HttpStatus.BAD_REQUEST);
        }
        List<Line> criteria = applicable.stream().limit(MAX_CRITERIA).toList();
        Map<String, Line> byKey = new LinkedHashMap<>();
        criteria.forEach(l -> byKey.put(l.key(), l));

        Map<String, Object> dossier = qualityTools.getDossier(principal, engagement.code());
        List<String> facts = dossierFacts(dossier);

        boolean f = appendix == DgclAppendix.PL01F;
        // Chi nhung truong ho so CO DU LIEU moi duoc lam can cu; AI phai chi ra 1 trong so do cho moi goi y
        Map<String, Object> evidence = availableEvidence(dossier);
        String system = """
                Ban la can bo kiem soat chat luong Kiem toan noi bo ngan hang (A6). Doc "tieuChi" va "canCu" (du lieu that cua \
                he thong). CHI tra ve tieu chi ma 1 muc trong "canCu" CHUNG MINH TRUC TIEP duoc:
                %s
                QUY TAC:
                1. Chi dung "key" co trong tieuChi. "evidence" PHAI la 1 ten trong canCuPhuHop[key] cua chinh tieu chi do.
                2. Khong co muc nao trong canCu noi ve tieu chi -> BO QUA tieu chi do (he thong tu ghi "can xem ho so"). \
                   THIEU THONG TIN KHONG PHAI LA KHONG TUAN THU. KHONG doan, KHONG suy dien tu ten tieu chi.
                3. "reason": tieng Viet CO DAU, toi da 25 tu, neu dung du lieu trong canCu. KHONG bia so lieu, ten, ngay.
                4. "overall": 1 cau tieng Viet co dau.
                Day CHI LA GOI Y; nguoi cham tu quyet dinh va tu nhap diem. PHAI tra loi bang cach goi tool "%s".
                """.formatted(f
                ? "PL01F: APPLIES = loi tru diem / muc cong / muc tru nay CO xay ra (loi thi ghi violationCount uoc tinh), "
                  + "NOT_APPLIES = khong xay ra."
                : "COMPLIANT = doan da tuan thu tieu chi, NON_COMPLIANT = co dau hieu khong tuan thu.", TOOL);
        String extra = request.instruction() == null || request.instruction().isBlank() ? "" : "\nYeu cau them cua nguoi cham: " + request.instruction();

        // Chia nho tung lo tieu chi: model nho tra loi ngan, khong tran ngu canh, khong "noi mai" toi het thoi gian cho
        Map<String, Map<String, Object>> suggestions = new HashMap<>();
        Set<String> allowed = f ? F_VALUES : AB_VALUES;
        boolean grounded = true;
        int answeredBatches = 0;
        int downgraded = 0;
        List<String> overall = new ArrayList<>();
        if (evidence.isEmpty() || criteria.stream().allMatch(l -> fittingEvidence(evidence, l.content()).isEmpty())) {
            overall.add("Hồ sơ đoàn trên hệ thống chưa có dữ liệu phù hợp (mốc thời gian, công việc, TTSS, kiến nghị…) để AI làm căn cứ "
                    + "cho các tiêu chí của phiếu này - mọi tiêu chí cần người chấm xem hồ sơ.");
        }
        List<Line> askable = criteria.stream().filter(l -> !fittingEvidence(evidence, l.content()).isEmpty()).toList();
        for (int from = 0; from < askable.size(); from += BATCH) {
            List<Line> batch = askable.subList(from, Math.min(from + BATCH, askable.size()));
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("phieu", appendix.name());
            input.put("thanhVien", AgentQualityToolsService.subjectLabel(subject));
            input.put("nghiepVu", sheet.segmentCodes());
            input.put("canCu", evidence);
            // Moi tieu chi kem danh sach can cu duoc phep dung cho no
            input.put("canCuPhuHop", batch.stream().collect(java.util.stream.Collectors.toMap(Line::key,
                    l -> fittingEvidence(evidence, l.content()), (x, y) -> x, LinkedHashMap::new)));
            input.put("tieuChi", batch.stream().map(l -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("key", l.key());
                m.put("noiDung", excerpt(l.content()));
                if (l.kind() != null) {
                    m.put("loai", l.kind());
                }
                return m;
            }).toList());
            String inputJson = toJson(input);
            Map<String, Object> args = draftService.callForTool(system, "Du lieu (JSON):\n" + inputJson + extra, toolSpec(evidence.keySet()), TOOL, principal,
                    "AI goi y cham DGCL " + engagement.code() + " " + appendix.name());
            if (args.containsKey("_freeText")) {
                grounded = false; // lo nay model khong tra dung tool - cac tieu chi cua lo de "can xem ho so"
                continue;
            }
            answeredBatches++;
            String o = AgentDraftService.stringOrNull(args.get("overall"));
            if (o != null) {
                overall.add(o);
            }
            for (Map<String, Object> item : AgentDraftService.mapList(args.get("items"))) {
                String key = AgentDraftService.stringOrNull(item.get("key"));
                if (key == null || batch.stream().noneMatch(l -> l.key().equals(key.trim()))) {
                    grounded = false; // tieu chi khong co trong lo/phieu - bo
                    continue;
                }
                String reason = AgentDraftService.stringOrNull(item.get("reason"));
                if (reason != null && !AgentDraftService.numbersFrom(reason, inputJson)) {
                    grounded = false;
                }
                String cited = AgentDraftService.stringOrNull(item.get("evidence"));
                Line criterion = batch.stream().filter(l -> l.key().equals(key.trim())).findFirst().orElseThrow();
                if (cited == null || !evidence.containsKey(cited.trim()) || claimsMissingData(reason)
                        || !fittingEvidence(evidence, criterion.content()).contains(cited.trim())
                        || !relevantTo(reason, criterion.content()) || !mentionsEvidence(reason, cited.trim(), evidence.get(cited.trim()))) {
                    // Khong chi ra duoc can cu co that, hoac "chua co thong tin" bi coi la vi pham -> ha ve "can xem ho so"
                    downgraded++;
                    grounded = false;
                    continue;
                }
                suggestions.putIfAbsent(key.trim(), item);
            }
        }
        if (!askable.isEmpty() && answeredBatches == 0) {
            throw new BusinessException("AGENT_DGCL_NO_SUGGESTION", "AI chua dua ra duoc goi y cho phieu nay, vui long thu lai", HttpStatus.BAD_GATEWAY);
        }

        List<DgclScoreDraftResponse.Item> items = new ArrayList<>();
        int positive = 0;
        int negative = 0;
        int review = 0;
        for (Line l : criteria) {
            Map<String, Object> s = suggestions.get(l.key());
            String value = s == null ? "NEED_REVIEW" : String.valueOf(s.getOrDefault("suggestion", "NEED_REVIEW")).trim().toUpperCase(Locale.ROOT);
            if (!allowed.contains(value)) {
                value = "NEED_REVIEW";
            }
            switch (value) {
                case "COMPLIANT", "NOT_APPLIES" -> positive++;
                case "NON_COMPLIANT", "APPLIES" -> negative++;
                default -> review++;
            }
            Integer violations = f && "APPLIES".equals(value) && "DEDUCTION".equals(l.kind()) ? positiveInt(s == null ? null : s.get("violationCount")) : null;
            String current = current(l, sheet.saved(), f);
            boolean differs = !"NOT_SET".equals(current) && !"NEED_REVIEW".equals(value) && !current.equals(value);
            items.add(new DgclScoreDraftResponse.Item(l.key(), l.stt(), l.content(), current, value, violations,
                    s == null ? null : AgentDraftService.stringOrNull(s.get("reason")),
                    s == null ? null : AgentDraftService.stringOrNull(s.get("evidence")), differs));
        }
        Double ratio = f || positive + negative == 0 ? null
                : AgentQualityToolsService.round(100.0 * positive / (positive + negative));

        DgclScoreDraftResponse response = new DgclScoreDraftResponse(engagement.code(), AgentQualityToolsService.subjectLabel(subject),
                appendix.name(), sheet.saved(), sheet.confirmed(), sheet.summary() == null ? null : AgentQualityToolsService.round(sheet.summary().score()),
                applicable.size(), applicable.size() > criteria.size(), items, positive, negative, review, downgraded, ratio, facts,
                overall.isEmpty() ? null : String.join(" ", overall), grounded, llmProvider.modelId());
        auditLogService.logToolCall(principal.userId(), UUID.randomUUID(), 0, "AI goi y cham DGCL " + engagement.code(),
                "draft_dgcl_score", Map.of("engagement", engagement.code(), "subject", subject.subjectKey(), "appendix", appendix.name(),
                        "criteria", criteria.size()),
                ToolExecutionResult.success(toJson(Map.of("positive", positive, "negative", negative, "needReview", review, "grounded", grounded))),
                System.currentTimeMillis() - start);
        return response;
    }

    /** Gia tri nguoi cham dang nhap tren phieu, cung thang do voi goi y. */
    private static String current(Line l, boolean saved, boolean f) {
        if (f) {
            if ((l.violationCount() != null && l.violationCount() > 0) || l.checked()) {
                return "APPLIES";
            }
            return saved ? "NOT_APPLIES" : "NOT_SET";
        }
        if (!saved) {
            return "NOT_SET"; // phieu chua luu: o dang tich san mac dinh, chua phai ket qua cham
        }
        return l.nonCompliant() ? "NON_COMPLIANT" : l.compliant() ? "COMPLIANT" : "NOT_SET";
    }

    /** Du kien ho so do he thong rut ra (khong qua AI) - hien cho nguoi cham doi chieu. */
    @SuppressWarnings("unchecked")
    private static List<String> dossierFacts(Map<String, Object> d) {
        List<String> facts = new ArrayList<>();
        if (d.get("timeline") instanceof Map<?, ?> t) {
            facts.add("Chuẩn bị: " + t.get("planning") + "; Thực địa: " + t.get("fieldwork") + "; Báo cáo: " + t.get("report"));
        }
        if (d.get("workItems") instanceof Map<?, ?> w) {
            facts.add("Công việc được phân công: " + w.get("total") + " (theo trạng thái " + w.get("countByStatus") + ", theo giai đoạn "
                    + w.get("countByPhase") + ")");
        }
        if (d.get("ttss") instanceof Map<?, ?> t) {
            facts.add("TTSS: " + t.get("total") + ", trọng yếu " + t.get("material") + ", chưa gắn kiến nghị " + t.get("withoutTeamRecommendation"));
        }
        facts.add("Kiến nghị đã tạo: " + d.get("recommendations"));
        if (d.get("notAvailable") instanceof List<?> na && !na.isEmpty()) {
            facts.add("Không lấy được (thiếu quyền/không thuộc phân công): " + String.join(", ", (List<String>) na));
        }
        return facts;
    }

    /** Cac truong ho so CO DU LIEU (bo truong rong/null) - AI chi duoc dan 1 trong cac ten nay lam "evidence". */
    @SuppressWarnings("unchecked")
    static Map<String, Object> availableEvidence(Map<String, Object> d) {
        Map<String, Object> e = new LinkedHashMap<>();
        if (d.get("decisionNumber") != null || d.get("decisionDate") != null) {
            e.put("quyetDinh", d.get("decisionNumber") + " ngày " + d.get("decisionDate"));
        }
        if (d.get("timeline") instanceof Map<?, ?> t) {
            for (Map.Entry<?, ?> en : t.entrySet()) {
                String v = String.valueOf(en.getValue());
                if (!v.contains("null")) {
                    e.put("mocThoiGian." + en.getKey(), v);
                }
            }
        }
        if (d.get("workItems") instanceof Map<?, ?> w && w.get("total") instanceof Number n && n.intValue() > 0) {
            e.put("congViec", w);
        }
        if (d.get("ttss") instanceof Map<?, ?> t && t.get("total") instanceof Number n && n.intValue() > 0) {
            e.put("ttss", t);
        }
        if (d.get("recommendations") instanceof Number n && n.intValue() > 0) {
            e.put("kienNghi", n + " kiến nghị đã tạo");
        }
        for (String k : List.of("objective", "scope")) {
            if (d.get(k) instanceof String v && !v.isBlank()) {
                e.put(k.equals("objective") ? "mucTieu" : "phamVi", v);
            }
        }
        return e;
    }

    private static final java.util.regex.Pattern MISSING_DATA = java.util.regex.Pattern.compile(
            "(chua|khong) co (thong tin|du lieu|ho so|bang chung)|(chua|khong) (thay|ghi nhan|the hien|xac dinh duoc)|thieu (thong tin|du lieu|ho so)|khong tim thay");

    /** Tu chung xuat hien o hau het tieu chi/ly do - khong chung minh duoc lien quan. */
    private static final Set<String> GENERIC_WORDS = Set.of("da", "co", "cua", "va", "cac", "theo", "duoc", "chua", "khong", "doan",
            "kiem", "toan", "trong", "voi", "cho", "la", "thuc", "hien", "nay", "do", "tren", "vao", "ve", "khi", "thi", "mot", "nhung",
            "tai", "boi", "den", "tu", "hoac", "neu", "phai", "can", "day", "du", "tuan", "thu", "dung", "quy", "dinh", "noi", "bo",
            "truong", "thanh", "vien", "tieu", "chi", "ho", "so", "ngay", "nam");

    /** Loai tieu chi ma tung loai can cu CO THE chung minh - xet tren noi dung TIEU CHI (he thong quyet dinh, khong de
     * model tu chon): vd quyet dinh kiem toan chi chung minh duoc tieu chi noi ve quyet dinh, khong chung minh duoc "da lap
     * bien ban", "da hop doan". */
    private static final Map<String, List<String>> CRITERION_ANCHORS = Map.of(
            "quyetDinh", List.of("quyet dinh"),
            "congViec", List.of("phan cong", "nhiem vu", "cong viec"),
            "ttss", List.of("phat hien", "sai sot", "ton tai", "sai pham"),
            "kienNghi", List.of("kien nghi", "khuyen nghi"),
            "mucTieu", List.of("muc tieu"),
            "phamVi", List.of("pham vi"));
    private static final List<String> TIME_CRITERION_ANCHORS = List.of("thoi han", "dung han", "ngay lam viec", "thoi gian", "truoc khi");

    /** Can cu (trong so cac truong ho so co du lieu) phu hop voi 1 tieu chi. */
    public static List<String> fittingEvidence(Map<String, Object> evidence, String criterion) {
        String c = AgentText.normalize(criterion);
        return evidence.keySet().stream().filter(k -> {
            List<String> anchors = k.startsWith("mocThoiGian.") ? TIME_CRITERION_ANCHORS : CRITERION_ANCHORS.getOrDefault(k, List.of());
            return anchors.stream().anyMatch(a -> AgentText.containsPhrase(c, a));
        }).toList();
    }

    /** Tu "neo" cua tung loai can cu - ly do dan can cu nao thi phai nhac toi chinh can cu do. */
    private static final Map<String, List<String>> EVIDENCE_ANCHORS = Map.of(
            "quyetDinh", List.of("quyet dinh", "qd"),
            "congViec", List.of("cong viec", "cbkt", "thkt", "dckt", "tien do"),
            "ttss", List.of("ttss", "sai sot", "ton tai", "trong yeu"),
            "kienNghi", List.of("kien nghi"),
            "mucTieu", List.of("muc tieu"),
            "phamVi", List.of("pham vi"));

    /** Ly do phai nhac toi can cu da dan (tu neo hoac 1 gia tri cu the nhu so QD/ngay) - chan kieu dan "quyet dinh" nhung
     * ly do lai noi "da thay bien ban" (khong co trong ho so). */
    public static boolean mentionsEvidence(String reason, String evidenceKey, Object evidenceValue) {
        if (reason == null || evidenceKey == null) {
            return false;
        }
        String r = AgentText.normalize(reason);
        List<String> anchors = evidenceKey.startsWith("mocThoiGian.")
                ? List.of("thoi gian", "thoi han", "ngay", "chuan bi", "thuc dia", "bao cao", "lich")
                : EVIDENCE_ANCHORS.getOrDefault(evidenceKey, List.of());
        if (anchors.stream().anyMatch(a -> AgentText.containsPhrase(r, a))) {
            return true;
        }
        // hoac trich dung 1 gia tri cu the cua can cu (so quyet dinh, ngay...)
        return evidenceValue != null && AgentText.tokens(String.valueOf(evidenceValue)).stream()
                .filter(t -> t.length() >= 4 && t.chars().anyMatch(Character::isDigit))
                .anyMatch(t -> AgentText.tokens(reason).contains(t));
    }

    /** Ly do phai co it nhat 1 tu noi dung chung voi chinh tieu chi - chan kieu lay 1 can cu chung ("da co quyet dinh")
     * de ket luan cho moi tieu chi khong lien quan. */
    public static boolean relevantTo(String reason, String criterion) {
        if (reason == null || criterion == null) {
            return false;
        }
        Set<String> criterionWords = AgentText.tokens(criterion).stream().filter(w -> w.length() >= 2 && !GENERIC_WORDS.contains(w))
                .collect(java.util.stream.Collectors.toSet());
        return AgentText.tokens(reason).stream().anyMatch(w -> w.length() >= 2 && !GENERIC_WORDS.contains(w) && criterionWords.contains(w));
    }

    /** Ly do kieu "chua co thong tin" = thieu du lieu, khong phai can cu de ket luan. */
    public static boolean claimsMissingData(String reason) {
        return reason != null && MISSING_DATA.matcher(AgentText.normalize(reason)).find();
    }

    private static ToolSpec toolSpec(Set<String> evidenceKeys) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", Map.of(
                "key", Map.of("type", "string", "description", "Ma tieu chi (key) trong tieuChi"),
                "suggestion", Map.of("type", "string", "enum", List.of("COMPLIANT", "NON_COMPLIANT", "APPLIES", "NOT_APPLIES", "NEED_REVIEW")),
                "violationCount", Map.of("type", "integer", "description", "PL01F loi tru diem: so loi uoc tinh"),
                "reason", Map.of("type", "string", "description", "Toi da 25 tu, can cu tu hoSoDoan"),
                "evidence", Map.of("type", "string", "enum", List.copyOf(evidenceKeys), "description", "Ten muc trong canCu chung minh goi y")));
        item.put("required", List.of("key", "suggestion", "evidence", "reason"));
        return new ToolSpec(TOOL, "Nop goi y cham DGCL tung tieu chi",
                Map.of("type", "object", "properties", Map.of(
                                "items", Map.of("type", "array", "items", item),
                                "overall", Map.of("type", "string", "description", "Nhan xet chung")),
                        "required", List.of("items")));
    }

    private static Integer positiveInt(Object v) {
        if (v == null) {
            return null;
        }
        try {
            int n = v instanceof Number num ? num.intValue() : Integer.parseInt(String.valueOf(v).trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() > CONTENT ? t.substring(0, CONTENT) + "..." : t;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
