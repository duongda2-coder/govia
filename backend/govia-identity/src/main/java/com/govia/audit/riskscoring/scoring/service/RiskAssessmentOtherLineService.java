package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherLineRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherLineResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherHeader;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherLine;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOther;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOtherScale;
import com.govia.audit.riskscoring.scoring.repository.RiskAssessmentOtherLineRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherScaleRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quan ly "line" (chi tieu + diem da cham) cua 1 header ZTC_CDRR_KHAC (xem
 * RiskAssessmentOtherHeaderService). He thong tu dong sinh 1 dong cho moi chi tieu
 * (RiskCriteriaOther) phu hop voi category cua header - dung theo dung mo ta trong tai lieu goc
 * ("He thong tu the hien tat ca ma chi tieu theo ma loai doi tuong kiem toan").
 */
@Service
public class RiskAssessmentOtherLineService {

    private final RiskAssessmentOtherLineRepository repository;
    private final RiskCriteriaOtherRepository criteriaOtherRepository;
    private final RiskCriteriaOtherScaleRepository scaleRepository;
    private final AuditLogService auditLogService;

    public RiskAssessmentOtherLineService(RiskAssessmentOtherLineRepository repository,
                                           RiskCriteriaOtherRepository criteriaOtherRepository,
                                           RiskCriteriaOtherScaleRepository scaleRepository,
                                           AuditLogService auditLogService) {
        this.repository = repository;
        this.criteriaOtherRepository = criteriaOtherRepository;
        this.scaleRepository = scaleRepository;
        this.auditLogService = auditLogService;
    }

    /** Dam bao co du 1 dong cho moi chi tieu phu hop voi category cua header - khong xoa dong cu. */
    @Transactional
    public void ensureLines(RiskAssessmentOtherHeader header) {
        UUID tenantId = header.getTenantId();
        List<RiskCriteriaOther> matchingCriteria =
                criteriaOtherRepository.findByTenantIdAndAuditObjectCategoryIdOrderByCodeAsc(tenantId, header.getAuditObjectCategoryId());
        Map<UUID, RiskAssessmentOtherLine> existingByCriteria = new HashMap<>();
        for (RiskAssessmentOtherLine line : repository.findByHeaderIdOrderByCriteriaOtherIdAsc(header.getId())) {
            existingByCriteria.put(line.getCriteriaOtherId(), line);
        }
        for (RiskCriteriaOther criterion : matchingCriteria) {
            if (!existingByCriteria.containsKey(criterion.getId())) {
                RiskAssessmentOtherLine line = new RiskAssessmentOtherLine();
                line.setTenantId(tenantId);
                line.setHeaderId(header.getId());
                line.setCriteriaOtherId(criterion.getId());
                repository.save(line);
            }
        }
    }

    @Transactional
    public List<RiskAssessmentOtherLineResponse> listByHeader(RiskAssessmentOtherHeader header) {
        ensureLines(header);
        return listByHeaderReadOnly(header);
    }

    /** Nhu listByHeader nhung KHONG tu sinh dong con thieu - dung khi xuat Excel/Word (tranh ghi
     * du lieu trong 1 giao dich chi-doc). */
    @Transactional(readOnly = true)
    public List<RiskAssessmentOtherLineResponse> listByHeaderReadOnly(RiskAssessmentOtherHeader header) {
        UUID tenantId = header.getTenantId();
        Map<UUID, RiskCriteriaOther> criteria = criteriaById(tenantId);
        Map<UUID, RiskCriteriaOtherScale> scales = scalesById(tenantId);
        return repository.findByHeaderIdOrderByCriteriaOtherIdAsc(header.getId()).stream()
                .map(line -> toResponse(line, criteria, scales))
                .sorted((a, b) -> {
                    String codeA = a.criteriaOtherCode() != null ? a.criteriaOtherCode() : "";
                    String codeB = b.criteriaOtherCode() != null ? b.criteriaOtherCode() : "";
                    return codeA.compareTo(codeB);
                })
                .toList();
    }

    /** Xoa han 1 dong chi tieu khoi header. Luu y: neu header duoc update() lai sau do, ensureLines()
     * se tao lai dong cho chi tieu nay neu chi tieu van con phu hop voi category cua header. */
    @Transactional
    public void delete(RiskAssessmentOtherHeader header, UUID lineId) {
        RiskAssessmentOtherLine line = repository.findById(lineId)
                .filter(l -> l.getHeaderId().equals(header.getId()))
                .orElseThrow(() -> new BusinessException("RISK_ASSESSMENT_OTHER_LINE_NOT_FOUND", "Khong tim thay dong cham diem", HttpStatus.NOT_FOUND));
        repository.delete(line);
        auditLogService.record("RiskAssessmentOtherLine", lineId, AuditAction.DELETE, "Xoa dong chi tieu DGRR khac");
    }

