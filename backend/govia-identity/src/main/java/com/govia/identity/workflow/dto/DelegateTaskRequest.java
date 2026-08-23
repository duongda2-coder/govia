package com.govia.identity.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/** Uy quyen (delegate) 1 task cho nguoi khac lam TAM THOI - ho xu ly xong goi resolve, task quay ve
 * lai nguoi uy quyen (owner) de duyet/hoan tat cuoi cung, khac voi reassign la chuyen han. */
public record DelegateTaskRequest(@NotBlank String delegateUserId) {
}
