import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmTd1Item {
  id: string;
  engagementId: string | null;
  engagementCode: string | null;
  assignedEmployeeId: string | null;
  assignedEmployeeCode: string | null;
  assignedUsername: string | null;
  processStepSummaryId: string | null;
  processStepSummaryCode: string | null;
  processStepSummaryName: string | null;
  branchCode: string;
  auditDate: string;
  customerCode: string | null;
  sampleFilterUser: string | null;
  customerName: string;
  approvedAmount: number | null;
  loanPurpose: string | null;
  description: string | null;
  onBalanceDebt: number | null;
  guaranteeBalance: number | null;
  riskClassifiedDebt: number | null;
  vamcSoldDebt: number | null;
  totalCreditBalance: number | null;
  debtGroup: string | null;
  auditScope: string | null;
  auditorCode: string | null;
  sampleReason: string | null;
  note: string | null;
  active: boolean;
}

export interface AuditCmTd1Request {
  engagementId: string;
  assignedEmployeeId: string | null;
  processStepSummaryId: string | null;
  branchCode: string;
  auditDate: string;
  customerCode: string | null;
  sampleFilterUser: string | null;
  customerName: string;
  approvedAmount: number | null;
  loanPurpose: string | null;
  description: string | null;
  onBalanceDebt: number | null;
  guaranteeBalance: number | null;
  riskClassifiedDebt: number | null;
  vamcSoldDebt: number | null;
  debtGroup: string | null;
  auditScope: string | null;
  auditorCode: string | null;
  sampleReason: string | null;
  note: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-td1";

export async function listAuditCmTd1(engagementId: string): Promise<AuditCmTd1Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmTd1Item[]>>(BASE, { params: { engagementId } });
  return res.data.data;
}

export async function createAuditCmTd1(request: AuditCmTd1Request): Promise<AuditCmTd1Item> {
  const res = await httpClient.post<ApiResponse<AuditCmTd1Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmTd1(id: string, request: AuditCmTd1Request): Promise<AuditCmTd1Item> {
  const res = await httpClient.put<ApiResponse<AuditCmTd1Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmTd1(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmTd1(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    params: { engagementId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmTd1(kind: "excel" | "word", engagementId: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params: { engagementId }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_td1.xlsx" : "audit_cm_td1.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
