package com.govia.audit.planengagement.ttss.service;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Quản lý TTSS & Kiến nghị" (Khối C, sheet "Quản lý công việc" trong Tạo CKT (1).xlsx, mục C).
 * "Download Template"/"Upload file TTSS" xuat 1 dong cho MOI cong viec da phan cong (khong tu dem
 * so mau o 16 bang chon mau CmNtd1..14/CmTd1/2 - xem ghi chu trong plan). Moi lan upload TAO MOI
 * cac dong (khong upsert) va tu dong sinh 1 Báo cáo tiến độ (AuditProgressReportService.recordUpload).
 */
@Service
public class AuditTtssService {

    private static final String PROCESS_KEY = "audit_recommendation_approval";
    private static final String ATTACHMENT_ENTITY_NAME = "AUDIT_PROGRESS_REPORT";
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

    public AuditTtssService(AuditTtssRecordRepository ttssRepository, AuditEngagementRepository engagementRepository,
                             AuditEngagementGroupRepository groupRepository, AuditEngagementGroupMemberRepository memberRepository,
                             AuditEngagementAssignmentRepository assignmentRepository, AuditWorkItemRepository workItemRepository,
                             AuditObjectUnitRepository objectUnitRepository, AuditMasterDataItemRepository masterDataItemRepository,
                             AuditProcessStepSummaryRepository processStepSummaryRepository, AuditProcessStepDetailRepository processStepDetailRepository,
                             AuditRecommendationRepository recommendationRepository, EmployeeRepository employeeRepository,
                             UserAccountRepository userAccountRepository, AuditProgressReportService progressReportService,
                             AuditWorkApprovalChainResolver approvalChainResolver, RuntimeService runtimeService, TaskService taskService,
                             WorkflowTaskService workflowTaskService, ExcelExportService excelExportService, ExcelImportService excelImportService,
                             AuditLogService auditLogService) {
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
    }

