package com.govia.audit.agent.service;

import com.govia.audit.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Danh sach agent dang co. Giai doan G1: A0 (tro ly chung - viec cua toi, tim man hinh, tim van ban
 * quy dinh) va A1 (rui ro). Cac agent G2-G4 (A2..A7) them vao day kem bo tool rieng, khong sua
 * AgentOrchestratorService.
 */
@Component
public class AgentProfileRegistry {

    public static final String GENERAL = "A0";
    public static final String RISK = "A1";

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
                    Cau hoi ve diem/xep loai rui ro chi nhanh, phat hien kiem toan: noi nguoi dung hoi lai \
                    cu the hon (vd neu ten chi nhanh va nam) de Tro ly Rui ro (A1) tra loi - KHONG tu doan so lieu.
                    """,
                    List.of("get_my_tasks", "search_screens", "search_documents"),
                    List.of("viec cua toi", "viec can xu ly", "can xu ly", "nhiem vu", "task", "phe duyet", "cho duyet",
                            "man hinh", "chuc nang", "menu", "tai lieu", "van ban", "quy dinh", "quy che", "thu vien",
                            "huong dan su dung"),
                    List.of("/workflow")),
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
