import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditControlType, AuditLevel } from "./auditControlPoint";

export interface AuditControlPointQtItem {
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

export interface AuditControlPointQtRequest {
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

const BASE = "/api/audit/plan/master-data-qt/control-point";

export async function listAuditControlPointsQt(): Promise<AuditControlPointQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditControlPointQtItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditControlPointQt(request: AuditControlPointQtRequest): Promise<AuditControlPointQtItem> {
  const res = await httpClient.post<ApiResponse<AuditControlPointQtItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditControlPointQt(id: string, request: AuditControlPointQtRequest): Promise<AuditControlPointQtItem> {
  const res = await httpClient.put<ApiResponse<AuditControlPointQtItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditControlPointQt(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditControlPointsQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditControlPointsQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_control_point_qt.xlsx" : "audit_control_point_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
