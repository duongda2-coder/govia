import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditKhktDtkhFileItem {
  id: string;
  year: number;
  departmentCode: string | null;
  versionCode: string | null;
  uploadedByUsername: string | null;
  uploadedByName: string | null;
  attachmentId: string | null;
  fileName: string | null;
  uploadedAt: string | null;
  note: string | null;
}

export interface AuditKhktDtkhFileUpdateRequest {
  departmentId: string;
  versionId: string | null;
  note: string | null;
}

const BASE = "/api/audit/plan/khkt-dtkh-file";

export async function listAuditKhktDtkhFiles(year: number): Promise<AuditKhktDtkhFileItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktDtkhFileItem[]>>(BASE, { params: { year } });
  return res.data.data;
}

export async function createAuditKhktDtkhFile(
  year: number,
  departmentId: string,
  versionId: string | null,
  note: string | null,
  file: File,
): Promise<AuditKhktDtkhFileItem> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<AuditKhktDtkhFileItem>>(BASE, formData, {
    params: { year, departmentId, versionId: versionId ?? undefined, note: note ?? undefined },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function updateAuditKhktDtkhFile(id: string, request: AuditKhktDtkhFileUpdateRequest): Promise<AuditKhktDtkhFileItem> {
  const res = await httpClient.put<ApiResponse<AuditKhktDtkhFileItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditKhktDtkhFile(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function downloadAuditKhktDtkhFile(attachmentId: string, fileName: string): Promise<void> {
  const res = await httpClient.get(`/api/attachments/${attachmentId}/download`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
