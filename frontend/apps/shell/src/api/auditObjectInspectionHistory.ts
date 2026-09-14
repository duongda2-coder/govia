import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditInspectionType = "TTCP" | "KTNN" | "TTGSNH" | "KTNB" | "GSBKS" | "KTGSNB";

export interface AuditObjectInspectionHistoryItem {
  id: string;
  auditObjectUnitId: string;
  inspectionType: AuditInspectionType;
  year: number;
  note: string | null;
}

export interface AuditObjectInspectionHistoryRequest {
  auditObjectUnitId: string;
  inspectionType: AuditInspectionType;
  year: number;
  note: string | null;
}

const BASE = "/api/audit/risk-scoring/master-data/audit-object-inspection-history";

export async function listAuditObjectInspectionHistory(auditObjectUnitId: string): Promise<AuditObjectInspectionHistoryItem[]> {
  const res = await httpClient.get<ApiResponse<AuditObjectInspectionHistoryItem[]>>(BASE, { params: { auditObjectUnitId } });
  return res.data.data;
}

export async function createAuditObjectInspectionHistory(
  request: AuditObjectInspectionHistoryRequest,
): Promise<AuditObjectInspectionHistoryItem> {
  const res = await httpClient.post<ApiResponse<AuditObjectInspectionHistoryItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditObjectInspectionHistory(
  id: string,
  request: AuditObjectInspectionHistoryRequest,
): Promise<AuditObjectInspectionHistoryItem> {
  const res = await httpClient.put<ApiResponse<AuditObjectInspectionHistoryItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditObjectInspectionHistory(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}
