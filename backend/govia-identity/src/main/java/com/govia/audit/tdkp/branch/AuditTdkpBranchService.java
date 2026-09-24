package com.govia.audit.tdkp.branch;

import com.govia.audit.branchstaff.entity.AuditBranchStaff;
import com.govia.audit.branchstaff.repository.AuditBranchStaffRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.tdkp.common.TdkpMasterData;
import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Sheet 4 - ZTC_TDKP_CN: theo dõi kiến nghị của KTNB đối với Chi nhánh (bảng tổng hợp + bảng chi tiết sai sót). */
@Service
public class AuditTdkpBranchService {

    private final AuditTdkpBranchRecommendationRepository recommendationRepository;
    private final AuditTdkpBranchDefectRepository defectRepository;
    private final AuditTtssRecordRepository ttssRepository;
    private final AuditRecommendationRepository auditRecommendationRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditBranchStaffRepository branchStaffRepository;
    private final TdkpMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditTdkpBranchService(AuditTdkpBranchRecommendationRepository recommendationRepository, AuditTdkpBranchDefectRepository defectRepository,
                                  AuditTtssRecordRepository ttssRepository, AuditRecommendationRepository auditRecommendationRepository,
                                  AuditEngagementRepository engagementRepository, AuditBranchStaffRepository branchStaffRepository,
                                  TdkpMasterData masterData, AuditLogService auditLogService, ExcelExportService excelExportService,
                                  ExcelImportService excelImportService) {
        this.recommendationRepository = recommendationRepository;
        this.defectRepository = defectRepository;
        this.ttssRepository = ttssRepository;
        this.auditRecommendationRepository = auditRecommendationRepository;
        this.engagementRepository = engagementRepository;
        this.branchStaffRepository = branchStaffRepository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    /** "Hiện trạng chỉnh sửa sai sót liên quan đến kiến nghị": tất cả sai sót cùng 1 hiện trạng thì lấy hiện trạng đó, còn lại là Đang thực hiện.
     * Sai sót chưa chọn hiện trạng tính là Chưa thực hiện; kiến nghị chưa có sai sót nào -> null. */
    public static TdkpStatus computeDefectStatus(Collection<TdkpStatus> customerStatuses) {
        if (customerStatuses.isEmpty()) {
            return null;
        }
        List<TdkpStatus> normalized = customerStatuses.stream().map(s -> s == null ? TdkpStatus.NOT_STARTED : s).toList();
        if (normalized.stream().allMatch(s -> s == TdkpStatus.DONE)) {
            return TdkpStatus.DONE;
        }
        if (normalized.stream().allMatch(s -> s == TdkpStatus.NOT_STARTED)) {
            return TdkpStatus.NOT_STARTED;
        }
        return TdkpStatus.IN_PROGRESS;
    }

    // ===================== bang tong hop =====================

    @Transactional(readOnly = true)
    public List<AuditTdkpBranchDto.RecommendationResponse> listRecommendations() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditTdkpBranchRecommendation> items = recommendationRepository.findByTenantIdOrderByManagementCodeAsc(tenantId);
        Lookup lookup = lookup();
        Map<UUID, List<AuditTdkpBranchDefect>> defects = defectsByRecommendation(tenantId, items);
        return items.stream().map(item -> toResponse(item, lookup, defects.getOrDefault(item.getId(), List.of()))).toList();
    }

    @Transactional
    public AuditTdkpBranchDto.RecommendationResponse createRecommendation(AuditTdkpBranchDto.RecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpBranchRecommendation item = new AuditTdkpBranchRecommendation();
        item.setTenantId(tenantId);
        item.setManagementCode(nextManagementCode(tenantId));
        applyRecommendation(item, request);
        item = recommendationRepository.save(item);
        auditLogService.record("AuditTdkpBranchRecommendation", item.getId(), AuditAction.CREATE, "Tao kien nghi chi nhanh: " + item.getManagementCode());
        return toResponse(item, lookup(), List.of());
    }

    @Transactional
    public AuditTdkpBranchDto.RecommendationResponse updateRecommendation(UUID id, AuditTdkpBranchDto.RecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpBranchRecommendation item = getRecommendationOrThrow(id);
        applyRecommendation(item, request);
        item = recommendationRepository.save(item);
        auditLogService.record("AuditTdkpBranchRecommendation", item.getId(), AuditAction.UPDATE, "Cap nhat kien nghi chi nhanh: " + item.getManagementCode());
        return toResponse(item, lookup(), defectRepository.findByTenantIdAndBranchRecommendationIdOrderByCreatedAtAsc(tenantId, id));
    }

