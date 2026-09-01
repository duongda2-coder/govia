import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd10Item {
  id: string;
  branchCode: string;
  issueDate: string;
  customerCode: string | null;
  customerName: string;
  accountNumber: string;
  cardTier: string | null;
  issuingUser: string | null;
  issuanceFee: number | null;
  issuanceType: string | null;
  issuanceOccurrence: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd10Request {
  branchCode: string;
  issueDate: string;
  customerCode: string | null;
  customerName: string;
  accountNumber: string;
  cardTier: string | null;
  issuingUser: string | null;
  issuanceFee: number | null;
  issuanceType: string | null;
  issuanceOccurrence: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd10";

export async function listAuditCmNtd10(): Promise<AuditCmNtd10Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd10Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd10(request: AuditCmNtd10Request): Promise<AuditCmNtd10Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd10Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd10(id: string, request: AuditCmNtd10Request): Promise<AuditCmNtd10Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd10Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd10(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd10(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd10(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd10.xlsx" : "audit_cm_ntd10.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
