import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditWorkPhase = "CBKT" | "THKT" | "DCKT";
export type AssignmentStatus = "NOT_STARTED" | "IN_PROGRESS" | "DONE";
export type AssignmentApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface AuditWorkManagementItem {
  assignmentId: string;
  engagementId: string;
  engagementCode: string | null;
  engagementName: string | null;
  businessSegmentCode: string | null;
  workItemId: string;
  phase: AuditWorkPhase | null;
  workItemCode: string | null;
  workItemName: string | null;
  employeeId: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  employeeUsername: string | null;
  status: AssignmentStatus;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
  approvedBy: string | null;
  approvedAt: string | null;
}

export interface AuditWorkAssignmentStatusUpdateRequest {
  status: AssignmentStatus;
  note: string | null;
}

export interface AuditWorkReportFile {
  id: string;
  businessSegmentCode: string | null;
  uploadedAt: string;
  uploadedByUsername: string | null;
  uploadedByName: string | null;
  reportType: string | null;
  fileName: string;
}

function base(engagementId: string): string {
  return `/api/audit/plan/engagement/${engagementId}/work-management`;
}

export async function listAuditWorkManagement(
  engagementId: string,
  phase: AuditWorkPhase,
  employeeId?: string,
): Promise<AuditWorkManagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditWorkManagementItem[]>>(base(engagementId), {
    params: { phase, employeeId },
  });
  return res.data.data;
}

export async function updateAuditWorkAssignmentStatus(
  engagementId: string,
  assignmentId: string,
  request: AuditWorkAssignmentStatusUpdateRequest,
): Promise<AuditWorkManagementItem> {
  const res = await httpClient.put<ApiResponse<AuditWorkManagementItem>>(`${base(engagementId)}/${assignmentId}/status`, request);
  return res.data.data;
}

export async function approveAuditWorkAssignments(engagementId: string, assignmentIds: string[]): Promise<string[]> {
  const res = await httpClient.post<ApiResponse<string[]>>(`${base(engagementId)}/approve`, { assignmentIds });
  return res.data.data;
}

export async function listAuditWorkReportFiles(engagementId: string): Promise<AuditWorkReportFile[]> {
  const res = await httpClient.get<ApiResponse<AuditWorkReportFile[]>>(`${base(engagementId)}/report-files`);
  return res.data.data;
}

export async function uploadAuditWorkReportFile(engagementId: string, file: File): Promise<AuditWorkReportFile> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<AuditWorkReportFile>>(`${base(engagementId)}/report-files`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function deleteAuditWorkReportFile(engagementId: string, attachmentId: string): Promise<void> {
  await httpClient.delete(`${base(engagementId)}/report-files/${attachmentId}`);
}

export async function downloadAuditWorkReportFile(attachmentId: string, fileName: string): Promise<void> {
  const res = await httpClient.get(`/api/attachments/${attachmentId}/download`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