    @Transactional
    public void deleteRecommendation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpBranchRecommendation item = getRecommendationOrThrow(id);
        defectRepository.deleteByTenantIdAndBranchRecommendationId(tenantId, id);
        recommendationRepository.delete(item);
        auditLogService.record("AuditTdkpBranchRecommendation", id, AuditAction.DELETE, "Xoa kien nghi chi nhanh: " + item.getManagementCode());
    }

    // ===================== bang chi tiet sai sot =====================

    /** recommendationId null -> toan bo sai sot. */
    @Transactional(readOnly = true)
    public List<AuditTdkpBranchDto.DefectResponse> listDefects(UUID recommendationId) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditTdkpBranchRecommendation> recommendations = recommendationRepository.findByTenantIdOrderByManagementCodeAsc(tenantId);
        Map<UUID, List<AuditTdkpBranchDefect>> byRecommendation = defectsByRecommendation(tenantId, recommendations);
        Map<UUID, AuditTdkpBranchRecommendation> recommendationById = recommendations.stream().collect(Collectors.toMap(AuditTdkpBranchRecommendation::getId, r -> r));
        List<AuditTdkpBranchDto.DefectResponse> result = new ArrayList<>();
        for (AuditTdkpBranchRecommendation recommendation : recommendations) {
            if (recommendationId != null && !recommendationId.equals(recommendation.getId())) {
                continue;
            }
            List<AuditTdkpBranchDefect> defects = byRecommendation.getOrDefault(recommendation.getId(), List.of());
            TdkpStatus overall = computeDefectStatus(defects.stream().map(AuditTdkpBranchDefect::getCustomerStatus).toList());
            for (AuditTdkpBranchDefect defect : defects) {
                result.add(toDefectResponse(defect, recommendationById.get(defect.getBranchRecommendationId()), overall));
            }
        }
        return result;
    }

    @Transactional
    public AuditTdkpBranchDto.DefectResponse createDefect(AuditTdkpBranchDto.DefectRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpBranchRecommendation recommendation = getRecommendationOrThrow(request.branchRecommendationId());
        AuditTdkpBranchDefect item = new AuditTdkpBranchDefect();
        item.setTenantId(tenantId);
        applyDefect(item, request);
        item = defectRepository.save(item);
        UUID savedId = item.getId();
        auditLogService.record("AuditTdkpBranchDefect", savedId, AuditAction.CREATE, "Them sai sot cho kien nghi " + recommendation.getManagementCode());
        return listDefects(recommendation.getId()).stream().filter(d -> d.id().equals(savedId)).findFirst().orElseThrow();
    }

    @Transactional
    public AuditTdkpBranchDto.DefectResponse updateDefect(UUID id, AuditTdkpBranchDto.DefectRequest request) {
        AuditTdkpBranchDefect item = getDefectOrThrow(id);
        if (!item.getBranchRecommendationId().equals(request.branchRecommendationId())) {
            throw new BusinessException("TDKP_DEFECT_MOVE_NOT_ALLOWED", "Khong doi Ma quan ly kien nghi cua dong sai sot");
        }
        applyDefect(item, request);
        item = defectRepository.save(item);
        UUID savedId = item.getId();
        auditLogService.record("AuditTdkpBranchDefect", savedId, AuditAction.UPDATE, "Cap nhat sai sot kien nghi chi nhanh");
        return listDefects(item.getBranchRecommendationId()).stream().filter(d -> d.id().equals(savedId)).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteDefect(UUID id) {
        AuditTdkpBranchDefect item = getDefectOrThrow(id);
        defectRepository.delete(item);
        auditLogService.record("AuditTdkpBranchDefect", id, AuditAction.DELETE, "Xoa sai sot kien nghi chi nhanh");
    }

    /** List "Cán bộ liên quan": CBNV tại chi nhánh đi kiểm toán = danh mục Chức danh cán bộ chi nhánh của mã chi nhánh của kiến nghị. */
    @Transactional(readOnly = true)
    public List<String> staffOptions(UUID recommendationId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpBranchRecommendation recommendation = getRecommendationOrThrow(recommendationId);
        AuditObjectUnit unit = masterData.units().get(recommendation.getAuditObjectUnitId());
        if (unit == null) {
            return List.of();
        }
        return branchStaffRepository.findByTenantIdOrderByBranchCodeAscStaffNameAsc(tenantId).stream()
                .filter(s -> unit.getCode().equalsIgnoreCase(s.getBranchCode())).map(AuditBranchStaff::getStaffName).distinct().toList();
    }

    // ===================== chuyen du lieu tu phan he Thuc hien kiem toan =====================

    /** "Cho phép chuyển dữ liệu kiến nghị từ phân hệ Thực hiện kiểm toán sang": lấy các TTSS đã được phê duyệt kiến nghị (kiến nghị trưởng đoàn),
     * gom theo (cuộc kiểm toán, kiến nghị) thành 1 dòng bảng tổng hợp + mỗi TTSS thành 1 dòng bảng chi tiết. Chạy lại không tạo trùng. */
    @Transactional
    public AuditTdkpBranchDto.TransferResult transferFromExecution(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditEngagement> engagements = engagementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(e -> year == null || year.equals(e.getYear())).collect(Collectors.toMap(AuditEngagement::getId, e -> e));
        if (engagements.isEmpty()) {
            return new AuditTdkpBranchDto.TransferResult(0, 0, 0);
        }
        List<AuditTtssRecord> approved = ttssRepository.findByTenantIdAndEngagementIdIn(tenantId, List.copyOf(engagements.keySet())).stream()
                .filter(r -> r.getTeamRecommendationId() != null && r.getRecommendationApprovalStatus() == AssignmentApprovalStatus.APPROVED)
                .sorted(java.util.Comparator.comparing(AuditTtssRecord::getCreatedAt)).toList();

        Map<UUID, AuditRecommendation> auditRecommendations = auditRecommendationRepository
                .findByTenantIdAndEngagementIdInOrderByCodeAsc(tenantId, List.copyOf(engagements.keySet())).stream()
                .collect(Collectors.toMap(AuditRecommendation::getId, r -> r));
        int recommendationsCreated = 0;
        int defectsCreated = 0;
        int skipped = 0;
        Map<String, AuditTdkpBranchRecommendation> resolved = new HashMap<>();
        List<String> codes = recommendationRepository.findByTenantIdOrderByManagementCodeAsc(tenantId).stream()
                .map(AuditTdkpBranchRecommendation::getManagementCode).collect(Collectors.toCollection(ArrayList::new));

        for (AuditTtssRecord ttss : approved) {
            AuditRecommendation source = auditRecommendations.get(ttss.getTeamRecommendationId());
            AuditEngagement engagement = engagements.get(ttss.getEngagementId());
            if (source == null || engagement == null) {
                skipped++;
                continue;
            }
            String key = engagement.getId() + "|" + source.getId();
            AuditTdkpBranchRecommendation recommendation = resolved.get(key);
            if (recommendation == null) {
                recommendation = recommendationRepository
                        .findByTenantIdAndEngagementIdAndSourceRecommendationId(tenantId, engagement.getId(), source.getId()).orElse(null);
                if (recommendation == null) {
                    recommendation = new AuditTdkpBranchRecommendation();
                    recommendation.setTenantId(tenantId);
                    String code = TdkpSupport.nextCode("KN", codes);
                    codes.add(code);
                    recommendation.setManagementCode(code);
                    recommendation.setAuditObjectUnitId(engagement.getAuditObjectUnitId());
                    recommendation.setEngagementId(engagement.getId());
                    recommendation.setSourceRecommendationId(source.getId());
                    recommendation.setAuditYear(engagement.getYear());
                    recommendation.setContent(source.getContent());
                    recommendation.setBusinessSegmentId(source.getBusinessSegmentId());
                    recommendation = recommendationRepository.save(recommendation);
                    recommendationsCreated++;
                }
                resolved.put(key, recommendation);
            }
            AuditTdkpBranchDefect existingDefect = defectRepository.findByTenantIdAndSourceTtssId(tenantId, ttss.getId()).orElse(null);
            if (existingDefect != null) {
                // Cac dong da chuyen TU TRUOC KHI co cot defectCode (truoc test23.9) khong duoc dien
                // gia tri nay - backfill lai tu findingCode ("Mã TTSS") theo phan hoi nguoi dung (test24.9).
                if (existingDefect.getDefectCode() == null || existingDefect.getDefectCode().isBlank()) {
                    existingDefect.setDefectCode(ttss.getFindingCode());
                    defectRepository.save(existingDefect);
                }
                skipped++;
                continue;
            }
            AuditTdkpBranchDefect defect = new AuditTdkpBranchDefect();
            defect.setTenantId(tenantId);
            defect.setBranchRecommendationId(recommendation.getId());
            defect.setSourceTtssId(ttss.getId());
            defect.setDefectContent(ttss.getTtssContent());
            defect.setCustomerEntry(joinNonBlank(" - ", ttss.getCustomerName(), ttss.getTransactionContent()));
            defect.setCreditContract(ttss.getReferenceNumber());
            defect.setDefectCode(ttss.getFindingCode());
            defect.setDefectType(ttss.getFindingName() != null && !ttss.getFindingName().isBlank() ? ttss.getFindingName() : ttss.getFindingCode());
            defect.setRelatedStaff(ttss.getRelatedStaff());
            defectRepository.save(defect);
            defectsCreated++;
        }
        auditLogService.record("AuditTdkpBranchRecommendation", null, AuditAction.CREATE,
                "Chuyen kien nghi tu Thuc hien kiem toan: " + recommendationsCreated + " kien nghi, " + defectsCreated + " sai sot");
        return new AuditTdkpBranchDto.TransferResult(recommendationsCreated, defectsCreated, skipped);
    }

    // ===================== Excel =====================

    private List<ExportColumn> recommendationColumns() {
        return List.of(new ExportColumn("managementCode", "Mã quản lý kiến nghị"), new ExportColumn("branchName", "Chi nhánh"),
                new ExportColumn("branchCode", "Mã chi nhánh"), new ExportColumn("auditYear", "Năm KT"), new ExportColumn("content", "Nội dung kiến nghị"),
                new ExportColumn("recommendationType", "Phân loại kiến nghị"), new ExportColumn("businessSegment", "Mã mảng nghiệp vụ"),
                new ExportColumn("deadline", "Thời hạn hoàn thành kiến nghị"), new ExportColumn("editContent", "Nội dung chỉnh sửa"),
                new ExportColumn("status", "Hiện trạng"), new ExportColumn("evaluation", "Đánh giá tình hình thực hiện kiến nghị"),
                new ExportColumn("deadlineState", "Trạng thái"), new ExportColumn("note", "Ghi Chú"));
    }

    @Transactional(readOnly = true)
    public byte[] exportRecommendations() {
        List<Map<String, Object>> rows = listRecommendations().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("managementCode", r.managementCode());
            row.put("branchName", r.branchName());
            row.put("branchCode", r.branchCode());
            row.put("auditYear", r.auditYear());
            row.put("content", r.content());
            row.put("recommendationType", r.recommendationTypeName());
            row.put("businessSegment", r.businessSegmentCode());
            row.put("deadline", r.deadline());
            row.put("editContent", r.editContent());
            row.put("status", r.statusLabel());
            row.put("evaluation", r.evaluation());
            row.put("deadlineState", r.deadlineStateLabel());
            row.put("note", r.note());
            return row;
        }).toList();
        return excelExportService.export("TDKP_CN_KIEN_NGHI", recommendationColumns(), rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportDefects() {
        List<ExportColumn> columns = List.of(new ExportColumn("managementCode", "Mã quản lý kiến nghị"), new ExportColumn("defectContent", "Sai sót liên quan đến kiến nghị"),
                new ExportColumn("customerEntry", "Khách hàng/Bút toán"), new ExportColumn("creditContract", "Hợp đồng tín dụng liên quan (LAV)"),
                new ExportColumn("defectCode", "Mã tồn tại sai sót"), new ExportColumn("defectType", "Loại sai sót"),
                new ExportColumn("recommendationDefectStatus", "Hiện trạng chỉnh sửa sai sót liên quan đến kiến nghị"),
                new ExportColumn("customerStatus", "Hiện trạng chỉnh sửa sai sót liên quan đến khách hàng"), new ExportColumn("relatedStaff", "Cán bộ liên quan"));
        List<Map<String, Object>> rows = listDefects(null).stream().map(d -> {
            Map<String, Object> row = new HashMap<>();
            row.put("managementCode", d.managementCode());
            row.put("defectContent", d.defectContent());
            row.put("customerEntry", d.customerEntry());
            row.put("creditContract", d.creditContract());
            row.put("defectCode", d.defectCode());
            row.put("defectType", d.defectType());
            row.put("recommendationDefectStatus", d.recommendationDefectStatusLabel());
            row.put("customerStatus", d.customerStatusLabel());
            row.put("relatedStaff", d.relatedStaff());
            return row;
        }).toList();
        return excelExportService.export("TDKP_CN_SAI_SOT", columns, rows);
    }

    /** Import bảng tổng hợp: cột dạng list (chi nhánh, phân loại, mảng nghiệp vụ, hiện trạng) phải khớp danh mục; có Mã quản lý kiến nghị đã tồn tại
     * thì cập nhật dòng đó, ngược lại tạo mới và hệ thống tự sinh mã. */
    @Transactional
    public ImportResult importRecommendations(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), recommendationColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        UUID tenantId = TenantContext.getTenantId();
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (TdkpSupport.isBlank(row.get("content"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Noi dung kien nghi");
                }
                String unitText = TdkpSupport.isBlank(row.get("branchCode")) ? row.get("branchName") : row.get("branchCode");
                UUID unitId = masterData.resolveUnit(unitText, "Chi nhanh");
                if (unitId == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Chi nhanh");
                }
                AuditTdkpBranchDto.RecommendationRequest request = new AuditTdkpBranchDto.RecommendationRequest(unitId, TdkpSupport.parseInt(row.get("auditYear")),
                        row.get("content").trim(), masterData.resolveItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, row.get("recommendationType"), "Phan loai kien nghi"),
                        masterData.resolveItem(AuditMasterDataCategory.BUSINESS_SEGMENT, row.get("businessSegment"), "Ma mang nghiep vu"),
                        TdkpSupport.parseDate(row.get("deadline")), TdkpSupport.emptyToNull(row.get("editContent")), TdkpStatus.parse(row.get("status")),
                        TdkpSupport.emptyToNull(row.get("evaluation")), TdkpSupport.emptyToNull(row.get("note")));
                String code = TdkpSupport.emptyToNull(row.get("managementCode"));
                AuditTdkpBranchRecommendation existing = code == null ? null : recommendationRepository.findByTenantIdAndManagementCode(tenantId, code).orElse(null);
                if (existing != null) {
                    updateRecommendation(existing.getId(), request);
                } else {
                    createRecommendation(request);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditTdkpBranchRecommendation", null, AuditAction.CREATE,
                "Import Excel kien nghi chi nhanh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    // ===================== noi bo =====================

    private void applyRecommendation(AuditTdkpBranchRecommendation item, AuditTdkpBranchDto.RecommendationRequest request) {
        item.setAuditObjectUnitId(masterData.requireUnit(request.auditObjectUnitId(), "Chi nhanh"));
        item.setAuditYear(request.auditYear());
        item.setContent(request.content().trim());
        item.setRecommendationTypeId(masterData.requireItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, request.recommendationTypeId(), "Phan loai kien nghi"));
        item.setBusinessSegmentId(masterData.requireItem(AuditMasterDataCategory.BUSINESS_SEGMENT, request.businessSegmentId(), "Ma mang nghiep vu"));
        item.setDeadline(request.deadline());
        item.setEditContent(TdkpSupport.emptyToNull(request.editContent()));
        item.setStatus(request.status());
        item.setEvaluation(TdkpSupport.emptyToNull(request.evaluation()));
        item.setNote(TdkpSupport.emptyToNull(request.note()));
    }

    private void applyDefect(AuditTdkpBranchDefect item, AuditTdkpBranchDto.DefectRequest request) {
        item.setBranchRecommendationId(request.branchRecommendationId());
        item.setDefectContent(TdkpSupport.emptyToNull(request.defectContent()));
        item.setCustomerEntry(TdkpSupport.emptyToNull(request.customerEntry()));
        item.setCreditContract(TdkpSupport.emptyToNull(request.creditContract()));
        item.setDefectCode(TdkpSupport.emptyToNull(request.defectCode()));
        item.setDefectType(TdkpSupport.emptyToNull(request.defectType()));
        item.setCustomerStatus(request.customerStatus());
        item.setRelatedStaff(TdkpSupport.emptyToNull(request.relatedStaff()));
    }

    private String nextManagementCode(UUID tenantId) {
        return TdkpSupport.nextCode("KN", recommendationRepository.findByTenantIdOrderByManagementCodeAsc(tenantId).stream()
                .map(AuditTdkpBranchRecommendation::getManagementCode).toList());
    }

    private Map<UUID, List<AuditTdkpBranchDefect>> defectsByRecommendation(UUID tenantId, List<AuditTdkpBranchRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return Map.of();
        }
        return defectRepository.findByTenantIdAndBranchRecommendationIdIn(tenantId, recommendations.stream().map(AuditTdkpBranchRecommendation::getId).toList())
                .stream().sorted(java.util.Comparator.comparing(AuditTdkpBranchDefect::getCreatedAt))
                .collect(Collectors.groupingBy(AuditTdkpBranchDefect::getBranchRecommendationId, LinkedHashMap::new, Collectors.toList()));
    }

    private AuditTdkpBranchRecommendation getRecommendationOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return recommendationRepository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_BRANCH_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi chi nhanh", HttpStatus.NOT_FOUND));
    }

    private AuditTdkpBranchDefect getDefectOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return defectRepository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_BRANCH_DEFECT_NOT_FOUND", "Khong tim thay dong sai sot", HttpStatus.NOT_FOUND));
    }

    private static String joinNonBlank(String separator, String... parts) {
        String joined = java.util.Arrays.stream(parts).filter(p -> p != null && !p.isBlank()).map(String::trim).collect(Collectors.joining(separator));
        return joined.isEmpty() ? null : joined;
    }

    private record Lookup(Map<UUID, AuditMasterDataItem> types, Map<UUID, AuditMasterDataItem> segments, Map<UUID, AuditObjectUnit> units) {
    }

    private Lookup lookup() {
        return new Lookup(masterData.items(AuditMasterDataCategory.RECOMMENDATION_TYPE), masterData.items(AuditMasterDataCategory.BUSINESS_SEGMENT),
                masterData.units());
    }

    private AuditTdkpBranchDto.RecommendationResponse toResponse(AuditTdkpBranchRecommendation item, Lookup lookup, List<AuditTdkpBranchDefect> defects) {
        AuditObjectUnit unit = lookup.units().get(item.getAuditObjectUnitId());
        String state = TdkpSupport.deadlineState(item.getDeadline());
        Instant editedAt = item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt();
        LocalDate editedDate = editedAt == null ? null : editedAt.atZone(ZoneId.systemDefault()).toLocalDate();
        int done = (int) defects.stream().filter(d -> d.getCustomerStatus() == TdkpStatus.DONE).count();
        return new AuditTdkpBranchDto.RecommendationResponse(item.getId(), item.getManagementCode(), item.getAuditObjectUnitId(),
                unit == null ? null : unit.getName(), unit == null ? null : unit.getCode(), item.getAuditYear(), item.getEngagementId(), item.getContent(),
                item.getRecommendationTypeId(), TdkpMasterData.nameOf(lookup.types(), item.getRecommendationTypeId()), item.getBusinessSegmentId(),
                TdkpMasterData.codeOf(lookup.segments(), item.getBusinessSegmentId()), item.getDeadline(), item.getEditContent(), item.getStatus(),
                TdkpStatus.labelOf(item.getStatus()), item.getEvaluation(), state, TdkpSupport.deadlineLabel(state), editedDate, item.getNote(),
                defects.size(), done);
    }

    private AuditTdkpBranchDto.DefectResponse toDefectResponse(AuditTdkpBranchDefect item, AuditTdkpBranchRecommendation recommendation, TdkpStatus overall) {
        return new AuditTdkpBranchDto.DefectResponse(item.getId(), item.getBranchRecommendationId(),
                Objects.requireNonNull(recommendation).getManagementCode(), item.getDefectContent(), item.getCustomerEntry(), item.getCreditContract(),
                item.getDefectCode(), item.getDefectType(), overall, TdkpStatus.labelOf(overall), item.getCustomerStatus(), TdkpStatus.labelOf(item.getCustomerStatus()),
                item.getRelatedStaff());
    }
}
