import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditWorkManagementItem } from "./auditWorkManagement";
import type { AuditTtssRecordItem } from "./auditTtss";
import type { AuditRecommendationItem } from "./auditRecommendation";
import type { AuditEngagementMonitoringItem } from "./auditEngagementMonitoring";

/** "QT" (Quy trinh) hoac "HD" (Hoat dong) - xem sheet "QL CKT quy trinh" cua "Tao CKT (4).xlsx". */
export type AuditProcessEngagementObjectType = "QT" | "HD";

export interface AuditProcessEngagementItem {
  id: string;
  code: string;
  objectType: AuditProcessEngagementObjectType;
  businessSegmentId: string;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  teamLeadEmployeeCode: string | null;
  teamLeadEmployeeName: string | null;
  teamLeadUsername: string | null;
  decisionNumber: string;
  name: string | null;
  workSetCode: string | null;
  createdBy: string | null;
  /** "So CN kiem toan" - so CKT con da tao. */
  childCount: number;
  /** "So can bo" - tong so nhan vien (distinct) trong cac nhom cua tat ca CKT con. */
  memberCount: number;
}

export interface AuditProcessEngagementRequest {
  objectType: AuditProcessEngagementObjectType;
  businessSegmentId: string;
  year: number;
  expectedMonth: number;
  decisionDate: string;
  teamLeadEmployeeId: string;
  decisionNumber: string;
  name?: string | null;
  workSetCode?: string | null;
}

export interface TeamLeadOption {
  id: string;
  employeeCode: string;
  fullName: string;
  truongDoanCapable: boolean;
}

const BASE = "/api/audit/plan/engagement-process";

export async function listAuditProcessEngagements(): Promise<AuditProcessEngagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessEngagementItem[]>>(BASE);
  return res.data.data;
}

export async function getAuditProcessEngagement(id: string): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.get<ApiResponse<AuditProcessEngagementItem>>(`${BASE}/${id}`);
  return res.data.data;
}

export async function createAuditProcessEngagement(request: AuditProcessEngagementRequest): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.post<ApiResponse<AuditProcessEngagementItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditProcessEngagement(id: string, request: AuditProcessEngagementRequest): Promise<AuditProcessEngagementItem> {
  const res = await httpClient.put<ApiResponse<AuditProcessEngagementItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditProcessEngagement(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function listTeamLeadOptions(): Promise<TeamLeadOption[]> {
  const res = await httpClient.get<ApiResponse<TeamLeadOption[]>>(`${BASE}/lookups/team-leads`);
  return res.data.data;
}

export async function importAuditProcessEngagements(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditProcessEngagements(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_process_engagement.xlsx" : "audit_process_engagement.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

// ===================== "6. Chi tiết đoàn" / "3-5. Quản lý công việc/TTSS/KN" (doc, tong hop tat ca CKT con) =====================

export async function listProcessEngagementChildren(processEngagementId: string): Promise<AuditEngagementMonitoringItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementMonitoringItem[]>>(`${BASE}/${processEngagementId}/children`);
  return res.data.data;
}

export async function listProcessEngagementWorkManagement(processEngagementId: string): Promise<AuditWorkManagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditWorkManagementItem[]>>(`${BASE}/${processEngagementId}/work-management`);
  return res.data.data;
}

export async function listProcessEngagementTtss(processEngagementId: string): Promise<AuditTtssRecordItem[]> {
  const res = await httpClient.get<ApiResponse<AuditTtssRecordItem[]>>(`${BASE}/${processEngagementId}/ttss`);
  return res.data.data;
}

export async function listProcessEngagementRecommendations(processEngagementId: string): Promise<AuditRecommendationItem[]> {
  const res = await httpClient.get<ApiResponse<AuditRecommendationItem[]>>(`${BASE}/${processEngagementId}/recommendations`);
  return res.data.data;
}

// ===================== "2. File báo cáo khác" (rieng cap CKT quy trinh) =====================

export interface AuditProcessEngagementReportFile {
  id: string;
  businessSegmentCode: string | null;
  uploadedAt: string;
  uploadedByUsername: string | null;
  uploadedByName: string | null;
  reportType: string | null;
  fileName: string;
}

export async function listProcessEngagementReportFiles(processEngagementId: string): Promise<AuditProcessEngagementReportFile[]> {
  const res = await httpClient.get<ApiResponse<AuditProcessEngagementReportFile[]>>(`${BASE}/${processEngagementId}/report-files`);
  return res.data.data;
}

export async function uploadProcessEngagementReportFile(processEngagementId: string, file: File): Promise<AuditProcessEngagementReportFile> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<AuditProcessEngagementReportFile>>(`${BASE}/${processEngagementId}/report-files`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function deleteProcessEngagementReportFile(processEngagementId: string, attachmentId: string): Promise<void> {
  await httpClient.delete(`${BASE}/${processEngagementId}/report-files/${attachmentId}`);
}
