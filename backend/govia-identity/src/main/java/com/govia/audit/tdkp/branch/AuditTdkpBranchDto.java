package com.govia.audit.tdkp.branch;

import com.govia.audit.tdkp.common.TdkpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpBranchDto {

    private AuditTdkpBranchDto() {
    }

    /** Mã quản lý kiến nghị KHÔNG có trong request - hệ thống tự sinh, bất biến. */
    public record RecommendationRequest(
            @NotNull UUID auditObjectUnitId,
            Integer auditYear,
            @NotBlank @Size(max = 2000) String content,
            UUID recommendationTypeId,
            UUID businessSegmentId,
            LocalDate deadline,
            @Size(max = 500) String editContent,
            TdkpStatus status,
            @Size(max = 500) String evaluation,
            @Size(max = 500) String note
    ) {
    }

    public record RecommendationResponse(
            UUID id,
            String managementCode,
            UUID auditObjectUnitId,
            String branchName,
            String branchCode,
            Integer auditYear,
            UUID engagementId,
            String content,
            UUID recommendationTypeId,
            String recommendationTypeName,
            UUID businessSegmentId,
            String businessSegmentCode,
            LocalDate deadline,
            String editContent,
            TdkpStatus status,
            String statusLabel,
            String evaluation,
            String deadlineState,
            String deadlineStateLabel,
            LocalDate lastEditedDate,
            String note,
            /** Số tồn tại sai sót liên quan (số dòng ở bảng chi tiết). */
            int defectCount,
            /** Số tồn tại sai sót đã chỉnh sửa (hiện trạng chỉnh sửa liên quan đến khách hàng = Đã thực hiện). */
            int defectDoneCount
    ) {
    }

    public record DefectRequest(
            @NotNull UUID branchRecommendationId,
            @Size(max = 2000) String defectContent,
            @Size(max = 500) String customerEntry,
            @Size(max = 100) String creditContract,
            @Size(max = 255) String defectType,
            TdkpStatus customerStatus,
            @Size(max = 255) String relatedStaff
    ) {
    }

    public record DefectResponse(
            UUID id,
            UUID branchRecommendationId,
            String managementCode,
            String defectContent,
            String customerEntry,
            String creditContract,
            String defectType,
            /** Hiện trạng chỉnh sửa sai sót liên quan đến kiến nghị - hệ thống đếm và so sánh các dòng cùng kiến nghị. */
            TdkpStatus recommendationDefectStatus,
            String recommendationDefectStatusLabel,
            TdkpStatus customerStatus,
            String customerStatusLabel,
            String relatedStaff
    ) {
    }

    /** Kết quả chuyển từ phân hệ Thực hiện kiểm toán. */
    public record TransferResult(int recommendationsCreated, int defectsCreated, int skipped) {
    }
}
