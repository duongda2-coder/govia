import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditEmployeeCapabilityItem {
  employeeId: string;
  employeeCode: string;
  username: string | null;
  fullName: string;
  theCapable: boolean;
  qtdhCapable: boolean;
  hdvCapable: boolean;
  tcktCapable: boolean;
  cnttCapable: boolean;
  ttkqCapable: boolean;
  pcrtCapable: boolean;
  ttqtCapable: boolean;
  xdcbCapable: boolean;
  tdCapable: boolean;
  truongDoanCapable: boolean;
  truongNhomCapable: boolean;
  toGiamSatCapable: boolean;
  dgclCapable: boolean;
  enteredBy: string | null;
  updatedAt: string | null;
  approved: boolean;
  approvedBy: string | null;
  approvedAt: string | null;
}

export type CapabilityFlagKey =
  | "theCapable"
  | "qtdhCapable"
  | "hdvCapable"
  | "tcktCapable"
  | "cnttCapable"
  | "ttkqCapable"
  | "pcrtCapable"
  | "ttqtCapable"
  | "xdcbCapable"
  | "tdCapable"
  | "truongDoanCapable"
  | "truongNhomCapable"
  | "toGiamSatCapable"
  | "dgclCapable";

export interface AuditEmployeeCapabilityItemRequest {
  employeeId: string;
  theCapable: boolean;
  qtdhCapable: boolean;
  hdvCapable: boolean;
  tcktCapable: boolean;
  cnttCapable: boolean;
  ttkqCapable: boolean;
  pcrtCapable: boolean;
  ttqtCapable: boolean;
  xdcbCapable: boolean;
  tdCapable: boolean;
  truongDoanCapable: boolean;
  truongNhomCapable: boolean;
  toGiamSatCapable: boolean;
  dgclCapable: boolean;
}

const BASE = "/api/audit/master-data/employee-capability";

export async function listAuditEmployeeCapabilities(): Promise<AuditEmployeeCapabilityItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEmployeeCapabilityItem[]>>(BASE);
  return res.data.data;
}

export async function bulkUpdateAuditEmployeeCapabilities(
  items: AuditEmployeeCapabilityItemRequest[],
): Promise<AuditEmployeeCapabilityItem[]> {
  const res = await httpClient.put<ApiResponse<AuditEmployeeCapabilityItem[]>>(`${BASE}/bulk`, items);
  return res.data.data;
}

export async function approveAuditEmployeeCapability(employeeId: string): Promise<AuditEmployeeCapabilityItem> {
  const res = await httpClient.post<ApiResponse<AuditEmployeeCapabilityItem>>(`${BASE}/${employeeId}/approve`);
  return res.data.data;
}

export async function importAuditEmployeeCapabilities(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditEmployeeCapabilities(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_employee_capability.xlsx" : "audit_employee_capability.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
