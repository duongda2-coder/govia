package com.govia.audit.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Gợi ý chấm ĐGCL" (A6, G4) cho 1 phieu PL01A/PL01B/PL01F cua 1 thanh vien (subjectKey nhu man DGCL; TEAM = ca doan). */
public record DgclScoreDraftRequest(
        @NotNull UUID engagementId,
        @NotBlank @Size(max = 64) String subjectKey,
        @NotBlank @Size(max = 10) String appendix,
        @Size(max = 500) String instruction
) {
}
