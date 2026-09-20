import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditKhnsRoleInTeam = "TEAM_LEAD" | "GROUP_LEAD" | "MEMBER" | "SUPPORT";

/** Chức vụ chi tiết của cán bộ tại 1 đơn vị (màn KHNS_PB) - 1 cán bộ giữ được nhiều chức vụ cùng lúc. */
export type AuditKhnsPosition =
  | "TEAM_LEAD"
  | "QTDH_GROUP_LEAD"
  | "QTDH_MEMBER"
  | "TD_GROUP_LEAD"
  | "TD_MEMBER"
  | "NTD_GROUP_LEAD"
  | "NTD_MEMBER";

export interface AuditKhnsObjectAssignment {
  auditObjectCode: string;
  positions: AuditKhnsPosition[];
  segmentCodes: string[];
}

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
  year: number;
  /** Chức vụ + nghiệp vụ của cán bộ ở màn KHNS_PB, mỗi đơn vị khác nhau 1 phần tử - hiện y nguyên cột "Chức vụ" của màn đó. */
  positionDetails: { positions: AuditKhnsPosition[]; segmentNames: string[] }[];
  /** Tên đối tượng kiểm toán của tháng 1..12 (lấy từ KHNS_PB), null nếu tháng đó không đi kiểm toán. */
  monthAuditObjectNames: (string | null)[];
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
  /** Chỉ KHNS_PB gửi: chức vụ + nghiệp vụ chi tiết theo từng đơn vị; bỏ trống = giữ nguyên. */
  objectAssignments?: AuditKhnsObjectAssignment[];
}

const BASE = "/api/audit/plan/khns-nam";

/** allocatedOnly=true: chỉ cán bộ đã được phân bổ đi kiểm toán (dùng cho KHNS_PB).
 * listedOnly=true: chỉ cán bộ đã được đưa vào danh sách KHNS_NAM qua nút "Cập nhật danh sách cán bộ". */
export async function listAuditKhnsNam(year: number, allocatedOnly = false, listedOnly = false): Promise<AuditKhnsNamRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhnsNamRowItem[]>>(BASE, { params: { year, allocatedOnly, listedOnly } });
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

/** Nút "Cập nhật danh sách cán bộ" ở KHNS_NAM - lấy danh sách cán bộ đã phân bổ ở KHNS_PB; trả về số cán bộ trong danh sách. */
export async function syncAuditKhnsNamList(year: number): Promise<number> {
  const res = await httpClient.post<ApiResponse<number>>(`${BASE}/sync-list`, null, { params: { year } });
  return res.data.data;
}

export interface AuditKhnsNamInfoRequest {
  otherDuties: string | null;
  decisionNumber: string | null;
  decisionDate: string | null;
  expectedBatch: string | null;
  note: string | null;
}

/** Sửa các trường nhập tay của KHNS_NAM (không đụng tới phân bổ tháng/chức vụ lấy từ KHNS_PB). */
export async function updateAuditKhnsNamInfo(employeeId: string, year: number, request: AuditKhnsNamInfoRequest): Promise<AuditKhnsNamRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhnsNamRowItem>>(`${BASE}/${employeeId}/info`, request, { params: { year } });
  return res.data.data;
}

export async function updateAuditKhnsNamNote(employeeId: string, year: number, note: string | null): Promise<AuditKhnsNamRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhnsNamRowItem>>(`${BASE}/${employeeId}/note`, { note }, { params: { year } });
  return res.data.data;
}

/** Thông tin NSD nhập khi xuất báo cáo theo đợt (file mẫu ZTC_BC_DOT) - đều tuỳ chọn, để trống thì để trống trong file. */
export interface AuditKhnsNamBatchReportRequest {
  decisionNumber: string | null;
  /** yyyy-MM-dd */
  decisionDate: string | null;
  unitPeriods: { auditObjectCode: string; period: string }[];
}

export async function exportAuditKhnsNamMonthlyReport(year: number, month: number, request: AuditKhnsNamBatchReportRequest): Promise<void> {
  const res = await httpClient.post(`${BASE}/export`, request, { params: { year, month }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = `bao_cao_khns_thang_${month}_${year}.xlsx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export type AuditKhnsNamDecisionType = "TEAM" | "INVENTORY";

/** "Xuất QĐ thành lập đoàn" / "Xuất QĐ kiểm kê" (file Word mẫu của BKS) cho 1 chi nhánh đi kiểm toán trong tháng (đợt). */
export async function exportAuditKhnsNamDecision(
  year: number,
  month: number,
  auditObjectCode: string,
  type: AuditKhnsNamDecisionType,
  branchName: string,
): Promise<void> {
  const res = await httpClient.get(`${BASE}/export-decision`, { params: { year, month, auditObjectCode, type }, responseType: "blob" });
  // tên file tự đặt ở đây: header Content-Disposition không được CORS cho phép đọc từ trình duyệt
  const baseName = type === "TEAM" ? "QD thanh lap doan" : "QD kiem ke";
  const fileName = `${baseName} - ${branchName.replace(/[\\/:*?"<>|]/g, " ").trim()} - T${month}-${year}.docx`;
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

/** 1 dòng màn KHNS_PB = 1 cán bộ được phân bổ vào 1 đơn vị (months = các tháng cán bộ đi kiểm toán đơn vị đó). */
export interface AuditKhnsPbRowItem {
  employeeId: string;
  auditObjectCode: string;
  auditObjectName: string;
  businessSegmentCodes: string[];
  creditScale: number | null;
  fundingScale: number | null;
  employeeCode: string;
  employeeName: string;
  username: string | null;
  roleInTeam: AuditKhnsRoleInTeam | null;
  positions: AuditKhnsPosition[];
  /** Nghiệp vụ cán bộ làm tại đơn vị này (tối đa 3, không trộn Tín dụng với NTD). */
  segmentCodes: string[];
  segmentNames: string[];
  months: number[];
}

export async function listAuditKhnsPbRows(year: number): Promise<AuditKhnsPbRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhnsPbRowItem[]>>(`${BASE}/allocation`, { params: { year } });
  return res.data.data;
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
