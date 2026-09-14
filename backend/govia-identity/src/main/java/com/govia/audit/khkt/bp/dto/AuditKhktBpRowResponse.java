package com.govia.audit.khkt.bp.dto;

import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dung chung cho ca danh sach Phan 1 (De xuat) va Phan 2 (Da xac nhan) - cung 1 hinh dang du lieu.
 * "inspectionHistory": map 30 khoa dang "{LOAI}_T{1..5}" (vd "TTCP_T5") -> co/khong co lich su
 * thanh tra/giam sat/kiem toan nam tuong ung, tinh dong tu AuditObjectInspectionHistory theo
 * auditObjectUnitId + (year - offset) - xem AuditKhktBpService.buildInspectionHistory. */
public record AuditKhktBpRowResponse(
        UUID id,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        Integer year,
        AuditKhktSourceType sourceType,
        String auditObjectCode,
        String auditObjectName,
        String auditObjectCategoryCode,
        BigDecimal riskScore,
        String rankLabel,
        BigDecimal onBalanceSheetLoan,
        BigDecimal fundingSource,
        String reviewResult,
        AuditKhktSelectionDecision selectionDecision,
        List<String> businessSegmentCodes,
        Map<String, Boolean> inspectionHistory
) {
}
