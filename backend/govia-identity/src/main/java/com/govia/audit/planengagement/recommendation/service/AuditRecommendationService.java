package com.govia.audit.planengagement.recommendation.service;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import com.govia.audit.planengagement.recommendation.repository.AuditRecommendationRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.govia.audit.masterdata.entity.AuditMasterDataCategory.BUSINESS_SEGMENT;

/**
 * "Lưu mã kiến nghị" (Khối C, chức năng "3. Thêm kiến nghị") - catalog rieng cho tung cuoc kiem
 * toan. Luon co san dong mac dinh KNKT000/CE/"Kiến nghị chung" (dung dac ta) - tu seed neu catalog
 * cua engagement do dang rong. Cac dong con lai do NGUOI DUNG tu nhap ma (KNKT001, KNKT002...),
 * khong tu sinh (dac ta yeu cau nguoi dung nhap tay theo quy tac).
 */
@Service
public class AuditRecommendationService {

    private static final String DEFAULT_CODE = "KNKT000";
    private static final String DEFAULT_SEGMENT_CODE = "CE";
    private static final String DEFAULT_CONTENT = "Kiến nghị chung";

    private final AuditRecommendationRepository repository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditTtssRecordRepository ttssRecordRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditRecommendationService(AuditRecommendationRepository repository, AuditEngagementRepository engagementRepository,
                                       AuditMasterDataItemRepository masterDataItemRepository, AuditTtssRecordRepository ttssRecordRepository,
                                       AuditLogService auditLogService, ExcelExportService excelExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.engagementRepository = engagementRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.ttssRecordRepository = ttssRecordRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional
    public List<AuditRecommendationResponse> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);
        return toResponses(tenantId, repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId));
    }

    @Transactional
    public AuditRecommendationResponse create(UUID engagementId, AuditRecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);

        String code = generateNextCode(tenantId, engagementId);

        AuditRecommendation recommendation = new AuditRecommendation();
        recommendation.setTenantId(tenantId);
        recommendation.setEngagementId(engagementId);
        recommendation.setCode(code);
        recommendation.setBusinessSegmentId(request.businessSegmentId());
        recommendation.setContent(request.content());
        recommendation = repository.save(recommendation);

        auditLogService.record("AuditRecommendation", recommendation.getId(), AuditAction.CREATE,
                "Them kien nghi " + code + " cho CKT");
        return toResponse(recommendation, segmentsById(tenantId));
    }

    /** "Tải mẫu" - xuat toan bo catalog hien co cua CKT (kem KNKT000) de nguoi dung sua/them dong
     * roi Import lai - cung 1 bo cot voi upload() de khop header khi doc lai. */
    @Transactional
    public byte[] downloadTemplate(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        List<Map<String, Object>> rows = repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId).stream()
                .map(r -> {
                    AuditMasterDataItem segment = segments.get(r.getBusinessSegmentId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", r.getCode());
                    row.put("businessSegmentCode", segment == null ? null : segment.getCode());
                    row.put("content", r.getContent());
                    return row;
                }).toList();
        return excelExportService.export("mau_kien_nghi", templateColumns(), rows);
    }

    /** Import Excel danh muc kien nghi - UPSERT theo "Mã kiến nghị" (ma la khoa tu nhien, da bat
     * buoc + duy nhat trong 1 CKT tu create()): dong nao trung ma dong da co thi CAP NHAT (ke ca
     * KNKT000 - cho phep sua noi dung/nghiep vu cua dong mac dinh qua import), khong trung thi tao
     * moi. Bo qua dong thieu "Mã kiến nghị" hoac "Nội dung kiến nghị" (bat buoc, giong create()). */
    @Transactional
    public List<AuditRecommendationResponse> upload(UUID engagementId, MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        ensureDefaultSeeded(tenantId, engagementId);

        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), templateColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        Map<String, UUID> segmentIdsByCode = masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getCode, AuditMasterDataItem::getId, (a, b) -> a));
        Map<String, AuditRecommendation> existingByCode = repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId)
                .stream().collect(Collectors.toMap(AuditRecommendation::getCode, r -> r));

        List<AuditRecommendation> saved = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String code = emptyToNull(row.get("code"));
            String content = emptyToNull(row.get("content"));
            if (code == null || content == null) {
                continue;
            }
            code = code.trim().toUpperCase();

            AuditRecommendation recommendation = existingByCode.get(code);
            if (recommendation == null) {
                recommendation = new AuditRecommendation();
                recommendation.setTenantId(tenantId);
                recommendation.setEngagementId(engagementId);
                recommendation.setCode(code);
            }
            String segmentCode = emptyToNull(row.get("businessSegmentCode"));
            recommendation.setBusinessSegmentId(segmentCode == null ? null : segmentIdsByCode.get(segmentCode));
            recommendation.setContent(content);
            recommendation = repository.save(recommendation);
            existingByCode.put(code, recommendation);
            saved.add(recommendation);
        }

        auditLogService.record("AuditRecommendation", engagementId, AuditAction.CREATE,
                "Import Excel danh muc kien nghi: " + saved.size() + " dong cho CKT");
        return toResponses(tenantId, saved);
    }

    private List<ExportColumn> templateColumns() {
        return List.of(
                new ExportColumn("code", "Mã kiến nghị"),
                new ExportColumn("businessSegmentCode", "Loại nghiệp vụ"),
                new ExportColumn("content", "Nội dung kiến nghị"));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Khong cho xoa dong mac dinh (KNKT000, luon phai co san - xem ensureDefaultSeeded) hoac dong
     * dang duoc gan lam kien nghi chinh thuc cho it nhat 1 dong TTSS (AuditTtssRecord.teamRecommendationId). */
    @Transactional
    public void delete(UUID engagementId, UUID recommendationId) {
        UUID tenantId = TenantContext.getTenantId();
        getEngagementOrThrow(tenantId, engagementId);
        AuditRecommendation recommendation = repository.findById(recommendationId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi", HttpStatus.NOT_FOUND));

        if (DEFAULT_CODE.equals(recommendation.getCode())) {
            throw new BusinessException("AUDIT_RECOMMENDATION_DEFAULT_NOT_DELETABLE",
                    "Khong the xoa kien nghi mac dinh " + DEFAULT_CODE, HttpStatus.BAD_REQUEST);
        }
        if (ttssRecordRepository.existsByTenantIdAndTeamRecommendationId(tenantId, recommendationId)) {
            throw new BusinessException("AUDIT_RECOMMENDATION_IN_USE",
                    "Kien nghi dang duoc gan cho it nhat 1 dong TTSS, khong the xoa", HttpStatus.BAD_REQUEST);
        }

        repository.delete(recommendation);
        auditLogService.record("AuditRecommendation", recommendationId, AuditAction.DELETE,
                "Xoa kien nghi " + recommendation.getCode() + " cua CKT");
    }

    /** Tu sinh ma tiep theo dang "KNKTxxx" (it nhat 3 chu so, khong gioi han tren - vd sau KNKT999 la
     * KNKT1000) - quet TOAN BO ma hien co cua CKT (ke ca KNKT000 mac dinh), lay so lon nhat roi +1,
     * dam bao khong bao gio trung voi ma da co (khong can kiem tra trung/ma dat truoc rieng nua). */
    private String generateNextCode(UUID tenantId, UUID engagementId) {
        int maxNumber = repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId).stream()
                .map(AuditRecommendation::getCode)
                .mapToInt(this::codeNumber)
                .max().orElse(0);
        return String.format("KNKT%03d", maxNumber + 1);
    }

    private int codeNumber(String code) {
        if (code == null || !code.startsWith("KNKT")) {
            return 0;
        }
        try {
            return Integer.parseInt(code.substring("KNKT".length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void ensureDefaultSeeded(UUID tenantId, UUID engagementId) {
        if (!repository.findByTenantIdAndEngagementIdOrderByCodeAsc(tenantId, engagementId).isEmpty()) {
            return;
        }
        AuditMasterDataItem defaultSegment = masterDataItemRepository
                .findByTenantIdAndCategoryAndCode(tenantId, BUSINESS_SEGMENT, DEFAULT_SEGMENT_CODE).orElse(null);

        AuditRecommendation defaultRecommendation = new AuditRecommendation();
        defaultRecommendation.setTenantId(tenantId);
        defaultRecommendation.setEngagementId(engagementId);
        defaultRecommendation.setCode(DEFAULT_CODE);
        defaultRecommendation.setBusinessSegmentId(defaultSegment == null ? null : defaultSegment.getId());
        defaultRecommendation.setContent(DEFAULT_CONTENT);
        repository.save(defaultRecommendation);
    }

    private AuditEngagement getEngagementOrThrow(UUID tenantId, UUID id) {
        return engagementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> segmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private List<AuditRecommendationResponse> toResponses(UUID tenantId, List<AuditRecommendation> recommendations) {
        Map<UUID, AuditMasterDataItem> segments = segmentsById(tenantId);
        return recommendations.stream().map(r -> toResponse(r, segments)).toList();
    }

    private AuditRecommendationResponse toResponse(AuditRecommendation recommendation, Map<UUID, AuditMasterDataItem> segments) {
        AuditMasterDataItem segment = segments.get(recommendation.getBusinessSegmentId());
        return new AuditRecommendationResponse(recommendation.getId(), recommendation.getEngagementId(), recommendation.getCode(),
                recommendation.getBusinessSegmentId(), segment == null ? null : segment.getCode(), recommendation.getContent());
    }
}
