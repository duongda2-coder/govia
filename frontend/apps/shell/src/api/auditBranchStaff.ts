import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditBranchStaffItem {
  id: string;
  branchCode: string;
  staffName: string;
  position: string | null;
  priority: number | null;
  note: string | null;
  active: boolean;
}

export interface AuditBranchStaffRequest {
  branchCode: string;
  staffName: string;
  position: string | null;
  priority: number | null;
  note: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data/branch-staff";

export async function listAuditBranchStaff(): Promise<AuditBranchStaffItem[]> {
  const res = await httpClient.get<ApiResponse<AuditBranchStaffItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditBranchStaff(request: AuditBranchStaffRequest): Promise<AuditBranchStaffItem> {
  const res = await httpClient.post<ApiResponse<AuditBranchStaffItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditBranchStaff(id: string, request: AuditBranchStaffRequest): Promise<AuditBranchStaffItem> {
  const res = await httpClient.put<ApiResponse<AuditBranchStaffItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditBranchStaff(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditBranchStaff(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditBranchStaff(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_branch_staff.xlsx" : "audit_branch_staff.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
