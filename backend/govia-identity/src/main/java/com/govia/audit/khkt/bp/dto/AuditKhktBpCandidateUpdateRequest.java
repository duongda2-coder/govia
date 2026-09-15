package com.govia.audit.khkt.bp.dto;

import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;

import java.util.List;
import java.util.UUID;

/** Dung khi sua 1 dong da co trong danh sach de xuat (Phan 1) - CHI cho sua Ket qua ra soat, Can
 * cu de xuat, cac Lua chon, Linh vuc kiem toan, Linh vuc de xuat KT va cac truong dieu chinh ke
 * hoach; muon doi doi tuong/nam/phong thi phai xoa dong va them lai (xem AuditKhktBpService.update). */
public record AuditKhktBpCandidateUpdateRequest(
        String reviewResult,
        AuditKhktSelectionDecision selectionDecision,
        String proposalBasis,
        AuditKhktSelectionChoice approvedSelection,
        AuditKhktSelectionChoice expectedSelection,
        String auditScope,
        AuditKhktSelectionChoice adhocAuditOrSupervision,
        AuditKhktSelectionChoice planAdjustment,
        String adjustmentReason,
        AuditKhktSelectionChoice khktgsAfterAdjustment,
        List<UUID> businessSegmentIds
) {
}
