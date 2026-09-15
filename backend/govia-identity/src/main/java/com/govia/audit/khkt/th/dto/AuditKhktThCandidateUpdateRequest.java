package com.govia.audit.khkt.th.dto;

import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;

import java.util.List;
import java.util.UUID;

/** Dung khi sua 1 dong TH1 - CHI cho sua phan "cua rieng TH" (Can cu de xuat TH, Y kien chuyen
 * gia, 3 Lua chon, Linh vuc kiem toan, cac truong dieu chinh ke hoach, TH de xuat LVKT); cac
 * truong con lai duoc dong bo tu BP2 qua sync(), khong sua truc tiep o day (xem AuditKhktThService). */
public record AuditKhktThCandidateUpdateRequest(
        String proposalBasisTh,
        String expertOpinion,
        boolean selection1,
        boolean selection2,
        boolean selection3,
        String auditScope,
        AuditKhktSelectionChoice adhocAuditOrSupervision,
        AuditKhktSelectionChoice planAdjustment,
        String adjustmentReason,
        AuditKhktSelectionChoice khktgsAfterAdjustment,
        List<UUID> thBusinessSegmentIds
) {
}
