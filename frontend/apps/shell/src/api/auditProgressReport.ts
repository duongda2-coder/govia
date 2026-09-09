import axios from "axios";
import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AssignmentApprovalStatus } from "./auditWorkManagement";

export interface AuditProgressReportItem {
  id: string;
  engagementId: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  reportedEmployeeId: string | null;
  reportedEmployeeCode: string | null;
  reportedEmployeeName: string | null;
  totalFindings: number;
  totalTtss: number;
  totalMaterialFindings: number;
  totalMaterialTtss: number;
  totalSamples: number;
  completedSamples: number;
  reportDate: string;
  reportRound: number;
  reportedByUsername: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
  approvedBy: string | null;
  approvedAt: string | null;
}

export interface AuditProgressReportAttachment {
  id: string;
  fileName: string;
}

function base(engagementId: string): string {
  return `/api/audit/plan/engagement/${engagementId}/work-management/progress-reports`;
}

export async function listAuditProgressReports(engagementId: string): Promise<AuditProgressReportItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProgressReportItem[]>>(base(engagementId));
  return res.data.data;
}

export async function approveAuditProgressReports(engagementId: string, reportIds: string[]): Promise<string[]> {
  const res = await httpClient.post<ApiResponse<string[]>>(`${base(engagementId)}/approve`, { reportIds });
  return res.data.data;
}

/** File TTSS goc dinh kem tu dong luc sinh dong bao cao (xem AuditProgressReportService.recordUpload
 * - luu qua AttachmentService dung chung, entityName="AUDIT_PROGRESS_REPORT", entityId=report.id). */
export async function listAuditProgressReportAttachments(reportId: string): Promise<AuditProgressReportAttachment[]> {
  const res = await httpClient.get<ApiResponse<AuditProgressReportAttachment[]>>("/api/attachments", {
    params: { entityName: "AUDIT_PROGRESS_REPORT", entityId: reportId },
  });
  return res.data.data;
}

export async function downloadAuditProgressReportAttachment(attachmentId: string, fileName: string): Promise<void> {
  try {
    const res = await httpClient.get(`/api/attachments/${attachmentId}/download`, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(res.data as Blob);
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(blobUrl);
  } catch (err) {
    // responseType "blob" ap dung ca cho response LOI - err.response.data la 1 Blob (chua JSON dang
    // text) thay vi object da parse, nen getApiErrorMessage() luon fallback ve thong bao chung chung
    // du backend tra ve message cu the (vd "File dinh kem khong con tren dia..."). Doc lai thanh JSON
    // truoc khi nem tiep de UI hien dung ly do that su.
    if (axios.isAxiosError(err) && err.response?.data instanceof Blob) {
      try {
        err.response.data = JSON.parse(await err.response.data.text());
      } catch {
        // khong parse duoc (vd loi mang tra ve HTML) - giu nguyen blob, getApiErrorMessage fallback binh thuong
      }
    }
    throw err;
  }
}
