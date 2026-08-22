package com.govia.identity.dto;

import java.util.List;

public record RolePermissionsRequest(
        List<String> permissionCodes
) {
}
