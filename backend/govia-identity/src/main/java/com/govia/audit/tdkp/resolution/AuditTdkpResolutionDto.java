package com.govia.audit.tdkp.resolution;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.tdkp.common.TdkpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AuditTdkpResolutionDto {

    private AuditTdkpResolutionDto() {
    }

    /** Mã quản lý KHÔNG có trong request - hệ thống tự sinh. */
    public record Request(
            @NotBlank @Size(max = 20) String resolutionNumber,
            LocalDate issueDate,
            @Size(max = 500) String content,
            @Size(max = 255) String workDetail,
            @Size(max = 100) String fieldArea,
            UUID unitId,
            @Size(max = 255) String contactPerson,
            @Size(max = 100) String relatedResolution,
            LocalDate completionDeadline,
            @Size(max = 100) String completionDeadlineBasis,
            @Size(max = 500) String implementation,
            TdkpStatus progressStatus,
            @Size(max = 100) String reason,
            @Size(max = 100) String issuanceEvaluation,
            LocalDate completionDate,
            @Size(max = 10) String followUpGroup,
            @Size(max = 100) String followerName,
            @Size(max = 100) String proposal,
            @Size(max = 100) String proposalReason,
            @Size(max = 500) String note,
            AssignmentApprovalStatus approvalStatus
    ) {
    }

    public record Response(
            UUID id,
            String code,
            String resolutionNumber,
            LocalDate issueDate,
            String content,
            String workDetail,
            String fieldArea,
            UUID unitId,
            String unitCode,
            String unitName,
            String contactPerson,
            String relatedResolution,
            LocalDate completionDeadline,
            String completionDeadlineBasis,
            String implementation,
            TdkpStatus progressStatus,
            String progressStatusLabel,
            String reason,
            String issuanceEvaluation,
            LocalDate completionDate,
            /** "Trạng thái NQ (QH,TH)" - tinh tu completionDeadline so voi ngay hien tai, giong deadlineState cac man hinh TDKP khac. */
            String resolutionState,
            String resolutionStateLabel,
            String followUpGroup,
            String followUpGroupLabel,
            String followerName,
            String proposal,
            String proposalReason,
            String note,
            AssignmentApprovalStatus approvalStatus,
            String approvalStatusLabel
    ) {
    }
}
