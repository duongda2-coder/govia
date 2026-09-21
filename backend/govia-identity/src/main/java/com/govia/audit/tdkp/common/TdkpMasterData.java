package com.govia.audit.tdkp.common;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tra cuu danh muc dung chung cho cac man hinh TDKP: cot dang "List" phai chon/nhap dung danh muc (kiem tra ca luc Import Excel). */
@Component
public class TdkpMasterData {

    private final AuditMasterDataItemRepository itemRepository;
    private final AuditObjectUnitRepository unitRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    public TdkpMasterData(AuditMasterDataItemRepository itemRepository, AuditObjectUnitRepository unitRepository,
                          EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository) {
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public Map<UUID, AuditMasterDataItem> items(AuditMasterDataCategory category) {
        return itemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(TenantContext.getTenantId(), category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Map<UUID, AuditObjectUnit> units() {
        return unitRepository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .collect(Collectors.toMap(AuditObjectUnit::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Map<UUID, Employee> employees() {
        return employeeRepository.findByTenantIdOrderByFullNameAsc(TenantContext.getTenantId()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /** employeeId -> username cua tai khoan he thong gan voi can bo (neu co). */
    public Map<UUID, String> usernames(Collection<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findByEmployeeIdIn(employeeIds).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
    }

    /** Kiem tra id (neu co) thuoc dung danh muc cua tenant - tra ve chinh id do. */
    public UUID requireItem(AuditMasterDataCategory category, UUID id, String fieldLabel) {
        if (id != null && !items(category).containsKey(id)) {
            throw new BusinessException("TDKP_LIST_VALUE_NOT_FOUND", fieldLabel + " khong ton tai trong danh muc");
        }
        return id;
    }

    public UUID requireUnit(UUID id, String fieldLabel) {
        if (id != null && !units().containsKey(id)) {
            throw new BusinessException("TDKP_LIST_VALUE_NOT_FOUND", fieldLabel + " khong ton tai trong danh muc doi tuong kiem toan");
        }
        return id;
    }

    public UUID requireEmployee(UUID id, String fieldLabel) {
        if (id != null && !employees().containsKey(id)) {
            throw new BusinessException("TDKP_LIST_VALUE_NOT_FOUND", fieldLabel + " khong ton tai trong danh sach can bo");
        }
        return id;
    }

    /** Import Excel: tim dong danh muc theo ma hoac ten; rong -> null; khong thay -> loi dong import. */
    public UUID resolveItem(AuditMasterDataCategory category, String text, String fieldLabel) {
        if (TdkpSupport.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        List<AuditMasterDataItem> all = List.copyOf(items(category).values());
        return all.stream().filter(i -> value.equalsIgnoreCase(i.getCode())).findFirst()
                .or(() -> all.stream().filter(i -> value.equalsIgnoreCase(i.getName())).findFirst())
                .map(AuditMasterDataItem::getId)
                .orElseThrow(() -> new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", fieldLabel + ": khong tim thay '" + value + "' trong danh muc"));
    }

    public UUID resolveUnit(String text, String fieldLabel) {
        if (TdkpSupport.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        List<AuditObjectUnit> all = List.copyOf(units().values());
        return all.stream().filter(u -> value.equalsIgnoreCase(u.getCode())).findFirst()
                .or(() -> all.stream().filter(u -> value.equalsIgnoreCase(u.getName())).findFirst())
                .map(AuditObjectUnit::getId)
                .orElseThrow(() -> new BusinessException("IMPORT_LIST_VALUE_NOT_FOUND", fieldLabel + ": khong tim thay doi tuong '" + value + "'"));
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
