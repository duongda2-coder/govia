import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditProcessStepSummaryItem {
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

export interface AuditProcessStepSummaryRequest {
  businessSegmentId: string | null;
  code: string;
  name: string;
  workItemId: string | null;
  active: boolean;
}

export interface AuditProcessStepDetailItem {
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

export interface AuditProcessStepDetailRequest {
  businessSegmentId: string | null;
  processStepSummaryId: string | null;
  controlPointId: string | null;
  code: string;
  active: boolean;
}

const SUMMARY_BASE = "/api/audit/plan/master-data/process-step-summary";
const DETAIL_BASE = "/api/audit/plan/master-data/process-step-detail";

export async function listAuditProcessStepSummaries(): Promise<AuditProcessStepSummaryItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessStepSummaryItem[]>>(SUMMARY_BASE);
  return res.data.data;
}

export async function createAuditProcessStepSummary(request: AuditProcessStepSummaryRequest): Promise<AuditProcessStepSummaryItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessStepSummaryItem>>(SUMMARY_BASE, request);
  return res.data.data;
}

export async function updateAuditProcessStepSummary(id: string, request: AuditProcessStepSummaryRequest): Promise<AuditProcessStepSummaryItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessStepSummaryItem>>(`${SUMMARY_BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessStepSummary(id: string): Promise<void> {
  await httpClient.delete(`${SUMMARY_BASE}/${id}`);
}

export async function importAuditProcessStepSummaries(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${SUMMARY_BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditProcessStepSummaries(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${SUMMARY_BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_step_summary.xlsx" : "audit_process_step_summary.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export async function listAuditProcessStepDetails(): Promise<AuditProcessStepDetailItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessStepDetailItem[]>>(DETAIL_BASE);
  return res.data.data;
}

export async function createAuditProcessStepDetail(request: AuditProcessStepDetailRequest): Promise<AuditProcessStepDetailItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessStepDetailItem>>(DETAIL_BASE, request);
  return res.data.data;
}

export async function updateAuditProcessStepDetail(id: string, request: AuditProcessStepDetailRequest): Promise<AuditProcessStepDetailItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessStepDetailItem>>(`${DETAIL_BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessStepDetail(id: string): Promise<void> {
  await httpClient.delete(`${DETAIL_BASE}/${id}`);
}

export async function importAuditProcessStepDetails(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${DETAIL_BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditProcessStepDetails(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${DETAIL_BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_step_detail.xlsx" : "audit_process_step_detail.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
