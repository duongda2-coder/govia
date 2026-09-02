package com.govia.audit.planengagement.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.util.UUID;

/** Cong viec kiem toan "du dieu kien" (theo nghiep vu cua thanh vien) nhung chua duoc phan cong -
 * nguon du lieu cho modal "Chọn công việc" o man hinh Phan cong. */
public record EligibleWorkItemResponse(UUID id, AuditWorkPhase phase, String code, String name) {
}
