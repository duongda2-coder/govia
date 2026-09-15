package com.govia.audit.khkt.th.dto;

import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Dung chung cho ca danh sach Phan 1 (TH) va Phan 2 (TH2 - da xac nhan). "approvalStatus" luon
 * null o Phan 1 (chi ton tai tren AuditKhktThConfirmed). "proposingDepartmentCodes" va
 * "bpProposedSegmentCodes" tinh dong tu AuditKhktBpConfirmed (BP2) - danh sach phong da de xuat
 * doi tuong nay va hop cac linh vuc ho da de xuat, CHI DE THAM KHAO (khong phai TH de xuat that su -
 * xem thBusinessSegmentCodes). */
public record AuditKhktThRowResponse(
        UUID id,
        Integer year,
        AuditKhktSourceType sourceType,
        String auditObjectCode,
        String auditObjectName,
        String auditObjectCategoryCode,
        BigDecimal riskScore,
        String rankLabel,
        BigDecimal onBalanceSheetLoan,
        BigDecimal fundingSource,
        String bpReviewResult,
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
        AuditKhktApprovalStatus approvalStatus,
        List<String> proposingDepartmentCodes,
        List<String> bpProposedSegmentCodes,
        List<String> thBusinessSegmentCodes
) {
}