    @Transactional
    public RiskAssessmentOtherLineResponse updateScore(RiskAssessmentOtherHeader header, UUID lineId, RiskAssessmentOtherLineRequest request) {
        RiskAssessmentOtherLine line = repository.findById(lineId)
                .filter(l -> l.getHeaderId().equals(header.getId()))
                .orElseThrow(() -> new BusinessException("RISK_ASSESSMENT_OTHER_LINE_NOT_FOUND", "Khong tim thay dong cham diem", HttpStatus.NOT_FOUND));

        if (request.scaleId() != null) {
            RiskCriteriaOtherScale scale = scaleRepository.findById(request.scaleId())
                    .filter(s -> s.getTenantId().equals(header.getTenantId()))
                    .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_SCALE_NOT_FOUND", "Khong tim thay muc thang diem"));
            if (!scale.getCriteriaOtherId().equals(line.getCriteriaOtherId())) {
                throw new BusinessException("RISK_ASSESSMENT_OTHER_SCALE_MISMATCH", "Muc thang diem khong thuoc chi tieu cua dong nay");
            }
        }
        line.setScaleId(request.scaleId());
        line = repository.save(line);

        auditLogService.record("RiskAssessmentOtherLine", line.getId(), AuditAction.UPDATE, "Cham diem chi tieu DGRR khac");

        Map<UUID, RiskCriteriaOther> criteria = criteriaById(header.getTenantId());
        Map<UUID, RiskCriteriaOtherScale> scales = scalesById(header.getTenantId());
        return toResponse(line, criteria, scales);
    }

    /** Dung khi import Excel: tim chi tieu theo ma (trong dung category cua header) + muc thang diem
     * theo diem so, roi gan vao dong tuong ung (tao dong neu chua co). */
    @Transactional
    public void setScoreByCriteriaCode(UUID tenantId, RiskAssessmentOtherHeader header, String criteriaCode, Integer score) {
        RiskCriteriaOther criterion;
        try {
            criterion = criteriaOtherRepository
                    .findByTenantIdAndAuditObjectCategoryIdAndCode(tenantId, header.getAuditObjectCategoryId(), criteriaCode)
                    .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_NOT_FOUND", "Khong tim thay chi tieu: " + criteriaCode));
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new BusinessException("RISK_CRITERIA_OTHER_DUPLICATE",
                    "Danh muc chi tieu dang co nhieu hon 1 dong trung ma: " + criteriaCode
                            + " - vui long kiem tra va xoa/gop dong trung trong danh muc Chi tieu rui ro khac");
        }
        RiskCriteriaOtherScale scale = scaleRepository
                .findByTenantIdAndCriteriaOtherIdAndScaleScore(tenantId, criterion.getId(), score)
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_SCALE_NOT_FOUND",
                        "Khong tim thay muc thang diem " + score + " cho chi tieu " + criteriaCode));

        RiskAssessmentOtherLine line = repository.findByHeaderIdAndCriteriaOtherId(header.getId(), criterion.getId())
                .orElseGet(() -> {
                    RiskAssessmentOtherLine l = new RiskAssessmentOtherLine();
                    l.setTenantId(tenantId);
                    l.setHeaderId(header.getId());
                    l.setCriteriaOtherId(criterion.getId());
                    return l;
                });
        line.setScaleId(scale.getId());
        repository.save(line);
    }

    private Map<UUID, RiskCriteriaOther> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaOther> map = new HashMap<>();
        for (RiskCriteriaOther c : criteriaOtherRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<UUID, RiskCriteriaOtherScale> scalesById(UUID tenantId) {
        Map<UUID, RiskCriteriaOtherScale> map = new HashMap<>();
        for (RiskCriteriaOtherScale s : scaleRepository.findByTenantIdOrderByCriteriaOtherIdAscScaleScoreAsc(tenantId)) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private RiskAssessmentOtherLineResponse toResponse(RiskAssessmentOtherLine line, Map<UUID, RiskCriteriaOther> criteria,
                                                         Map<UUID, RiskCriteriaOtherScale> scales) {
        RiskCriteriaOther criterion = criteria.get(line.getCriteriaOtherId());
        RiskCriteriaOtherScale scale = line.getScaleId() != null ? scales.get(line.getScaleId()) : null;
        return new RiskAssessmentOtherLineResponse(line.getId(), line.getHeaderId(),
                line.getCriteriaOtherId(), criterion != null ? criterion.getCode() : null, criterion != null ? criterion.getName() : null,
                line.getScaleId(), scale != null ? scale.getScaleScore() : null, scale != null ? scale.getRatingLevel() : null);
    }
}
