package com.govia.identity.dto;

import java.util.UUID;

public record CopyRolesRequest(
        UUID sourceAccountId
) {
}
