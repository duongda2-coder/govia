import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditWorkPhase } from "./auditWorkItem";

export interface AuditWorkItemQtItem {
  id: string;
  auditObjectCategoryId: string | null;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  phase: AuditWorkPhase | null;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  code: string;
  name: string;
  applicableYear: number | null;
  workSetCode: string | null;
  workType: string | null;
  active: boolean;
  hasSampleSelection: boolean;
  branchOrHeadOffice: string | null;
}

export interface AuditWorkItemQtRequest {
  auditObjectCategoryId: string | null;
  phase: AuditWorkPhase | null;
  businessSegmentId: string | null;
  code: string;
  name: string;
  applicableYear: number | null;
  workSetCode: string | null;
  workType: string | null;
  active: boolean;
  hasSampleSelection: boolean;
  branchOrHeadOffice: string | null;
}

const BASE = "/api/audit/plan/master-data-qt/work-item";

export async function listAuditWorkItemsQt(): Promise<AuditWorkItemQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditWorkItemQtItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditWorkItemQt(request: AuditWorkItemQtRequest): Promise<AuditWorkItemQtItem> {
  const res = await httpClient.post<ApiResponse<AuditWorkItemQtItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditWorkItemQt(id: string, request: AuditWorkItemQtRequest): Promise<AuditWorkItemQtItem> {
  const res = await httpClient.put<ApiResponse<AuditWorkItemQtItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditWorkItemQt(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditWorkItemsQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditWorkItemsQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_work_item_qt.xlsx" : "audit_work_item_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
