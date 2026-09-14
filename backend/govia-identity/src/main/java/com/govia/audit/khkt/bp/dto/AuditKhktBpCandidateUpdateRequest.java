package com.govia.audit.khkt.bp.dto;

import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;

import java.util.List;
import java.util.UUID;

/** Dung khi sua 1 dong da co trong danh sach de xuat (Phan 1) - CHI cho sua Ket qua ra soat, Lua
 * chon va Linh vuc de xuat KT; muon doi doi tuong/nam/phong thi phai xoa dong va them lai (xem
 * AuditKhktBpService.update). */
public record AuditKhktBpCandidateUpdateRequest(
        String reviewResult,
        AuditKhktSelectionDecision selectionDecision,
        List<UUID> businessSegmentIds
) {
}
