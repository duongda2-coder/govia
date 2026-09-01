package com.govia.audit.cmntd6.dto;

import java.util.UUID;

public record AuditCmNtd6Response(
        UUID id,
        String branchCode,
        String staffCode,
        String staffName,
        String ipcasUser,
        String adUser,
        String securityDevice,
        String sampleReason,
        String sampleCode,
        String auditResult,
        boolean active
) {
}
