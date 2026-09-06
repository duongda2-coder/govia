package com.govia.audit.planengagement.monitoring.dto;

/** So cong viec da "Hoan thanh" (DONE) / tong so cong viec duoc phan cong, dung chung cho ca 3 cot
 * tien do (CBKT, THKT co mau, THKT khong mau) cua man hinh "Chi tiet doan kiem toan". */
public record ProgressStat(int completed, int total) {
}
