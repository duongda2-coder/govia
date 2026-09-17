package com.govia.audit.khkt.bp.dto;

import com.govia.audit.khkt.common.entity.AuditKhktSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Dung khi them 1 doi tuong kiem toan vao danh sach de xuat (Phan 1) - chon tu ket qua cham
 * diem rui ro chi nhanh hoac doi tuong khac, snapshot lai riskScore/rankLabel ngay tai thoi diem
 * them (xem AuditKhktBpService.create). auditObjectName toi da 500 ky tu de khop voi ten doi tuong
 * "Quy trinh" (AuditObjectProcess.name), dai hon nhieu so voi Chi nhanh/Cong ty con/Du an. */
public record AuditKhktBpCandidateRequest(
        @NotNull UUID departmentId,
        @NotNull Integer year,
        @NotNull AuditKhktSourceType sourceType,
        @NotBlank @Size(max = 20) String auditObjectCode,
        @NotBlank @Size(max = 500) String auditObjectName,
        String auditObjectCategoryCode,
        BigDecimal riskScore,
        String rankLabel,
        List<UUID> businessSegmentIds
) {
}
