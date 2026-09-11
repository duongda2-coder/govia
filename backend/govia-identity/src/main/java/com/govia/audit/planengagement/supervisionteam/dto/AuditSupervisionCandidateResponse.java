package com.govia.audit.planengagement.supervisionteam.dto;

import java.util.UUID;

/** 1 dong trong man hinh "Chon to giam sat" - nhan vien co to_giam_sat_capable=true, danh dau
 * selected=true neu da co trong audit_supervision_team_member cua CKT dang xet. */
public record AuditSupervisionCandidateResponse(
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String username,
        boolean selected
) {
}
