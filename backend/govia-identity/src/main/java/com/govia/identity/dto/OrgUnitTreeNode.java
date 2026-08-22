package com.govia.identity.dto;

import java.util.List;
import java.util.UUID;

public record OrgUnitTreeNode(
        UUID id,
        String code,
        String name,
        String type,
        String levelCode,
        boolean active,
        List<OrgUnitTreeNode> children
) {
}
