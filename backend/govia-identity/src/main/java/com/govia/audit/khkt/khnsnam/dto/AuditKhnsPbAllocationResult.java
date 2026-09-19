package com.govia.audit.khkt.khnsnam.dto;

import java.util.List;

/** Ket qua nut "Phan bo nhan su" o KHNS_PB: objectCount = so doi tuong kiem toan (da khai bao thang)
 * can bo tri doan, fullyStaffedCount = so doan da du Truong doan + du cac vi tri, warnings = ly do
 * cac doan chua du (de NSD bo sung nhan su/kha nang dam nhan roi phan bo lai). */
public record AuditKhnsPbAllocationResult(int objectCount, int fullyStaffedCount, int employeesAssigned, List<String> warnings) {
}
