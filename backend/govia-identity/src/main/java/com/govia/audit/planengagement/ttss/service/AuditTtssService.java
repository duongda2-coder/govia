package com.govia.audit.planengagement.ttss.service;

import com.govia.audit.exceptiontype.entity.AuditExceptionType;
import com.govia.audit.exceptiontype.repository.AuditExceptionTypeRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.approval.AuditWorkApprovalChainResolver;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.progressreport.service.AuditProgressReportService;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementAssignmentRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.dto.AuditTtssApproveRecommendationsRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssLinkRecommendationRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.processstep.entity.AuditProcessStepDetail;
import com.govia.audit.processstep.entity.AuditProcessStepSummary;
import com.govia.audit.processstep.repository.AuditProcessStepDetailRepository;
import com.govia.audit.processstep.repository.AuditProcessStepSummaryRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.workflow.dto.CompleteTaskRequest;
import com.govia.identity.workflow.service.WorkflowTaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Quản lý TTSS & Kiến nghị" (Khối C, sheet "Quản lý công việc" trong Tạo CKT (1).xlsx, mục C).
 * "Download Template"/"Upload file TTSS" xuat 1 dong cho MOI cong viec da phan cong (khong tu dem
 * so mau o 16 bang chon mau CmNtd1..14/CmTd1/2 - xem ghi chu trong plan). Moi lan upload UPSERT
 * theo khoa tu nhien (workItemCode + processStepDetailId + findingCode + referenceNumber +
 * referenceNumber2 - xem uploadKey()) va tu dong sinh 1 Báo cáo tiến độ
 * (AuditProgressReportService.recordUpload).
 */
@Service
public class AuditTtssService {

    private static final String PROCESS_KEY = "audit_recommendation_approval";
    private static final String ATTACHMENT_ENTITY_NAME = "AUDIT_PROGRESS_REPORT";
    /** Dung chung voi AuditEngagementMonitoringService - quyen "thay tat ca" bo qua scoping theo
     * truong doan/truong nhom (vd cho vai tro giam sat/QA ngoai doan kiem toan). */
    private static final String PERMISSION_VIEW_ALL = "AUDIT.PLAN_ENGAGEMENT.VIEW_ALL";
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditTtssRecordRepository ttssRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditEngagementGroupMemberRepository memberRepository;
    private final AuditEngagementAssignmentRepository assignmentRepository;
    private final AuditWorkItemRepository workItemRepository;
    private final AuditObjectUnitRepository objectUnitRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditProcessStepSummaryRepository processStepSummaryRepository;
    private final AuditProcessStepDetailRepository processStepDetailRepository;
    private final AuditExceptionTypeRepository exceptionTypeRepository;
    private final AuditRecommendationRepository recommendationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditProgressReportService progressReportService;
    private final AuditWorkApprovalChainResolver approvalChainResolver;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WorkflowTaskService workflowTaskService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;
    private final AuditLogService auditLogService;
    private final AuditTtssSampleSelectionResolver sampleSelectionResolver;

    public AuditTtssService(AuditTtssRecordRepository ttssRepository, AuditEngagementRepository engagementRepository,
                             AuditEngagementGroupRepository groupRepository, AuditEngagementGroupMemberRepository memberRepository,
                             AuditEngagementAssignmentRepository assignmentRepository, AuditWorkItemRepository workItemRepository,
                             AuditObjectUnitRepository objectUnitRepository, AuditMasterDataItemRepository masterDataItemRepository,
                             AuditProcessStepSummaryRepository processStepSummaryRepository, AuditProcessStepDetailRepository processStepDetailRepository,
                             AuditExceptionTypeRepository exceptionTypeRepository,
                             AuditRecommendationRepository recommendationRepository, EmployeeRepository employeeRepository,
                             UserAccountRepository userAccountRepository, AuditProgressReportService progressReportService,
                             AuditWorkApprovalChainResolver approvalChainResolver, RuntimeService runtimeService, TaskService taskService,
                             WorkflowTaskService workflowTaskService, ExcelExportService excelExportService, ExcelImportService excelImportService,
                             AuditLogService auditLogService, AuditTtssSampleSelectionResolver sampleSelectionResolver) {
        this.ttssRepository = ttssRepository;
        this.engagementRepository = engagementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.objectUnitRepository = objectUnitRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.processStepSummaryRepository = processStepSummaryRepository;
        this.processStepDetailRepository = processStepDetailRepository;
        this.exceptionTypeRepository = exceptionTypeRepository;
        this.recommendationRepository = recommendationRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.progressReportService = progressReportService;
        this.approvalChainResolver = approvalChainResolver;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.workflowTaskService = workflowTaskService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
        this.auditLogService = auditLogService;
        this.sampleSelectionResolver = sampleSelectionResolver;
    }

