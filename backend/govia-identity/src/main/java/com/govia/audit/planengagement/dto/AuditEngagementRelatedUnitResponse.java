package com.govia.audit.planengagement.dto;

import java.util.UUID;

public record AuditEngagementRelatedUnitResponse(
        UUID id,
        UUID auditEngagementId,
        String engagementCode,
        UUID auditObjectUnitId,
        String auditObjectUnitCode,
        String auditObjectUnitName,
        String unitType
) {
}
