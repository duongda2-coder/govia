import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditKhktScaleItem {
  id: string;
  sortOrder: number;
  creditThreshold: number | null;
  fundingThreshold: number | null;
  scaleName: string;
  active: boolean;
}

export interface AuditKhktScaleRequest {
  sortOrder: number;
  creditThreshold: number | null;
  fundingThreshold: number | null;
  scaleName: string;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data/khkt-scale";

export async function listAuditKhktScales(): Promise<AuditKhktScaleItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktScaleItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditKhktScale(request: AuditKhktScaleRequest): Promise<AuditKhktScaleItem> {
  const res = await httpClient.post<ApiResponse<AuditKhktScaleItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditKhktScale(id: string, request: AuditKhktScaleRequest): Promise<AuditKhktScaleItem> {
  const res = await httpClient.put<ApiResponse<AuditKhktScaleItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditKhktScale(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditKhktScales(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditKhktScales(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_khkt_scale.xlsx" : "audit_khkt_scale.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
