import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditProcessEngagementItem {
  id: string;
  code: string;
  businessSegmentId: string;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  teamLeadEmployeeCode: string | null;
  teamLeadEmployeeName: string | null;
  decisionNumber: string;
  name: string | null;
}

export interface AuditProcessEngagementRequest {
  businessSegmentId: string;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  decisionNumber: string;
  name?: string | null;
}

export interface TeamLeadOption {
  id: string;
  employeeCode: string;
  fullName: string;
  truongDoanCapable: boolean;
}

const BASE = "/api/audit/plan/engagement-process";

export async function listAuditProcessEngagements(): Promise<AuditProcessEngagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessEngagementItem[]>>(BASE);
  return res.data.data;
}

export async function getAuditProcessEngagement(id: string): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.get<ApiResponse<AuditProcessEngagementItem>>(`${BASE}/${id}`);
  return res.data.data;
}

export async function createAuditProcessEngagement(request: AuditProcessEngagementRequest): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessEngagementItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditProcessEngagement(id: string, request: AuditProcessEngagementRequest): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessEngagementItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessEngagement(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function listTeamLeadOptions(): Promise<TeamLeadOption[]> {
  const res = await httpClient.get<ApiResponse<TeamLeadOption[]>>(`${BASE}/lookups/team-leads`);
  return res.data.data;
}

export async function importAuditProcessEngagements(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditProcessEngagements(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_engagement.xlsx" : "audit_process_engagement.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
