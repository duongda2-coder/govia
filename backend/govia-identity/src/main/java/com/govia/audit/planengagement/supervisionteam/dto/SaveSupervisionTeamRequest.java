package com.govia.audit.planengagement.supervisionteam.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** "Luu to giam sat" - thay the TOAN BO danh sach thanh vien hien tai bang employeeIds nay. */
public record SaveSupervisionTeamRequest(
        @NotNull List<UUID> employeeIds
) {
}
