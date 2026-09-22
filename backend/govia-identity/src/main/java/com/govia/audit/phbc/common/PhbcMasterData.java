package com.govia.audit.phbc.common;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tra cuu danh muc dung chung cho man hinh Phat hanh bao cao: Mang nghiep vu + Don vi thuc hien (Doi tuong kiem toan, loai doi tuong = 'HO'). */
@Component
public class PhbcMasterData {

    /** "Loại đối tượng" cần lọc cho cột "Đơn vị thực hiện" - Hội sở thật, khác GSCC/Chi nhánh (xem RiskAssessmentOtherRankingService). */
    private static final String HEAD_OFFICE_UNIT_TYPE = "HO";

    private final AuditMasterDataItemRepository itemRepository;
    private final AuditObjectUnitRepository unitRepository;

    public PhbcMasterData(AuditMasterDataItemRepository itemRepository, AuditObjectUnitRepository unitRepository) {
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
    }

    public Map<UUID, AuditMasterDataItem> businessSegments() {
        return itemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(TenantContext.getTenantId(), AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /** "Đơn vị thực hiện": chỉ các đối tượng kiểm toán có loại đối tượng = 'HO' (Hội sở). */
    public Map<UUID, AuditObjectUnit> executingUnits() {
        return unitRepository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .filter(u -> HEAD_OFFICE_UNIT_TYPE.equalsIgnoreCase(u.getUnitType()))
                .collect(Collectors.toMap(AuditObjectUnit::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public UUID requireBusinessSegment(UUID id) {
        if (id != null && !businessSegments().containsKey(id)) {
            throw new BusinessException("PHBC_LIST_VALUE_NOT_FOUND", "Mang nghiep vu khong ton tai trong danh muc");
        }
        return id;
    }

    public UUID requireExecutingUnit(UUID id) {
        if (id != null && !executingUnits().containsKey(id)) {
            throw new BusinessException("PHBC_LIST_VALUE_NOT_FOUND", "Don vi thuc hien khong ton tai trong danh muc doi tuong kiem toan (loai HO)");
        }
        return id;
    }

    /** Import Excel: tim mang nghiep vu theo ma hoac ten; rong -> null; khong thay -> loi dong import. */
    public UUID resolveBusinessSegment(String text) {
        if (PhbcSupport.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        List<AuditMasterDataItem> all = List.copyOf(businessSegments().values());
        return all.stream().filter(i -> value.equalsIgnoreCase(i.getCode())).findFirst()
                .or(() -> all.stream().filter(i -> value.equalsIgnoreCase(i.getName())).findFirst())
                .map(AuditMasterDataItem::getId)
                .orElseThrow(() -> new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Mang nghiep vu: khong tim thay '" + value + "' trong danh muc"));
    }

    public UUID resolveExecutingUnit(String text) {
        if (PhbcSupport.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        List<AuditObjectUnit> all = List.copyOf(executingUnits().values());
        return all.stream().filter(u -> value.equalsIgnoreCase(u.getCode())).findFirst()
                .or(() -> all.stream().filter(u -> value.equalsIgnoreCase(u.getName())).findFirst())
                .map(AuditObjectUnit::getId)
                .orElseThrow(() -> new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", "Don vi thuc hien: khong tim thay '" + value + "'"));
    }

    public static String nameOf(Map<UUID, AuditMasterDataItem> items, UUID id) {
        AuditMasterDataItem item = id == null ? null : items.get(id);
        return item == null ? null : item.getName();
    }

    public static String codeOf(Map<UUID, AuditMasterDataItem> items, UUID id) {
        AuditMasterDataItem item = id == null ? null : items.get(id);
        return item == null ? null : item.getCode();
    }
}
