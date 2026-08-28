import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditControlType = "MANUAL" | "AUTOMATIC";
export type AuditLevel = "HIGH" | "MEDIUM" | "LOW";

export interface AuditControlPointItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  code: string;
  name: string;
  possibleRisk: string | null;
  controlPointByStep: string | null;
  actualControl: string | null;
  controlType: AuditControlType | null;
  controlFrequency: AuditLevel | null;
  auditProcedure: string | null;
  residualRiskAssessment: string | null;
  processRegulation: string | null;
  referenceClause: string | null;
  processEffectiveness: string | null;
  controlEffectivenessAssessment: string | null;
  controlEfficiencyAssessment: string | null;
  active: boolean;
}

export interface AuditControlPointRequest {
  businessSegmentId: string | null;
  code: string;
  name: string;
  possibleRisk: string | null;
  controlPointByStep: string | null;
  actualControl: string | null;
  controlType: AuditControlType | null;
  controlFrequency: AuditLevel | null;
  auditProcedure: string | null;
  residualRiskAssessment: string | null;
  processRegulation: string | null;
  referenceClause: string | null;
  processEffectiveness: string | null;
  controlEffectivenessAssessment: string | null;
  controlEfficiencyAssessment: string | null;
  active: boolean;
}

const BASE = "/api/audit/master-data/control-point";

export async function listAuditControlPoints(): Promise<AuditControlPointItem[]> {
  const res = await httpClient.get<ApiResponse<AuditControlPointItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditControlPoint(request: AuditControlPointRequest): Promise<AuditControlPointItem> {
  const res = await httpClient.post<ApiResponse<AuditControlPointItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditControlPoint(id: string, request: AuditControlPointRequest): Promise<AuditControlPointItem> {
  const res = await httpClient.put<ApiResponse<AuditControlPointItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditControlPoint(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditControlPoints(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditControlPoints(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_control_point.xlsx" : "audit_control_point.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