    @Transactional(readOnly = true)
    public List<AuditTtssRecordResponse> list(UUID engagementId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        List<AuditTtssRecord> records = ttssRepository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId);
        return toResponses(tenantId, scopeByVisibility(tenantId, engagement, records, principal));
    }

    /** Ket qua phan quyen THEO DONG cho 1 nguoi dung tren 1 CKT - dung chung cho list()/delete()/
     * approveRecommendations() de dam bao xoa/duyet cung bi gioi han dung pham vi da thay o man
     * hinh danh sach (khong the xoa/duyet 1 dong minh khong duoc phep xem). */
    private record TtssVisibility(boolean seeAll, Set<String> visibleUsernames) {
        boolean canSee(AuditTtssRecord record) {
            return seeAll || (record.getRecordUsername() != null && visibleUsernames.contains(record.getRecordUsername()));
        }
    }

    /** Phan quyen THEO DONG du lieu (khac voi PERM_AUDIT.TTSS.VIEW - quyen do chi kiem soat viec vao
     * duoc man hinh). Ap dung cho "Cán bộ thực hiện" = {@link AuditTtssRecord#getRecordUsername()}:
     * - Truong doan ({@link AuditEngagement#getTeamLeadEmployeeId()}) hoac co quyen
     *   AUDIT.PLAN_ENGAGEMENT.VIEW_ALL: thay/xoa/duyet duoc TAT CA nghiep vu.
     * - Truong nhom ({@link AuditEngagementGroup#getLeaderEmployeeId()}): thay/xoa/duyet duoc dong
     *   cua CHINH MINH + cua moi thanh vien trong (cac) nhom minh lam truong nhom - CHI dung nhom
     *   minh phu trach, KHONG lan sang nhom khac (doi chieu qua UserAccount.username vi TTSS chi luu
     *   username, khong luu employeeId).
     * - Thanh vien thuong: chi thay/xoa duoc dong do CHINH MINH upload (recordUsername = username
     *   cua minh) - KHONG thay duoc dong cua thanh vien khac trong cung nhom. */
    private TtssVisibility resolveVisibility(UUID tenantId, AuditEngagement engagement, CurrentUserPrincipal principal) {
        if (principal == null || principal.username() == null) {
            return new TtssVisibility(false, Set.of());
        }
        if (principal.permissions() != null && principal.permissions().contains(PERMISSION_VIEW_ALL)) {
            return new TtssVisibility(true, Set.of());
        }
        UUID actorEmployeeId = principal.employeeCode() == null ? null
                : employeeRepository.findByTenantIdAndEmployeeCode(tenantId, principal.employeeCode()).map(Employee::getId).orElse(null);
        if (actorEmployeeId != null && actorEmployeeId.equals(engagement.getTeamLeadEmployeeId())) {
            return new TtssVisibility(true, Set.of());
        }

        Set<String> visibleUsernames = new HashSet<>();
        visibleUsernames.add(principal.username());
        if (actorEmployeeId != null) {
            List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagement.getId());
            List<UUID> ledGroupIds = groups.stream().filter(g -> actorEmployeeId.equals(g.getLeaderEmployeeId())).map(AuditEngagementGroup::getId).toList();
            if (!ledGroupIds.isEmpty()) {
                List<UUID> memberEmployeeIds = memberRepository.findByTenantIdAndGroupIdIn(tenantId, ledGroupIds).stream()
                        .map(AuditEngagementGroupMember::getEmployeeId).toList();
                userAccountRepository.findByEmployeeIdIn(memberEmployeeIds).forEach(account -> visibleUsernames.add(account.getUsername()));
            }
        }
        return new TtssVisibility(false, visibleUsernames);
    }

    private List<AuditTtssRecord> scopeByVisibility(UUID tenantId, AuditEngagement engagement, List<AuditTtssRecord> records,
                                                      CurrentUserPrincipal principal) {
        TtssVisibility visibility = resolveVisibility(tenantId, engagement, principal);
        return records.stream().filter(visibility::canSee).toList();
    }

    /** Ket qua phan quyen THEO DONG cho downloadTemplate() - cung 3 muc nhu {@link TtssVisibility}
     * (truong doan/VIEW_ALL thay tat ca, truong nhom thay nhom minh phu trach, thanh vien thuong chi
     * thay cua chinh minh) nhung xet theo employeeId cua thanh vien duoc PHAN CONG cong viec
     * (AuditEngagementGroupMember.employeeId qua AuditEngagementAssignment.groupMemberId), khac voi
     * TtssVisibility xet theo recordUsername cua dong TTSS DA UPLOAD - template chua co dong TTSS nao
     * de doc recordUsername, chi co danh sach phan cong. */
    private record AssignmentVisibility(boolean seeAll, Set<UUID> visibleEmployeeIds) {
        boolean canSee(UUID employeeId) {
            return seeAll || (employeeId != null && visibleEmployeeIds.contains(employeeId));
        }
    }

    private AssignmentVisibility resolveAssignmentVisibility(UUID tenantId, AuditEngagement engagement, CurrentUserPrincipal principal) {
        if (principal == null || principal.employeeCode() == null) {
            return new AssignmentVisibility(false, Set.of());
        }
        if (principal.permissions() != null && principal.permissions().contains(PERMISSION_VIEW_ALL)) {
            return new AssignmentVisibility(true, Set.of());
        }
        UUID actorEmployeeId = employeeRepository.findByTenantIdAndEmployeeCode(tenantId, principal.employeeCode()).map(Employee::getId).orElse(null);
        if (actorEmployeeId == null) {
            return new AssignmentVisibility(false, Set.of());
        }
        if (actorEmployeeId.equals(engagement.getTeamLeadEmployeeId())) {
            return new AssignmentVisibility(true, Set.of());
        }

        Set<UUID> visibleEmployeeIds = new HashSet<>();
        visibleEmployeeIds.add(actorEmployeeId);
        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagement.getId());
        List<UUID> ledGroupIds = groups.stream().filter(g -> actorEmployeeId.equals(g.getLeaderEmployeeId())).map(AuditEngagementGroup::getId).toList();
        if (!ledGroupIds.isEmpty()) {
            memberRepository.findByTenantIdAndGroupIdIn(tenantId, ledGroupIds)
                    .forEach(m -> visibleEmployeeIds.add(m.getEmployeeId()));
        }
        return new AssignmentVisibility(false, visibleEmployeeIds);
    }

    /** "1. Download template TTSS" - 1 dong cho TUNG dong mau DA UPLOAD (16 man hinh CmNtd/CmTd) MA
     * NGUOI TAI VE DUOC PHEP THAY (xem resolveAssignmentVisibility() - cung pham vi voi
     * list()/delete()/approveRecommendations() ben tren, tranh lo nghiep vu cua thanh vien/nhom khac).
     * Truoc day xuat 1 dong/cong viec da phan cong (chi tu dong dien du lieu KHI khop mau duy nhat) -
     * doi theo yeu cau nguoi dung 2026-09-08 ("chi lay cac cong viec da phan cong, chua lay o cac
     * mau da upload len"): 1 cong viec co 10 dong mau se ra 10 dong TTSS, moi dong dien day du tu
     * chinh dong mau do (khong can khop duy nhat nua). "Mã công việc" van chi dien duoc khi nguoi
     * duoc phan cong dong mau do CHI co DUNG 1 cong viec trong segment tuong ung (con lai de trong -
     * cung 1 ly do voi resolveUnique(): khong co khoa lien ket tin cay giua dong mau va cong viec). */
    @Transactional(readOnly = true)
    public byte[] downloadTemplate(UUID engagementId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditObjectUnit unit = objectUnitRepository.findById(engagement.getAuditObjectUnitId()).orElse(null);
        AssignmentVisibility visibility = resolveAssignmentVisibility(tenantId, engagement, principal);

        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
        Map<UUID, AuditEngagementGroupMember> membersById = members.stream().collect(Collectors.toMap(AuditEngagementGroupMember::getId, m -> m));
        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId,
                members.stream().map(AuditEngagementGroupMember::getId).toList());
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));

        // Khoa theo MA nghiep vu (segment CODE, khong phai id) - vi vong lap ben duoi duyet theo
        // SUPPORTED_SEGMENT_CODES (list ma cung, khong phai danh muc AuditMasterDataItem cua tenant
        // - xem ly do o javadoc downloadTemplate()), nen phai dich businessSegmentId cua work item
        // sang CODE qua segmentsById() thi moi khop duoc voi ma dang duyet.
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        Map<String, List<String>> workItemCodesByEmployeeSegment = new HashMap<>();
        for (AuditEngagementAssignment assignment : assignments) {
            AuditEngagementGroupMember member = membersById.get(assignment.getGroupMemberId());
            AuditWorkItem workItem = workItems.get(assignment.getWorkItemId());
            AuditMasterDataItem segment = workItem == null ? null : segments.get(workItem.getBusinessSegmentId());
            if (member == null || segment == null) {
                continue;
            }
            workItemCodesByEmployeeSegment
                    .computeIfAbsent(member.getEmployeeId() + "|" + segment.getCode(), k -> new ArrayList<>())
                    .add(workItem.getCode());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String segmentCode : sampleSelectionResolver.supportedSegmentCodes()) {
            for (AuditTtssSampleSelectionResolver.SampleFields fields : sampleSelectionResolver.candidatesFor(tenantId, engagementId, segmentCode)) {
                if (!visibility.canSee(fields.assignedEmployeeId())) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("stt", rows.size() + 1);
                row.put("engagementCode", engagement.getCode());
                row.put("auditObjectUnitCode", unit == null ? null : unit.getCode());
                row.put("businessSegmentCode", segmentCode);
                row.put("workItemCode", uniqueWorkItemCode(workItemCodesByEmployeeSegment, fields.assignedEmployeeId(), segmentCode));
                applySampleFields(row, fields);
                rows.add(row);
            }
        }
        return excelExportService.export("audit_ttss_template", templateColumns(), rows);
    }

    /** "Mã công việc" chi dien duoc khi 1 nguoi duoc phan cong DUNG 1 cong viec trong segment do -
     * mo ho (0 hoac >1 cong viec) thi de trong cho nguoi dung tu dien, tranh gan nham. */
    private String uniqueWorkItemCode(Map<String, List<String>> workItemCodesByEmployeeSegment, UUID employeeId, String segmentCode) {
        if (employeeId == null || segmentCode == null) {
            return null;
        }
        List<String> codes = workItemCodesByEmployeeSegment.get(employeeId + "|" + segmentCode);
        return codes != null && codes.size() == 1 ? codes.get(0) : null;
    }

    /** Ghi de cac cot con trong cua 1 dong mau bang du lieu tu doc duoc AuditTtssSampleSelectionResolver
     * (chi ghi field nao KHAC null - mot bang nguon co the chi cung cap 1 vai truong, xem crosswalk
     * trong AuditTtssSampleSelectionResolver). */
    private void applySampleFields(Map<String, Object> row, AuditTtssSampleSelectionResolver.SampleFields fields) {
        if (fields.referenceNumber() != null) {
            row.put("referenceNumber", fields.referenceNumber());
        }
        if (fields.referenceNumber2() != null) {
            row.put("referenceNumber2", fields.referenceNumber2());
        }
        if (fields.customerCode() != null) {
            row.put("customerCode", fields.customerCode());
        }
        if (fields.customerName() != null) {
            row.put("customerName", fields.customerName());
        }
        if (fields.amount() != null) {
            row.put("amount", fields.amount());
        }
        if (fields.performingUser() != null) {
            row.put("performingUser", fields.performingUser());
        }
        if (fields.transactionContent() != null) {
            row.put("transactionContent", fields.transactionContent());
        }
    }

    /** "2. Upload file TTSS" - moi dong UPSERT theo khoa tu nhien (xem uploadKey()): trung khoa thi
     * CAP NHAT dong da co, khong trung thi tao dong moi - sau do tu dong sinh 1 Báo cáo tiến độ cho
     * chinh nguoi upload. */
    @Transactional
    public List<AuditTtssRecordResponse> upload(UUID engagementId, MultipartFile file, String note, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);

        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), templateColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        Employee uploader = principal.employeeCode() == null ? null
                : employeeRepository.findByTenantIdAndEmployeeCode(tenantId, principal.employeeCode()).orElse(null);
        String performerName = uploader == null ? principal.username() : uploader.getFullName();

        Map<String, UUID> segmentIdsByCode = masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getCode, AuditMasterDataItem::getId, (a, b) -> a));
        Map<String, UUID> stepSummaryIdsByCode = processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepSummary::getCode, AuditProcessStepSummary::getId, (a, b) -> a));
        Map<String, UUID> stepDetailIdsByCode = processStepDetailRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepDetail::getCode, AuditProcessStepDetail::getId, (a, b) -> a));
        Map<String, String> findingNamesByCode = exceptionTypeRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditExceptionType::getCode, AuditExceptionType::getName, (a, b) -> a));

        Map<String, AuditTtssRecord> existingByKey = new HashMap<>();
        for (AuditTtssRecord existing : ttssRepository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)) {
            String key = uploadKey(existing.getWorkItemCode(), existing.getProcessStepDetailId(), existing.getFindingCode(),
                    existing.getReferenceNumber(), existing.getReferenceNumber2());
            if (key != null) {
                existingByKey.put(key, existing);
            }
        }

        List<AuditTtssRecord> saved = new ArrayList<>();
        int updatedCount = 0;
        for (Map<String, String> row : rows) {
            String workItemCode = emptyToNull(row.get("workItemCode"));
            UUID processStepDetailId = stepDetailIdsByCode.get(emptyToNull(row.get("processStepDetailCode")));
            String findingCode = emptyToNull(row.get("findingCode"));
            String referenceNumber = emptyToNull(row.get("referenceNumber"));
            String referenceNumber2 = emptyToNull(row.get("referenceNumber2"));
            String key = uploadKey(workItemCode, processStepDetailId, findingCode, referenceNumber, referenceNumber2);

            AuditTtssRecord record = key == null ? null : existingByKey.get(key);
            boolean isUpdate = record != null;
            if (record == null) {
                record = new AuditTtssRecord();
                record.setTenantId(tenantId);
                record.setEngagementId(engagementId);
            }
            record.setRecordUsername(principal.username());
            record.setTtssPerformerName(performerName);
            record.setBusinessSegmentId(segmentIdsByCode.get(emptyToNull(row.get("businessSegmentCode"))));
            record.setWorkItemCode(workItemCode);
            record.setProcessStepSummaryId(stepSummaryIdsByCode.get(emptyToNull(row.get("processStepSummaryCode"))));
            record.setTtssContent(emptyToNull(row.get("ttssContent")));
            record.setProcessStepDetailId(processStepDetailId);
            record.setFindingCode(findingCode);
            String findingName = emptyToNull(row.get("findingName"));
            record.setFindingName(findingName != null ? findingName : findingNamesByCode.get(findingCode));
            record.setMaterial(!isBlank(row.get("material")));
            record.setReferenceNumber(referenceNumber);
            record.setReferenceNumber2(referenceNumber2);
            record.setCustomerCode(emptyToNull(row.get("customerCode")));
            record.setCustomerName(emptyToNull(row.get("customerName")));
            record.setAmount(parseDecimal(row.get("amount")));
            record.setPerformingUser(emptyToNull(row.get("performingUser")));
            record.setTransactionContent(emptyToNull(row.get("transactionContent")));
            record.setExceptionDate(parseDate(row.get("exceptionDate")));
            record.setApproverName(emptyToNull(row.get("approverName")));
            record.setControllerName(emptyToNull(row.get("controllerName")));
            record.setRelatedStaff(emptyToNull(row.get("relatedStaff")));
            record.setUploaderRecommendationCode(emptyToNull(row.get("uploaderRecommendationCode")));
            record.setUploaderRecommendationName(emptyToNull(row.get("uploaderRecommendationName")));
            record.setAppendix(emptyToNull(row.get("appendix")));

            AuditTtssRecord persisted = ttssRepository.save(record);
            saved.add(persisted);
            if (isUpdate) {
                updatedCount++;
            }
            if (key != null) {
                existingByKey.put(key, persisted);
            }
        }

        auditLogService.record("AuditTtssRecord", engagementId, AuditAction.CREATE,
                "Upload file TTSS: " + saved.size() + " dong cho CKT " + engagement.getCode()
                        + " (" + updatedCount + " cap nhat, " + (saved.size() - updatedCount) + " moi)");

        if (uploader != null) {
            UUID businessSegmentId = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupRepository
                            .findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId).stream()
                            .map(AuditEngagementGroup::getId).toList()).stream()
                    .filter(m -> m.getEmployeeId().equals(uploader.getId()))
                    .map(AuditEngagementGroupMember::getBusinessSegment1Id)
                    .findFirst().orElse(null);
            progressReportService.recordUpload(engagementId, uploader.getId(), businessSegmentId, saved, note, principal.username(), file);
        }

        return toResponses(tenantId, saved);
    }

    @Transactional
    public List<AuditTtssRecordResponse> linkRecommendation(UUID engagementId, AuditTtssLinkRecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        AuditRecommendation recommendation = recommendationRepository.findById(request.recommendationId())
                .filter(r -> r.getTenantId().equals(tenantId) && r.getEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi", HttpStatus.NOT_FOUND));

        List<AuditTtssRecord> updated = new ArrayList<>();
        for (UUID recordId : request.ttssRecordIds()) {
            AuditTtssRecord record = getRecordOrThrow(tenantId, engagementId, recordId);
            record.setTeamRecommendationId(recommendation.getId());
            record.setRecommendationApprovalStatus(null);
            record.setRecommendationApprovedBy(null);
            record.setRecommendationApprovedAt(null);
            updated.add(ttssRepository.save(record));
        }
        auditLogService.record("AuditTtssRecord", engagementId, AuditAction.UPDATE,
                "Gan kien nghi " + recommendation.getCode() + " cho " + updated.size() + " dong TTSS");
        return toResponses(tenantId, updated);
    }

    /** "5. Phê duyệt kiến nghị" - chi chap nhan dong da duoc gan kien nghi (teamRecommendationId != null). */
    @Transactional
    public List<UUID> approveRecommendations(UUID engagementId, AuditTtssApproveRecommendationsRequest request, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        requireTeamLead(tenantId, engagement, principal.employeeCode());
        TtssVisibility visibility = resolveVisibility(tenantId, engagement, principal);

        List<UUID> approvedIds = new ArrayList<>();
        for (UUID recordId : request.ttssRecordIds()) {
            AuditTtssRecord record = getRecordOrThrow(tenantId, engagementId, recordId);
            if (!visibility.canSee(record)) {
                throw new BusinessException("AUDIT_TTSS_NOT_VISIBLE", "Ban khong co quyen duyet dong TTSS nay", HttpStatus.FORBIDDEN);
            }
            if (record.getTeamRecommendationId() == null) {
                throw new BusinessException("AUDIT_TTSS_NOT_LINKED", "Chi duoc phe duyet dong da duoc gan kien nghi", HttpStatus.BAD_REQUEST);
            }
            if (record.getRecommendationApprovalStatus() == AssignmentApprovalStatus.APPROVED) {
                continue;
            }

            List<UUID> approverChain = approvalChainResolver.resolveChain(engagement);
            Map<String, Object> variables = new HashMap<>();
            variables.put("ttssRecordId", record.getId().toString());
            variables.put("approved", true);
            variables.put("approverChain", approverChain.stream().map(UUID::toString).toList());

            var processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(PROCESS_KEY)
                    .tenantId(tenantId.toString())
                    .businessKey(record.getId().toString())
                    .variables(variables)
                    .start();

            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskTenantId(tenantId.toString())
                    .singleResult();
            if (task == null) {
                ttssRepository.findById(recordId)
                        .filter(r -> r.getRecommendationApprovalStatus() == AssignmentApprovalStatus.APPROVED)
                        .ifPresent(r -> approvedIds.add(recordId));
                continue;
            }

            record.setRecommendationProcessInstanceId(processInstance.getId());
            record.setRecommendationApprovalStatus(AssignmentApprovalStatus.PENDING);
            ttssRepository.save(record);

            workflowTaskService.complete(task.getId(),
                    new CompleteTaskRequest(Map.of("approved", true, "approverUsername", principal.username())), principal);
            approvedIds.add(recordId);
        }

        auditLogService.record("AuditTtssRecord", engagementId, AuditAction.APPROVE,
                "Truong doan phe duyet " + approvedIds.size() + " kien nghi cua CKT " + engagement.getCode());
        return approvedIds;
    }

    /** Xoa 1 dong TTSS - cho phep xoa bat ke trang thai gan/duyet kien nghi (truong doan/nguoi upload
     * tu chiu trach nhiem khi xoa), nhung CHI trong pham vi dong minh duoc phep xem (xem
     * resolveVisibility()) - khong the xoa dong cua nguoi khac ngoai pham vi phu trach. */
    @Transactional
    public void delete(UUID engagementId, UUID recordId, CurrentUserPrincipal principal) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditTtssRecord record = getRecordOrThrow(tenantId, engagementId, recordId);
        if (!resolveVisibility(tenantId, engagement, principal).canSee(record)) {
            throw new BusinessException("AUDIT_TTSS_NOT_VISIBLE", "Ban khong co quyen xoa dong TTSS nay", HttpStatus.FORBIDDEN);
        }
        ttssRepository.delete(record);
        auditLogService.record("AuditTtssRecord", recordId, AuditAction.DELETE,
                "Xoa dong TTSS " + (record.getFindingCode() == null ? recordId : record.getFindingCode()) + " cua CKT");
    }

    private void requireTeamLead(UUID tenantId, AuditEngagement engagement, String actorEmployeeCode) {
        if (actorEmployeeCode == null) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phe duyet", HttpStatus.FORBIDDEN);
        }
        Employee lead = employeeRepository.findById(engagement.getTeamLeadEmployeeId())
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay truong doan", HttpStatus.NOT_FOUND));
        if (!actorEmployeeCode.equalsIgnoreCase(lead.getEmployeeCode())) {
            throw new BusinessException("AUDIT_ENGAGEMENT_NOT_TEAM_LEAD", "Chi truong doan moi duoc phep phe duyet", HttpStatus.FORBIDDEN);
        }
    }

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private AuditTtssRecord getRecordOrThrow(UUID tenantId, UUID engagementId, UUID recordId) {
        return ttssRepository.findById(recordId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_TTSS_NOT_FOUND", "Khong tim thay dong TTSS", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> segmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    /** Ten cot PHAI khop CHINH XAC (sau khi trim) voi mau Excel that ma nghiep vu dang dung
     * (template_upload_ttss.xlsx, khac voi ban dich ban dau) - kem ca loi chinh ta/thieu dau trong
     * ban goc (vd "Trong yếu" thieu dau, khong duoc "sua dep" lai vi se lam gay khop cot khi import
     * dung file that. "recordUsername"/"ttssPerformerName" chi de LAM TIEU DE cho nguoi dung biet -
     * gia tri LUON tu dong lay tu nguoi upload, KHONG doc lai tu file (xem upload()). Cac cot
     * "*Name" cua BQT chi la ten goi y cho nguoi dien tay, khong doc lai khi import (ten duoc tra ve
     * qua tra cuu FK trong toResponse()). Rieng "findingName" (Ten TTSS) neu de trong se duoc tu
     * dong lay theo "findingCode" tu danh muc AuditExceptionType (Loai ton tai sai sot) neu co khai
     * bao - xem upload(). */
    private List<ExportColumn> templateColumns() {
        return List.of(
                new ExportColumn("stt", "STT"),
                new ExportColumn("engagementCode", "mã CKT"),
                new ExportColumn("auditObjectUnitCode", "Mã CN (lấy từ mã KH)"),
                new ExportColumn("recordUsername", "Cán bộ thực hiện (Lấy user Upload lên)"),
                new ExportColumn("businessSegmentCode", "NV"),
                new ExportColumn("workItemCode", "mã công việc"),
                new ExportColumn("processStepSummaryCode", "mã BQT tổng hợp"),
                new ExportColumn("processStepSummaryName", "tên BQT tổng hợp"),
                new ExportColumn("processStepDetailCode", "Mã BQT chi tiết"),
                new ExportColumn("processStepDetailName", "Tên BQT chi tiết"),
                new ExportColumn("findingCode", "Mã TTSS"),
                new ExportColumn("findingName", "Tên TTSS"),
                new ExportColumn("material", "Trong yếu"),
                new ExportColumn("ttssContent", "Diễn giải"),
                new ExportColumn("referenceNumber", "Số tham chiếu (Số TK, Số thẻ, Số hợp đồng TG,…, số bt)"),
                new ExportColumn("referenceNumber2", "Số tham chiếu 2 (Ngày phát sinh giao dịch) nếu có"),
                new ExportColumn("customerCode", "Mã KH/TKHT/Mã CB"),
                new ExportColumn("customerName", "Tên KH"),
                new ExportColumn("amount", "số tiền giản ngân/Số tiền hạch toán"),
                new ExportColumn("performingUser", "User thực hiện (nếu có) ,user hạch toán"),
                new ExportColumn("transactionContent", "Nội dung (giao dịch nếu có)"),
                new ExportColumn("uploaderRecommendationCode", "mã KN ( nếu có )"),
                new ExportColumn("uploaderRecommendationName", "Loại Kiến nghị"),
                new ExportColumn("exceptionDate", "Ngày HTKN"),
                new ExportColumn("ttssPerformerName", "Tên cán bộ người thực hiện TTSS"),
                new ExportColumn("relatedStaff", "Cán bộ LQ khác"),
                new ExportColumn("approverName", "tên cán bộ phê duyệt"),
                new ExportColumn("controllerName", "tên cán bộ-người kiểm soát"),
                new ExportColumn("appendix", "Phụ lục"));
    }

    /** Khoa trung khi upload lai file TTSS: chi coi la "trung" (UPDATE dong cu) khi CA BA truong
     * dinh danh chinh (ma cong viec, ma TTSS, so tham chieu) deu co gia tri - thieu 1 trong 3 thi
     * luon tao dong moi de tranh gop nham cac dong khong du du lieu phan biet. */
    private String uploadKey(String workItemCode, UUID processStepDetailId, String findingCode, String referenceNumber, String referenceNumber2) {
        if (workItemCode == null || findingCode == null || referenceNumber == null) {
            return null;
        }
        return String.join("|", workItemCode, String.valueOf(processStepDetailId), findingCode, referenceNumber, String.valueOf(referenceNumber2));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (DateTimeParseException ignored) {
                // thu dinh dang tiep theo
            }
        }
        if (trimmed.matches("\\d+(\\.\\d+)?")) {
            try {
                long serial = new BigDecimal(trimmed).longValue();
                return LocalDate.of(1899, 12, 30).plusDays(serial);
            } catch (NumberFormatException | java.time.DateTimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<AuditTtssRecordResponse> toResponses(UUID tenantId, List<AuditTtssRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        Map<UUID, AuditProcessStepSummary> stepSummaries = processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepSummary::getId, s -> s));
        Map<UUID, AuditProcessStepDetail> stepDetails = processStepDetailRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepDetail::getId, s -> s));
        Map<UUID, AuditRecommendation> recommendations = recommendationRepository
                .findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, records.get(0).getEngagementId()).stream()
                .collect(Collectors.toMap(AuditRecommendation::getId, r -> r));
        return records.stream().map(r -> toResponse(r, segments, stepSummaries, stepDetails, recommendations)).toList();
    }

    private AuditTtssRecordResponse toResponse(AuditTtssRecord record, Map<UUID, AuditMasterDataItem> segments,
                                                Map<UUID, AuditProcessStepSummary> stepSummaries, Map<UUID, AuditProcessStepDetail> stepDetails,
                                                Map<UUID, AuditRecommendation> recommendations) {
        AuditMasterDataItem segment = segments.get(record.getBusinessSegmentId());
        AuditProcessStepSummary stepSummary = stepSummaries.get(record.getProcessStepSummaryId());
        AuditProcessStepDetail stepDetail = stepDetails.get(record.getProcessStepDetailId());
        AuditRecommendation teamRecommendation = recommendations.get(record.getTeamRecommendationId());
        return new AuditTtssRecordResponse(record.getId(), record.getEngagementId(), record.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), record.getRecordUsername(), record.getWorkItemCode(),
                record.getProcessStepSummaryId(), stepSummary == null ? null : stepSummary.getCode(), stepSummary == null ? null : stepSummary.getName(),
                record.getProcessStepDetailId(), stepDetail == null ? null : stepDetail.getCode(), record.getTtssContent(),
                record.getFindingCode(), record.getFindingName(), record.isMaterial(), record.getReferenceNumber(), record.getReferenceNumber2(),
                record.getCustomerCode(), record.getCustomerName(), record.getAmount(), record.getPerformingUser(), record.getTransactionContent(),
                record.getExceptionDate(), record.getApproverName(), record.getControllerName(), record.getTtssPerformerName(), record.getRelatedStaff(),
                record.getUploaderRecommendationCode(), record.getUploaderRecommendationName(), record.getAppendix(), record.getTeamRecommendationId(),
                teamRecommendation == null ? null : teamRecommendation.getCode(), teamRecommendation == null ? null : teamRecommendation.getContent(),
                record.getRecommendationApprovalStatus(), record.getRecommendationApprovedBy(), record.getRecommendationApprovedAt());
    }
}
