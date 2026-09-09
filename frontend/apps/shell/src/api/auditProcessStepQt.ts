import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditProcessStepSummaryQtItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  code: string;
  name: string;
  workItemId: string | null;
  workItemCode: string | null;
  workItemName: string | null;
  active: boolean;
}

export interface AuditProcessStepSummaryQtRequest {
  businessSegmentId: string | null;
  code: string;
  name: string;
  workItemId: string | null;
  active: boolean;
}

const SUMMARY_BASE = "/api/audit/plan/master-data-qt/process-step-summary";

export async function listAuditProcessStepSummariesQt(): Promise<AuditProcessStepSummaryQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessStepSummaryQtItem[]>>(SUMMARY_BASE);
  return res.data.data;
}

export async function createAuditProcessStepSummaryQt(request: AuditProcessStepSummaryQtRequest): Promise<AuditProcessStepSummaryQtItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessStepSummaryQtItem>>(SUMMARY_BASE, request);
  return res.data.data;
}

export async function updateAuditProcessStepSummaryQt(id: string, request: AuditProcessStepSummaryQtRequest): Promise<AuditProcessStepSummaryQtItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessStepSummaryQtItem>>(`${SUMMARY_BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessStepSummaryQt(id: string): Promise<void> {
  await httpClient.delete(`${SUMMARY_BASE}/${id}`);
}

export async function importAuditProcessStepSummariesQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${SUMMARY_BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditProcessStepSummariesQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${SUMMARY_BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_step_summary_qt.xlsx" : "audit_process_step_summary_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export interface AuditProcessStepDetailQtItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  processStepSummaryId: string | null;
  processStepSummaryCode: string | null;
  processStepSummaryName: string | null;
  controlPointId: string | null;
  controlPointCode: string | null;
  controlPointName: string | null;
  code: string;
  active: boolean;
}

export interface AuditProcessStepDetailQtRequest {
  businessSegmentId: string | null;
  processStepSummaryId: string | null;
  controlPointId: string | null;
  code: string;
  active: boolean;
}

const DETAIL_BASE = "/api/audit/plan/master-data-qt/process-step-detail";

export async function listAuditProcessStepDetailsQt(): Promise<AuditProcessStepDetailQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessStepDetailQtItem[]>>(DETAIL_BASE);
  return res.data.data;
}

export async function createAuditProcessStepDetailQt(request: AuditProcessStepDetailQtRequest): Promise<AuditProcessStepDetailQtItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessStepDetailQtItem>>(DETAIL_BASE, request);
  return res.data.data;
}

export async function updateAuditProcessStepDetailQt(id: string, request: AuditProcessStepDetailQtRequest): Promise<AuditProcessStepDetailQtItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessStepDetailQtItem>>(`${DETAIL_BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessStepDetailQt(id: string): Promise<void> {
  await httpClient.delete(`${DETAIL_BASE}/${id}`);
}

export async function importAuditProcessStepDetailsQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${DETAIL_BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditProcessStepDetailsQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${DETAIL_BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_step_detail_qt.xlsx" : "audit_process_step_detail_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
