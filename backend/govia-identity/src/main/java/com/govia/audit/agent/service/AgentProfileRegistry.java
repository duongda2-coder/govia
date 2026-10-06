package com.govia.audit.agent.service;

import com.govia.audit.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Danh sach agent dang co: G1 = A0 (tro ly chung) + A1 (rui ro); G2 = A4 (phat hien & kien nghi), A3
 * (tac nghiep KT), A7 (danh muc). Thu tu trong danh sach = thu tu uu tien khi AgentRouter cham diem
 * hoa nhau (A1 dung cuoi vi tu khoa "chi nhanh", "diem" rat chung). Them agent G3+ chi can them vao
 * day kem bo tool rieng, khong sua AgentOrchestratorService.
 */
@Component
public class AgentProfileRegistry {

    public static final String GENERAL = "A0";
    public static final String RISK = "A1";
    public static final String EXECUTION = "A3";
    public static final String FINDING = "A4";
    public static final String CATALOG = "A7";

    private final AgentProperties agentProperties;

    private final List<AgentProfile> profiles = List.of(
            new AgentProfile(GENERAL, "Trợ lý chung",
                    "Việc cần xử lý của tôi, tìm màn hình/chức năng, tra cứu văn bản trong Thư viện tài liệu",
                    """
                    Pham vi cua ban (A0 - Tro ly chung):
                    - Liet ke viec dang cho nguoi dung xu ly trong quy trinh phe duyet (tool get_my_tasks).
                    - Chi cho nguoi dung man hinh/chuc nang nam o dau tren menu (tool search_screens) - chi \
                      duoc nhac man hinh co trong ket qua tool, kem duong dan "path" de nguoi dung bam vao.
                    - Tra cuu van ban, quy dinh noi bo trong Thu vien tai lieu (tool search_documents) - luon \
                      neu so hieu + ten van ban lam can cu, noi ro neu van ban da het hieu luc.
                    Cau hoi ve diem rui ro, cuoc kiem toan, TTSS/kien nghi hay danh muc: noi nguoi dung hoi lai \
                    cu the hon (vd neu ma chi nhanh/ma cuoc kiem toan) de tro ly chuyen trach tra loi - KHONG tu doan so lieu.
                    """,
                    List.of("get_my_tasks", "search_screens", "search_documents"),
                    List.of("viec cua toi", "viec can xu ly", "can xu ly", "nhiem vu cua toi", "task", "can toi duyet",
                            "cho toi duyet", "man hinh", "chuc nang", "menu", "tai lieu", "van ban", "quy dinh", "quy che",
                            "thu vien", "huong dan su dung"),
                    List.of("/workflow")),
            new AgentProfile(FINDING, "Trợ lý Phát hiện & Kiến nghị",
                    "Tổng hợp TTSS của cuộc kiểm toán, phát hiện trọng yếu, kiến nghị đã tạo, phát hiện/kiến nghị tương tự ở các cuộc khác",
                    """
                    Pham vi cua ban (A4 - Tro ly Phat hien & Kien nghi): ton tai sai sot (TTSS) va kien nghi cua \
                    cac cuoc kiem toan (CKT) nguoi dung duoc phan cong. Chua biet ma CKT thi goi list_my_engagements \
                    truoc. Tong quan dung get_ttss_summary; chi tiet dung get_ttss_records; kien nghi da tao dung \
                    list_engagement_recommendations; tham khao cach viet o CKT khac dung search_similar_findings.
                    Khi duoc nho goi y noi dung kien nghi: viet ngan gon, cu the, neu ro don vi/bo phan can khac phuc, \
                    viec can lam va can cu tu TTSS - dat trong "recommendations" va noi ro day la BAN NHAP de kiem toan \
                    vien xem xet. Ban KHONG tao/luu kien nghi - nguoi dung tu bam "Them" tren man hinh.
                    """,
                    List.of("list_my_engagements", "get_ttss_summary", "get_ttss_records", "list_engagement_recommendations",
                            "search_similar_findings"),
                    List.of("ttss", "ton tai", "sai sot", "ton tai sai sot", "kien nghi", "soan kien nghi", "phat hanh",
                            "bao cao kiem toan", "trong yeu", "phat hien tuong tu", "lap lai"),
                    List.of("/audit/plan/execution/work-management/ttss", "/audit/phbc")),
            new AgentProfile(EXECUTION, "Trợ lý Tác nghiệp KT",
                    "Cuộc kiểm toán được phân công, công việc và tiến độ CBKT/THKT/DCKT, điểm kiểm soát, thủ tục, bước quy trình, loại ngoại lệ",
                    """
                    Pham vi cua ban (A3 - Tro ly Tac nghiep kiem toan): cuoc kiem toan nguoi dung duoc phan cong \
                    (list_my_engagements), cong viec va tien do theo giai doan CBKT/THKT/DCKT (get_engagement_work_items), \
                    tra cuu diem kiem soat, thu tuc kiem toan, buoc quy trinh, loai ngoai le va mapping ngoai le \
                    (search_catalog - ban chi nhanh khong co hau to, ban quy trinh co hau to _qt).
                    Goi y thu tuc/diem kiem soat chi duoc lay tu ket qua search_catalog, kem ma de nguoi dung doi chieu.
                    """,
                    List.of("list_my_engagements", "get_engagement_work_items", "search_catalog"),
                    List.of("cuoc kiem toan nao", "dot kiem toan nao", "tham gia", "duoc phan cong", "cong viec", "phan cong", "tien do", "diem kiem soat",
                            "thu tuc kiem toan", "thu tuc", "buoc quy trinh", "loai ngoai le", "ngoai le", "doan kiem toan",
                            "truong doan", "thuc dia", "cbkt", "thkt", "dckt"),
                    List.of("/audit/plan/execution", "/audit/plan/engagement")),
            new AgentProfile(CATALOG, "Trợ lý Danh mục",
                    "Tra cứu và rà soát trùng lặp trong các danh mục kiểm toán (điểm kiểm soát, công việc, loại ngoại lệ, bước quy trình)",
                    """
                    Pham vi cua ban (A7 - Tro ly Danh muc): tra cuu dong trong cac danh muc kiem toan (search_catalog) \
                    va phat hien dong co ten trung lap (find_catalog_duplicates) de nguoi quan tri ra soat. Ban chi \
                    goi y - KHONG sua/xoa danh muc; nguoi dung tu xu ly tren man hinh danh muc.
                    """,
                    List.of("search_catalog", "find_catalog_duplicates"),
                    List.of("danh muc", "trung lap", "bi trung", "trung ten", "catalog", "ra soat danh muc", "chuan hoa danh muc"),
                    List.of("/audit/master-data", "/audit/plan/master-data")),
            new AgentProfile(RISK, "Trợ lý Rủi ro",
                    "Giải thích điểm/xếp loại rủi ro, so sánh chi nhánh, biến động giữa các năm, xếp hạng chuyên gia, phát hiện kiểm toán",
                    """
                    Pham vi cua ban (A1 - Tro ly Rui ro): diem va xep loai rui ro chi nhanh/don vi, chi tiet \
                    diem theo nghiep vu/tieu chi, so sanh chi nhanh, lich su va bien dong diem giua cac nam \
                    (get_score_changes), ket qua xep hang lai cua chuyen gia (get_expert_rank_overrides), \
                    tieu chi cham diem, phat hien kiem toan va file chung minh.
                    Voi cau hoi "tai sao" 1 chi nhanh co rui ro cao: goi get_branch_risk roi get_risk_breakdown.
                    """,
                    List.of("get_branch_risk", "get_branch_details", "get_risk_breakdown", "compare_branches",
                            "list_branches", "get_risk_history", "get_risk_criteria", "get_audit_findings",
                            "get_top_risk_branches", "get_evidence", "get_score_changes", "get_expert_rank_overrides"),
                    List.of("rui ro", "diem", "cham diem", "xep loai", "xep hang", "chi nhanh", "hsrr", "phat hien",
                            "finding", "tieu chi", "chuyen gia", "so sanh", "bien dong", "evidence", "bang chung", "top"),
                    List.of("/audit/risk-scoring")));

    public AgentProfileRegistry(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public List<AgentProfile> all() {
        return profiles;
    }

    public AgentProfile general() {
        return profiles.get(0);
    }

    public Optional<AgentProfile> find(String code) {
        return profiles.stream().filter(p -> p.code().equalsIgnoreCase(code)).findFirst();
    }

    /** A0 luon bat khi AI bat; agent chuyen trach co the tat rieng qua govia.agent.disabled-agents. */
    public boolean isEnabled(AgentProfile profile) {
        return GENERAL.equals(profile.code()) ? agentProperties.isEnabled() : agentProperties.isAgentEnabled(profile.code());
    }
}
