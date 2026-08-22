package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "Mat khau moi toi thieu 8 ky tu") String newPassword
) {
}
