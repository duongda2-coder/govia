import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditEngagementStatus = "DRAFT" | "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface AuditEngagementItem {
  id: string;
  code: string;
  auditObjectUnitId: string;
  auditObjectUnitCode: string | null;
  auditObjectUnitName: string | null;
  unitType: string | null;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  teamLeadEmployeeCode: string | null;
  teamLeadEmployeeName: string | null;
  decisionNumber: string;
  status: AuditEngagementStatus;
  riskRank: string | null;
  name: string | null;
  objective: string | null;
  scope: string | null;
  planningStartDate: string | null;
  planningEndDate: string | null;
  fieldworkStartDate: string | null;
  fieldworkEndDate: string | null;
  reportStartDate: string | null;
  reportEndDate: string | null;
  infoCollectionStart: string | null;
  infoCollectionEnd: string | null;
  sampleRequestStart: string | null;
  sampleRequestEnd: string | null;
  reportPlanStart: string | null;
  reportPlanEnd: string | null;
}

export interface AuditEngagementRequest {
  auditObjectUnitId: string;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  decisionNumber: string;
  status?: AuditEngagementStatus | null;
  riskRank?: string | null;
  name?: string | null;
  objective?: string | null;
  scope?: string | null;
  planningStartDate?: string | null;
  planningEndDate?: string | null;
  fieldworkStartDate?: string | null;
  fieldworkEndDate?: string | null;
  reportStartDate?: string | null;
  reportEndDate?: string | null;
  infoCollectionStart?: string | null;
  infoCollectionEnd?: string | null;
  sampleRequestStart?: string | null;
  sampleRequestEnd?: string | null;
  reportPlanStart?: string | null;
  reportPlanEnd?: string | null;
}

export interface AuditEngagementRelatedUnitItem {
  id: string;
  auditEngagementId: string;
  engagementCode: string;
  auditObjectUnitId: string;
  auditObjectUnitCode: string | null;
  auditObjectUnitName: string | null;
  unitType: string | null;
}

export interface AuditObjectUnitOption {
  id: string;
  code: string;
  name: string;
  unitType: string;
}

export interface EmployeeOption {
  id: string;
  employeeCode: string;
  fullName: string;
  username: string | null;
  truongDoanCapable: boolean;
  truongNhomCapable: boolean;
  capableSegmentCodes: string[];
}

const BASE = "/api/audit/plan/engagement";

export async function listAuditEngagements(): Promise<AuditEngagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementItem[]>>(BASE);
  return res.data.data;
}

export async function getAuditEngagement(id: string): Promise<AuditEngagementItem> {
  const res = await httpClient.get<ApiResponse<AuditEngagementItem>>(`${BASE}/${id}`);
  return res.data.data;
}

export async function createAuditEngagement(request: AuditEngagementRequest): Promise<AuditEngagementItem> {
  const res = await httpClient.post<ApiResponse<AuditEngagementItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditEngagement(id: string, request: AuditEngagementRequest): Promise<AuditEngagementItem> {
  const res = await httpClient.put<ApiResponse<AuditEngagementItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditEngagement(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function listAuditObjectUnitOptions(): Promise<AuditObjectUnitOption[]> {
  const res = await httpClient.get<ApiResponse<AuditObjectUnitOption[]>>(`${BASE}/lookups/audit-object-units`);
  return res.data.data;
}

export async function listEmployeeOptions(): Promise<EmployeeOption[]> {
  const res = await httpClient.get<ApiResponse<EmployeeOption[]>>(`${BASE}/lookups/employees`);
  return res.data.data;
}

export async function listRelatedUnits(engagementId: string): Promise<AuditEngagementRelatedUnitItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementRelatedUnitItem[]>>(`${BASE}/${engagementId}/related-units`);
  return res.data.data;
}

export async function addRelatedUnit(engagementId: string, auditObjectUnitId: string): Promise<AuditEngagementRelatedUnitItem> {
  const res = await httpClient.post<ApiResponse<AuditEngagementRelatedUnitItem>>(`${BASE}/${engagementId}/related-units`, { auditObjectUnitId });
  return res.data.data;
}

export async function deleteRelatedUnit(engagementId: string, relatedUnitId: string): Promise<void> {
  await httpClient.delete(`${BASE}/${engagementId}/related-units/${relatedUnitId}`);
}

export async function importAuditEngagements(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditEngagements(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_engagement.xlsx" : "audit_engagement.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
