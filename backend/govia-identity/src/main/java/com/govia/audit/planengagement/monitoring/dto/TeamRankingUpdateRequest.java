package com.govia.audit.planengagement.monitoring.dto;

import jakarta.validation.constraints.Size;

/** "Xếp loại đoàn" - man hinh "Quan ly dot kiem toan", user tu nhap truc tiep tren danh sach. */
public record TeamRankingUpdateRequest(
        @Size(max = 50) String teamRanking
) {
}
