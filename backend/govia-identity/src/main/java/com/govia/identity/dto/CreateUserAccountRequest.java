package com.govia.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserAccountRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "Mat khau toi thieu 8 ky tu") String password
) {
}
