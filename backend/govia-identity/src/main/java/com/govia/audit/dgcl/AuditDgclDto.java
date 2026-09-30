package com.govia.audit.dgcl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditDgclDto {

    private AuditDgclDto() {
    }

    /** Quyen cua NSD hien tai theo man hinh KNDN (cot "Thực hiện ĐGCL"/"Kiểm soát ĐGCL"). */
    public record Capability(boolean canEvaluate, boolean canControl) {
    }

    /** 1 dong man hinh "Đánh giá chất lượng" - 1 thanh vien doan hoac dong cuoi "Cuộc kiểm toán" (team = true).
     * test 30.9: diem PL01A/PL01B/PL01F + diem cong/tru/xep loai lay tu phieu da Lưu ben trong (xac nhan hay chua);
     * cac co plXxConfirmed cho biet phieu nao da "Xác nhận hoàn thành ĐGCL" (PL04B1 chi dung ket qua da xac nhan). */
    public record SubjectRow(
            String subjectKey,
            boolean team,
            String engagementCode,
            UUID employeeId,
            String employeeCode,
            String employeeName,
            String segmentCodes,
            String role,
            BigDecimal pl01aScore,
            BigDecimal pl01bScore,
            BigDecimal pl01fScore,
            BigDecimal bonusPoints,
            BigDecimal penaltyPoints,
            String classification,
            boolean pl01aConfirmed,
            boolean pl01bConfirmed,
            boolean pl01fConfirmed,
            int confirmedCount,
            int controlledCount,
            String evaluatorNames,
            String controllerNames
    ) {
    }

    public record Line(
            String key,
            String stt,
            String content,
            boolean header,
            String segment,
            String kind,
            Double rate,
            /** cot "tick" cua file 01A/pl01b: tieu chi can cham. */
            boolean tick,
            boolean required,
            boolean compliant,
            boolean nonCompliant,
            boolean checked,
            Integer violationCount,
            BigDecimal maxScore,
            String detail,
            String document,
            String note,
            String evaluatorName,
            Double calcMax,
            Double calcDeduction,
            Double calcPoints,
            Double calcRatio,
            UUID attachmentEntityId
    ) {
    }

    /** Dong tong hop cuoi phieu: PL01A/B = dong IV-VI (Tong cong/Ty le/Diem cham); PL01F = dong VI + xep loai. */
    public record Summary(
            Integer requiredCount,
            Integer compliantCount,
            Integer nonCompliantCount,
            Double ratio,
            Double score,
            Double weightedMax,
            Double bonusPoints,
            Double penaltyPoints,
            String classification
    ) {
    }

    public record Sheet(
            UUID engagementId,
            String engagementCode,
            String subjectKey,
            boolean team,
            String subjectName,
            String segmentCodes,
            DgclAppendix appendix,
            List<Line> lines,
            Summary summary,
            /** false = phieu chua luu lan nao, cac o NDTH/Tuân thủ dang la gia tri tich san (chua tinh vao diem ngoai). */
            boolean saved,
            String evaluatorName,
            boolean confirmed,
            String confirmedBy,
            Instant confirmedAt,
            boolean controlled,
            String controlledBy,
            Instant controlledAt,
            boolean canEdit,
            boolean canConfirm,
            boolean canUnconfirm,
            boolean canControl,
            boolean canUncontrol
    ) {
    }

    public record LineInput(
            @NotBlank String key,
            boolean required,
            boolean compliant,
            boolean nonCompliant,
            boolean checked,
            @Min(0) Integer violationCount,
            @PositiveOrZero BigDecimal maxScore,
            @Size(max = 2000) String detail,
            @Size(max = 1000) String document,
            @Size(max = 1000) String note
    ) {
    }

    public record SaveRequest(@NotNull @Valid List<LineInput> lines) {
    }
}
