package com.govia.audit.planengagement.dto;

import java.util.UUID;

/** Lookup nhe cho Select "Loai doi tuong" / "Ma DTKT" - tranh phu thuoc quyen AUDIT.RISK_SCORING.*
 * chi vi can doc danh sach doi tuong kiem toan. */
public record AuditObjectUnitOption(UUID id, String code, String name, String unitType) {
}
