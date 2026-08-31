package com.govia.audit.agent.tools;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ban Java cua docs/kien-truc-ky-thuat/audit-tools-contract.md - KHONG parse lai file .md, chi khai
 * bao tay tung tool khop 1-1 voi contract (ten, input schema, endpoint). Neu sua 1 trong 2 cho
 * (contract .md hoac registry nay) PHAI sua ca cho kia theo cho khop.
 *
 * <p>Rieng {@link #FINAL_ANSWER_TOOL_NAME} khong nam trong 10 Audit Tools that - day la "tool" gia
 * AgentOrchestratorService dung de bat model tra loi cuoi theo dung schema FACT/ANALYSIS/
 * RECOMMENDATION/EVIDENCE thay vi tra text tu do (xem AgentOrchestratorService).
 */
@Component
public class AuditToolRegistry {

    public static final String FINAL_ANSWER_TOOL_NAME = "submit_final_answer";

    private final List<AuditToolDefinition> definitions = List.of(
            def("get_branch_risk",
                    "Lay diem rui ro tong hop va muc xep loai (rankLabel) cua 1 chi nhanh trong 1 nam. "
                            + "Tra ve null neu chi nhanh chua co diem cham cho nam do - KHONG phai loi.",
                    schema(Map.of(
                            "branchCode", prop("string", "Ma chi nhanh"),
                            "year", prop("integer", "Nam can xem, vi du 2025")
                    ), List.of("branchCode", "year")),
                    "/api/audit/tools/branch-risk"),

            def("get_branch_details",
                    "Lay thong tin ho so cua 1 chi nhanh/don vi: ngay thanh lap, nhan su, chuc nang, "
                            + "phat hien trong yeu (text tu do, khong phai danh sach audit finding co cau truc).",
                    schema(Map.of(
                            "branchCode", prop("string", "Ma chi nhanh")
                    ), List.of("branchCode")),
                    "/api/audit/tools/branch-details"),

            def("get_risk_breakdown",
                    "Lay chi tiet diem rui ro cua 1 chi nhanh/nam theo tung nghiep vu, tung chi tieu dinh "
                            + "luong, va tung nhom cap 2 dinh tinh. Dung tool nay khi can giai thich TAI SAO 1 "
                            + "chi nhanh co diem/xep loai nhu vay.",
                    schema(Map.of(
                            "branchCode", prop("string", "Ma chi nhanh"),
                            "year", prop("integer", "Nam can xem")
                    ), List.of("branchCode", "year")),
                    "/api/audit/tools/risk-breakdown"),

            def("compare_branches",
                    "So sanh diem rui ro tong hop cua 2 hoac nhieu chi nhanh trong cung 1 nam.",
                    schema(Map.of(
                            "branchCodes", arrayProp("string", "Danh sach ma chi nhanh can so sanh, it nhat 2 ma"),
                            "year", prop("integer", "Nam can so sanh")
                    ), List.of("branchCodes", "year")),
                    "/api/audit/tools/compare-branches"),

            def("list_branches",
                    "Tim danh sach chi nhanh/don vi theo loai don vi va/hoac tu khoa. Dung tool nay khi "
                            + "chua biet ma chi nhanh chinh xac (vd nguoi dung goi ten khong day du).",
                    schema(Map.of(
                            "unitType", prop("string", "Ma loai don vi (danh muc UNIT_TYPE), vi du CN/HO/GSCC"),
                            "search", prop("string", "Tu khoa tim theo ma hoac ten chi nhanh"),
                            "activeOnly", prop("boolean", "true = chi lay don vi dang hieu luc")
                    ), List.of()),
                    "/api/audit/tools/branches"),

            def("get_risk_history",
                    "Xem lich su diem rui ro cua 1 chi nhanh qua nhieu nam, de biet xu huong tang/giam.",
                    schema(Map.of(
                            "branchCode", prop("string", "Ma chi nhanh"),
                            "fromYear", prop("integer", "Nam bat dau (tuy chon)"),
                            "toYear", prop("integer", "Nam ket thuc (tuy chon)")
                    ), List.of("branchCode")),
                    "/api/audit/tools/risk-history"),

            def("get_risk_criteria",
                    "Xem dinh nghia cac tieu chi cham diem rui ro (dinh luong/dinh tinh/tieu chi DGRR khac).",
                    schema(Map.of(
                            "kind", enumProp("Loai tieu chi can xem", "quantitative", "qualitative", "other")
                    ), List.of("kind")),
                    "/api/audit/tools/risk-criteria"),

            def("get_audit_findings",
                    "Lay danh sach cac phat hien kiem toan (audit finding) da duoc ghi nhan, loc theo chi "
                            + "nhanh/khoang ngay/muc do neu can. Mang rong nghia la CHUA CO phat hien nao duoc "
                            + "ghi nhan cho dieu kien do - phai noi ro, khong duoc suy dien.",
                    schema(Map.of(
                            "branchCode", prop("string", "Ma chi nhanh (tuy chon)"),
                            "fromDate", prop("string", "Ngay bat dau, dinh dang YYYY-MM-DD (tuy chon)"),
                            "toDate", prop("string", "Ngay ket thuc, dinh dang YYYY-MM-DD (tuy chon)"),
                            "severity", prop("string", "Ma muc do (danh muc RISK_LEVEL), tuy chon")
                    ), List.of()),
                    "/api/audit/tools/audit-findings"),

            def("get_top_risk_branches",
                    "Tim N chi nhanh co diem rui ro cao nhat trong 1 nam.",
                    schema(Map.of(
                            "year", prop("integer", "Nam can xem"),
                            "limit", prop("integer", "So luong chi nhanh muon lay, mac dinh 10"),
                            "unitType", prop("string", "Loc theo loai don vi, tuy chon")
                    ), List.of("year")),
                    "/api/audit/tools/top-risk-branches"),

            def("get_evidence",
                    "Lay danh sach file chung minh (evidence) da dinh kem cho 1 phat hien kiem toan cu the. "
                            + "findingId phai la id lay tu ket qua cua get_audit_findings, khong tu bia.",
                    schema(Map.of(
                            "findingId", prop("string", "UUID cua audit finding, lay tu get_audit_findings")
                    ), List.of("findingId")),
                    "/api/audit/tools/evidence")
    );

    private final AuditToolDefinition finalAnswerTool = def(
            FINAL_ANSWER_TOOL_NAME,
            "Goi tool nay DE KET THUC va tra loi nguoi dung - KHONG tra loi bang text tu do. "
                    + "'facts' chi chua du lieu lay truc tiep tu cac tool da goi (co the trich dan so lieu). "
                    + "'analysis' la nhan dinh dua tren facts. 'recommendations' la de xuat cua ban, khong "
                    + "duoc trinh bay lan voi facts. 'evidence' liet ke moi tool da dung de tra loi, kem "
                    + "tham so va du lieu chinh da lay duoc - moi phan tu PHAI ung voi 1 tool THAT SU da goi.",
            schema(Map.of(
                    "answer", prop("string", "Cau tra loi ngan gon, tu nhien cho nguoi dung"),
                    "facts", arrayProp("string", "Danh sach du kien lay truc tiep tu tool, khong suy dien"),
                    "analysis", arrayProp("string", "Phan tich dua tren facts"),
                    "recommendations", arrayProp("string", "De xuat cua ban - co the rong neu khong can"),
                    "evidence", evidenceArrayProp()
            ), List.of("answer", "facts")),
            null);

    public List<AuditToolDefinition> auditTools() {
        return definitions;
    }

    public AuditToolDefinition finalAnswerTool() {
        return finalAnswerTool;
    }

    public List<AuditToolDefinition> allDefinitions() {
        return java.util.stream.Stream.concat(definitions.stream(), java.util.stream.Stream.of(finalAnswerTool)).toList();
    }

    private static AuditToolDefinition def(String name, String description, Map<String, Object> schema, String path) {
        return new AuditToolDefinition(name, description, schema, path);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        if (!required.isEmpty()) {
            s.put("required", required);
        }
        return s;
    }

    private static Map<String, Object> prop(String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    private static Map<String, Object> arrayProp(String itemType, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "array");
        p.put("items", Map.of("type", itemType));
        p.put("description", description);
        return p;
    }

    private static Map<String, Object> enumProp(String description, String... values) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "string");
        p.put("description", description);
        p.put("enum", List.of(values));
        return p;
    }

    private static Map<String, Object> evidenceArrayProp() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", Map.of(
                "tool", prop("string", "Ten tool THAT SU da goi de lay du lieu nay"),
                "args", Map.of("type", "object", "description", "Tham so da dung khi goi tool do"),
                "keyData", Map.of("type", "object", "description", "So lieu chinh lay duoc tu tool do")
        ));
        item.put("required", List.of("tool"));
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "array");
        p.put("items", item);
        p.put("description", "Danh sach evidence - moi phan tu phai ung voi 1 tool that su da goi trong luot nay");
        return p;
    }
}
