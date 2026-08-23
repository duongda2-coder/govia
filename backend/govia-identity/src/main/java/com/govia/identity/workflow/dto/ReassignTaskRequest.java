package com.govia.identity.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Chuyen tiep (forward) 1 task cho nguoi khac - gan assignee moi, vinh vien, khong quay lai nguoi cu. */
public record ReassignTaskRequest(@NotBlank String assigneeUserId) {
}
