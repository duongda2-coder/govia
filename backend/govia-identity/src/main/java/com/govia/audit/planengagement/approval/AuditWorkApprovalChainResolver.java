package com.govia.audit.planengagement.approval;

import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.identity.entity.ApprovalMatrixRule;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.service.ApprovalMatrixRuleService;
import com.govia.identity.workflow.approval.ManagerHierarchyApprovalChainResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Hàm "get dây phê duyệt" riêng cho khối "Quản lý công việc" (CBKT/THKT): mặc định CHỈ 1 bước -
 * đúng người là trưởng đoàn của cuộc kiểm toán ({@link AuditEngagement#getTeamLeadEmployeeId()}),
 * đúng đặc tả "Trưởng đoàn có quyền phê duyệt". Viết dưới dạng resolver riêng (không hard-code 1
 * bước ở service gọi) và tra {@link ApprovalMatrixRule} (domain "AUDIT_WORKITEM") trước khi
 * fallback, để sau này thêm bước duyệt thứ 2 (vd Phó KTNB cho cuộc kiểm toán rủi ro cao) chỉ cần
 * thêm 1 dòng cấu hình ma trận phê duyệt cho domain này - KHÔNG phải sửa code/BPMN.
 */
@Component
public class AuditWorkApprovalChainResolver {

    private static final String DOMAIN = "AUDIT_WORKITEM";

    private final ApprovalMatrixRuleService approvalMatrixRuleService;
    private final ManagerHierarchyApprovalChainResolver managerHierarchyApprovalChainResolver;
    private final UserAccountRepository userAccountRepository;

    public AuditWorkApprovalChainResolver(ApprovalMatrixRuleService approvalMatrixRuleService,
                                           ManagerHierarchyApprovalChainResolver managerHierarchyApprovalChainResolver,
                                           UserAccountRepository userAccountRepository) {
        this.approvalMatrixRuleService = approvalMatrixRuleService;
        this.managerHierarchyApprovalChainResolver = managerHierarchyApprovalChainResolver;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Chưa cấu hình {@link ApprovalMatrixRule} nào cho domain "AUDIT_WORKITEM" (org unit của cuộc
     * kiểm toán, hoặc quy tắc mặc định): trả về đúng 1 người duyệt = tài khoản của trưởng đoàn.
     * Nếu ĐÃ cấu hình (chuẩn bị cho tương lai): đi ngược chuỗi quản lý bắt đầu từ trưởng đoàn tới
     * ngưỡng cấp bậc đã cấu hình, dùng lại {@link ManagerHierarchyApprovalChainResolver} - dây có
     * thể dài hơn 1 bước mà không cần đổi code gọi.
     */
    public List<UUID> resolveChain(AuditEngagement engagement) {
        UUID tenantId = engagement.getTenantId();
        ApprovalMatrixRule rule = approvalMatrixRuleService.resolveActiveRuleForOrgUnit(tenantId, DOMAIN, engagement.getAuditObjectUnitId());
        if (rule == null) {
            return userAccountRepository.findByEmployeeId(engagement.getTeamLeadEmployeeId())
                    .map(account -> List.of(account.getId()))
                    .orElse(List.of());
        }
        return managerHierarchyApprovalChainResolver.resolveChain(engagement.getTeamLeadEmployeeId(), rule.getFinalApprovalLevel());
    }
}
