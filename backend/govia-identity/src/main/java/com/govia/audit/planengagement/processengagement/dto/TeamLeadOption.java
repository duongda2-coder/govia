package com.govia.audit.planengagement.processengagement.dto;

import java.util.UUID;

/** Lookup nhe cho Select "Truong doan" - loc theo AuditEmployeeCapability.truongDoanCapable, giong
 * het cach lam cua man hinh CKT (don vi/chi nhanh) - xem AuditEngagementService.listEmployeeOptions(). */
public record TeamLeadOption(UUID id, String employeeCode, String fullName, boolean truongDoanCapable) {
}
