package com.govia.audit.planengagement.monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** "Điểm/Xếp loại/Ghi chú thành viên" - man hinh "Chi tiet doan kiem toan", user tu nhap truc tiep
 * tren tung dong thanh vien. */
public record TeamMemberScoringRequest(
        @Digits(integer = 3, fraction = 2) BigDecimal score,
        @Size(max = 50) String ranking,
        @Size(max = 1000) String note
) {
}
