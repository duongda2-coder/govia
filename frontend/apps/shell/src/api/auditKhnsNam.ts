import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditKhnsRoleInTeam = "TEAM_LEAD" | "GROUP_LEAD" | "MEMBER" | "SUPPORT";
export type EmployeeAuditorClassification = "TYPE_1" | "TYPE_2" | "TYPE_3";

export interface AuditKhnsNamRowItem {
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  positionName: string | null;
  departmentCode: string | null;
  auditorClassification: EmployeeAuditorClassification | null;
  businessSegmentCode: string | null;
  teamLeadCapable: boolean;
  roleInTeam: AuditKhnsRoleInTeam | null;
  otherDuties: string | null;
  auditObjectCodes: string[];
  auditObjectNames: string[];
  objectBusinessSegmentCodes: string[];
  decisionNumber: string | null;
  decisionDate: string | null;
  expectedBatch: string | null;
  totalTeamsCount: number;
  note: string | null;
  month1AuditObjectCode: string | null;
  month2AuditObjectCode: string | null;
  month3AuditObjectCode: string | null;
  month4AuditObjectCode: string | null;
  month5AuditObjectCode: string | null;
  month6AuditObjectCode: string | null;
  month7AuditObjectCode: string | null;
  month8AuditObjectCode: string | null;
  month9AuditObjectCode: string | null;
  month10AuditObjectCode: string | null;
  month11AuditObjectCode: string | null;
  month12AuditObjectCode: string | null;
}

export interface AuditKhnsNamUpdateRequest {
  roleInTeam: AuditKhnsRoleInTeam | null;
  otherDuties: string | null;
  auditObjectCodes: string[];
  decisionNumber: string | null;
  decisionDate: string | null;
  expectedBatch: string | null;
  note: string | null;
  month1AuditObjectCode: string | null;
  month2AuditObjectCode: string | null;
  month3AuditObjectCode: string | null;
  month4AuditObjectCode: string | null;
  month5AuditObjectCode: string | null;
  month6AuditObjectCode: string | null;
  month7AuditObjectCode: string | null;
  month8AuditObjectCode: string | null;
  month9AuditObjectCode: string | null;
  month10AuditObjectCode: string | null;
  month11AuditObjectCode: string | null;
  month12AuditObjectCode: string | null;
}

const BASE = "/api/audit/plan/khns-nam";

export async function listAuditKhnsNam(year: number): Promise<AuditKhnsNamRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhnsNamRowItem[]>>(BASE, { params: { year } });
  return res.data.data;
}

export async function updateAuditKhnsNam(
  employeeId: string,
  year: number,
  request: AuditKhnsNamUpdateRequest,
): Promise<AuditKhnsNamRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhnsNamRowItem>>(`${BASE}/${employeeId}`, request, { params: { year } });
  return res.data.data;
}

export async function updateAuditKhnsNamNote(employeeId: string, year: number, note: string | null): Promise<AuditKhnsNamRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhnsNamRowItem>>(`${BASE}/${employeeId}/note`, { note }, { params: { year } });
  return res.data.data;
}

export async function exportAuditKhnsNamMonthlyReport(year: number, month: number): Promise<void> {
  const res = await httpClient.get(`${BASE}/export`, { params: { year, month }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = `bao_cao_khns_thang_${month}_${year}.xlsx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export interface AuditKhnsPbAllocationResult {
  objectCount: number;
  fullyStaffedCount: number;
  employeesAssigned: number;
  warnings: string[];
}

/** Nut "Phân bổ nhân sự" ở KHNS_PB - tự động phân bổ cán bộ cho cả năm theo số tháng kiểm toán đã khai báo. */
export async function allocateAuditKhnsPb(year: number): Promise<AuditKhnsPbAllocationResult> {
  const res = await httpClient.post<ApiResponse<AuditKhnsPbAllocationResult>>(`${BASE}/allocate`, null, { params: { year } });
  return res.data.data;
}
