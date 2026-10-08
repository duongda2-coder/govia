package com.govia.audit.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** "AI soạn thư đôn đốc" tren 1 man hinh TDKP. source: CEO_ALL | CEO_KH | BRANCH | RESOLUTION | UNIT.
 * itemIds: cac dong nguoi dung dang chon; rong = moi dong chua xong da qua han hoac sap den han. */
public record ReminderDraftRequest(
        @NotBlank String source,
        @Size(max = 200) List<UUID> itemIds,
        Integer dueWithinDays
) {
}
