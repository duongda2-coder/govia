package com.govia.audit.planengagement.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Payload cua nut "Chọn công việc" - danh sach cong viec kiem toan duoc chon them thu cong. */
public record AssignWorkItemsRequest(@NotEmpty List<UUID> workItemIds) {
}
