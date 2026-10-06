package com.govia.audit.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.controller.AgentToolsController;
import com.govia.audit.agent.dto.AgentScreenRef;
import com.govia.audit.agent.service.AgentRequestContext;
import com.govia.audit.agent.service.AgentText;
import com.govia.audit.tools.controller.AuditToolsController;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.BusinessException;
import com.govia.identity.workflow.controller.WorkflowTaskController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thuc thi 1 Audit Tool bang cach goi THANG method cua {@link AuditToolsController} (KHONG qua
 * HTTP loopback). "@PreAuthorize" tren controller la 1 AOP proxy cua Spring Method Security - no
 * van duoc thuc thi khi goi method Java truc tiep tren bean do, chi can SecurityContextHolder cua
 * THREAD HIEN TAI da duoc JwtAuthenticationFilter dien dung tu request chat goc (dung 1 thread dong
 * bo tu dau den cuoi, khong co goi async) - vi vay agent van di qua CHINH XAC 1 con duong bao mat
 * voi moi client khac, khong co code kiem tra quyen rieng nao o day, khong co duong tat, khong can
 * mo socket/cong HTTP nao them (tranh cac han che loopback socket cua moi truong chay).
 */
@Component
public class AuditToolExecutor {

    private static final int MAX_SCREEN_HITS = 8;

    private final AuditToolsController controller;
    private final AgentToolsController agentToolsController;
    private final WorkflowTaskController workflowTaskController;
    private final ObjectMapper objectMapper;

    public AuditToolExecutor(AuditToolsController controller, AgentToolsController agentToolsController,
                             WorkflowTaskController workflowTaskController, ObjectMapper objectMapper) {
        this.controller = controller;
        this.agentToolsController = agentToolsController;
        this.workflowTaskController = workflowTaskController;
        this.objectMapper = objectMapper;
    }

    public ToolExecutionResult execute(String toolName, Map<String, Object> arguments) {
        try {
            Object response = dispatch(toolName, arguments);
            if (response == null) {
                return ToolExecutionResult.error("Khong ton tai tool ten '" + toolName + "'");
            }
            return ToolExecutionResult.success(objectMapper.writeValueAsString(response));
        } catch (AccessDeniedException e) {
            return ToolExecutionResult.forbidden("Khong co quyen truy cap du lieu cua tool '" + toolName + "'");
        } catch (BusinessException e) {
            return ToolExecutionResult.error("Tool '" + toolName + "' bao loi: " + e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ToolExecutionResult.error("Tham so khong hop le cho tool '" + toolName + "': " + e.getMessage());
        } catch (Exception e) {
            return ToolExecutionResult.error("Khong goi duoc tool '" + toolName + "': " + e.getMessage());
        }
    }

    private Object dispatch(String toolName, Map<String, Object> a) {
        return switch (toolName) {
            case "get_branch_risk" -> controller.getBranchRisk(requireString(a, "branchCode"), requireInt(a, "year"));
            case "get_branch_details" -> controller.getBranchDetails(requireString(a, "branchCode"));
            case "get_risk_breakdown" -> controller.getRiskBreakdown(requireString(a, "branchCode"), requireInt(a, "year"));
            case "compare_branches" -> controller.compareBranches(requireStringList(a, "branchCodes"), requireInt(a, "year"));
            case "list_branches" -> controller.listBranches(optString(a, "unitType"), optString(a, "search"), optBoolean(a, "activeOnly"));
            case "get_risk_history" -> controller.getRiskHistory(requireString(a, "branchCode"), optInt(a, "fromYear"), optInt(a, "toYear"));
            case "get_risk_criteria" -> controller.getRiskCriteria(requireString(a, "kind"));
            case "get_audit_findings" -> controller.getAuditFindings(optString(a, "branchCode"), optDate(a, "fromDate"), optDate(a, "toDate"), optString(a, "severity"));
            case "get_top_risk_branches" -> controller.getTopRiskBranches(requireInt(a, "year"), optInt(a, "limit"), optString(a, "unitType"));
            case "get_evidence" -> controller.getEvidence(requireUuid(a, "findingId"));
            case "get_score_changes" -> controller.getScoreChanges(requireInt(a, "year"), optInt(a, "compareYear"), optString(a, "direction"), optInt(a, "limit"));
            case "get_expert_rank_overrides" -> controller.getExpertRankOverrides(requireInt(a, "year"), optBoolean(a, "onlyChanged"));
            case "get_my_tasks" -> workflowTaskController.myTasks(currentPrincipal());
            case "search_screens" -> searchScreens(requireString(a, "query"));
            case "search_documents" -> agentToolsController.searchDocuments(requireString(a, "query"), optInt(a, "limit"), optBoolean(a, "includeExpired"));
            default -> null;
        };
    }

    /** Man hinh lay tu danh sach menu frontend gui kem request (da loc theo quyen cua chinh nguoi dung), xep
     * theo so tu khoa trung voi ten man hinh + nhom menu. */
    private List<Map<String, Object>> searchScreens(String query) {
        List<String> words = AgentText.tokens(query).stream().filter(w -> w.length() >= 2).distinct().toList();
        record Hit(AgentScreenRef screen, long score) {
        }
        return AgentRequestContext.screens().stream()
                .map(s -> {
                    String text = AgentText.normalize(s.label() + " " + s.group());
                    return new Hit(s, words.stream().filter(w -> AgentText.containsPhrase(text, w)).count());
                })
                .filter(h -> h.score() > 0)
                .sorted(Comparator.comparingLong(Hit::score).reversed())
                .limit(MAX_SCREEN_HITS)
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("label", h.screen().label());
                    m.put("group", h.screen().group());
                    m.put("path", h.screen().path());
                    return m;
                })
                .toList();
    }

    private CurrentUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new AccessDeniedException("Khong xac dinh duoc nguoi dung hien tai");
        }
        return principal;
    }

    private String requireString(Map<String, Object> a, String key) {
        Object v = a.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("Thieu tham so bat buoc '" + key + "'");
        }
        return String.valueOf(v);
    }

    private String optString(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Integer requireInt(Map<String, Object> a, String key) {
        Integer v = optInt(a, key);
        if (v == null) {
            throw new IllegalArgumentException("Thieu tham so bat buoc '" + key + "'");
        }
        return v;
    }

    private Integer optInt(Map<String, Object> a, String key) {
        Object v = a.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v).trim());
    }

    private Boolean optBoolean(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? null : Boolean.parseBoolean(String.valueOf(v));
    }

    private LocalDate optDate(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null || String.valueOf(v).isBlank() ? null : LocalDate.parse(String.valueOf(v));
    }

    private UUID requireUuid(Map<String, Object> a, String key) {
        return UUID.fromString(requireString(a, key));
    }

    private List<String> requireStringList(Map<String, Object> a, String key) {
        Object v = a.get(key);
        if (!(v instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("Thieu tham so bat buoc '" + key + "' (dang danh sach)");
        }
        return list.stream().map(String::valueOf).toList();
    }
}
