import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmTd1Item {
  id: string;
  branchCode: string;
  auditDate: string;
  customerCode: string | null;
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
  branchCode: string;
  auditDate: string;
  customerCode: string | null;
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

export async function listAuditCmTd1(): Promise<AuditCmTd1Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmTd1Item[]>>(BASE);
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

export async function importAuditCmTd1(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmTd1(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_td1.xlsx" : "audit_cm_td1.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
