package com.govia.audit.planengagement.service;

import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import com.govia.audit.workitem.entity.AuditWorkItem;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitem.repository.AuditWorkItemRepository;
import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import com.govia.audit.workitemqt.repository.AuditWorkItemQtRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Doc du lieu cong viec (phase/ma/ten/mang nghiep vu/co chon mau) cua 1 danh sach
 * AuditEngagementAssignment tu DUNG catalog cho tung dong: workItemQtId != null nghia la CKT quy
 * trinh (AuditWorkItemQt), nguoc lai la CKT chi nhanh (AuditWorkItem) - xem
 * [[qt_work_item_catalog_separation]]/AuditEngagementTeamService.eligibleWorkItems(). Dung chung
 * cho moi noi doc lai du lieu cong viec tu 1 phan cong da luu (Quan ly cong viec, TTSS, Bao cao
 * tien do, Quan ly dot kiem toan) de tranh viet lai 2 lan logic phan nhanh nay.
 */
@Component
public class AuditAssignmentWorkItemResolver {

    private final AuditWorkItemRepository workItemRepository;
    private final AuditWorkItemQtRepository workItemQtRepository;

    public AuditAssignmentWorkItemResolver(AuditWorkItemRepository workItemRepository, AuditWorkItemQtRepository workItemQtRepository) {
        this.workItemRepository = workItemRepository;
        this.workItemQtRepository = workItemQtRepository;
    }

    /** Tra ve map khoa boi AuditEngagementAssignment.getId() (KHONG phai workItemId - vi 2 catalog
     * dung 2 khong gian id khac nhau va 1 trong 2 cot FK luon null) - dong nao khong resolve duoc
     * (catalog row bi xoa) se vang mat trong map, goi cho phai tu xu ly truong hop null. */
    public Map<UUID, ResolvedWorkItem> resolve(List<AuditEngagementAssignment> assignments) {
        Map<UUID, AuditWorkItem> workItems = workItemRepository.findAllById(assignments.stream()
                        .map(AuditEngagementAssignment::getWorkItemId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AuditWorkItem::getId, w -> w));
        Map<UUID, AuditWorkItemQt> workItemsQt = workItemQtRepository.findAllById(assignments.stream()
                        .map(AuditEngagementAssignment::getWorkItemQtId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AuditWorkItemQt::getId, w -> w));

        Map<UUID, ResolvedWorkItem> result = new HashMap<>();
        for (AuditEngagementAssignment assignment : assignments) {
            ResolvedWorkItem resolved;
            if (assignment.getWorkItemQtId() != null) {
                AuditWorkItemQt w = workItemsQt.get(assignment.getWorkItemQtId());
                resolved = w == null ? null : new ResolvedWorkItem(w.getPhase(), w.getCode(), w.getName(), w.getBusinessSegmentId(), w.isHasSampleSelection());
            } else {
                AuditWorkItem w = workItems.get(assignment.getWorkItemId());
                resolved = w == null ? null : new ResolvedWorkItem(w.getPhase(), w.getCode(), w.getName(), w.getBusinessSegmentId(), w.isHasSampleSelection());
            }
            if (resolved != null) {
                result.put(assignment.getId(), resolved);
            }
        }
        return result;
    }

    public record ResolvedWorkItem(AuditWorkPhase phase, String code, String name, UUID businessSegmentId, boolean hasSampleSelection) {
    }
}
