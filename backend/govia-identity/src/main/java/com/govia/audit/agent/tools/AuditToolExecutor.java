package com.govia.audit.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.tools.controller.AuditToolsController;
import com.govia.core.web.BusinessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    private final AuditToolsController controller;
    private final ObjectMapper objectMapper;

    public AuditToolExecutor(AuditToolsController controller, ObjectMapper objectMapper) {
        this.controller = controller;
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
            default -> null;
        };
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