    @Transactional(readOnly = true)
    public List<AuditTtssRecordResponse> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        List<AuditTtssRecord> records = ttssRepository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId);
        return toResponses(tenantId, records);
    }

    /** "1. Download template TTSS" - 1 dong cho MOI cong viec da duoc phan cong trong CKT nay (ca
     * CBKT lan THKT), cac cot con lai de trong cho user dien tay. */
    @Transactional(readOnly = true)
    public byte[] downloadTemplate(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getEngagementOrThrow(tenantId, engagementId);
        AuditObjectUnit unit = objectUnitRepository.findById(engagement.getAuditObjectUnitId()).orElse(null);

        List<AuditEngagementGroup> groups = groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, engagementId);
        List<UUID> groupIds = groups.stream().map(AuditEngagementGroup::getId).toList();
        List<AuditEngagementGroupMember> members = memberRepository.findByTenantIdAndGroupIdIn(tenantId, groupIds);
        Map<UUID, AuditEngagementGroupMember> membersById = members.stream().collect(Collectors.toMap(AuditEngagementGroupMember::getId, m -> m));
        List<AuditEngagementAssignment> assignments = assignmentRepository.findByTenantIdAndGroupMemberIdIn(tenantId,
                members.stream().map(AuditEngagementGroupMember::getId).toList());
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream().map(AuditEngagementAssignment::getWorkItemId).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (AuditEngagementAssignment assignment : assignments) {
            AuditWorkItem workItem = workItems.get(assignment.getWorkItemId());
            if (workItem == null) {
                continue;
            }
            AuditMasterDataItem segment = segments.get(workItem.getBusinessSegmentId());
            Map<String, Object> row = new HashMap<>();
            row.put("engagementCode", engagement.getCode());
            row.put("auditObjectUnitCode", unit == null ? null : unit.getCode());
            row.put("businessSegmentCode", segment == null ? null : segment.getCode());
            row.put("workItemCode", workItem.getCode());
            rows.add(row);
        }
        return excelExportService.export("audit_ttss_template", templateColumns(), rows);
    }

    /** "2. Upload file TTSS" - moi dong tao MOI 1 AuditTtssRecord (khong upsert), sau do tu dong
     * sinh 1 Báo cáo tiến độ cho chinh nguoi upload. */
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

        List<AuditTtssRecord> saved = new ArrayList<>();
        for (Map<String, String> row : rows) {
            AuditTtssRecord record = new AuditTtssRecord();
            record.setTenantId(tenantId);
            record.setEngagementId(engagementId);
            record.setRecordUsername(principal.username());
            record.setTtssPerformerName(performerName);
            record.setBusinessSegmentId(segmentIdsByCode.get(emptyToNull(row.get("businessSegmentCode"))));
            record.setWorkItemCode(emptyToNull(row.get("workItemCode")));
            record.setProcessStepSummaryId(stepSummaryIdsByCode.get(emptyToNull(row.get("processStepSummaryCode"))));
            record.setTtssContent(emptyToNull(row.get("ttssContent")));
            record.setProcessStepDetailId(stepDetailIdsByCode.get(emptyToNull(row.get("processStepDetailCode"))));
            record.setFindingCode(emptyToNull(row.get("findingCode")));
            record.setFindingName(emptyToNull(row.get("findingName")));
            record.setMaterial(!isBlank(row.get("material")));
            record.setReferenceNumber(emptyToNull(row.get("referenceNumber")));
            record.setReferenceNumber2(emptyToNull(row.get("referenceNumber2")));
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
            saved.add(ttssRepository.save(record));
        }

        auditLogService.record("AuditTtssRecord", engagementId, AuditAction.CREATE,
                "Upload file TTSS: " + saved.size() + " dong cho CKT " + engagement.getCode());

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

        List<UUID> approvedIds = new ArrayList<>();
        for (UUID recordId : request.ttssRecordIds()) {
            AuditTtssRecord record = getRecordOrThrow(tenantId, engagementId, recordId);
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

    private List<ExportColumn> templateColumns() {
        return List.of(
                new ExportColumn("engagementCode", "Mã CKT"),
                new ExportColumn("auditObjectUnitCode", "Mã CN"),
                new ExportColumn("businessSegmentCode", "Nghiệp vụ"),
                new ExportColumn("workItemCode", "Mã công việc"),
                new ExportColumn("processStepSummaryCode", "Bước QT tổng hợp"),
                new ExportColumn("ttssContent", "Nội dung TTSS"),
                new ExportColumn("processStepDetailCode", "Mã BQT chi tiết"),
                new ExportColumn("findingCode", "Mã phát hiện"),
                new ExportColumn("findingName", "Tên phát hiện"),
                new ExportColumn("material", "Trọng yếu"),
                new ExportColumn("referenceNumber", "Số tham chiếu"),
                new ExportColumn("referenceNumber2", "Số tham chiếu 2"),
                new ExportColumn("customerCode", "Mã KH"),
                new ExportColumn("customerName", "Tên KH"),
                new ExportColumn("amount", "Số tiền"),
                new ExportColumn("performingUser", "User thực hiện"),
                new ExportColumn("transactionContent", "Nội dung giao dịch"),
                new ExportColumn("exceptionDate", "Ngày HTKN"),
                new ExportColumn("approverName", "Tên cán bộ phê duyệt"),
                new ExportColumn("controllerName", "Tên người kiểm soát"),
                new ExportColumn("relatedStaff", "Cán bộ liên quan khác"),
                new ExportColumn("uploaderRecommendationCode", "Mã KN"),
                new ExportColumn("uploaderRecommendationName", "Tên KN"));
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
                record.getUploaderRecommendationCode(), record.getUploaderRecommendationName(), record.getTeamRecommendationId(),
                teamRecommendation == null ? null : teamRecommendation.getCode(), teamRecommendation == null ? null : teamRecommendation.getContent(),
                record.getRecommendationApprovalStatus(), record.getRecommendationApprovedBy(), record.getRecommendationApprovedAt());
    }
}
