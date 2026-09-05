import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AssignmentApprovalStatus } from "./auditWorkManagement";

export interface AuditTtssRecordItem {
  id: string;
  engagementId: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  recordUsername: string | null;
  workItemCode: string | null;
  processStepSummaryId: string | null;
  processStepSummaryCode: string | null;
  processStepSummaryName: string | null;
  processStepDetailId: string | null;
  processStepDetailCode: string | null;
  ttssContent: string | null;
  findingCode: string | null;
  findingName: string | null;
  material: boolean;
  referenceNumber: string | null;
  referenceNumber2: string | null;
  customerCode: string | null;
  customerName: string | null;
  amount: number | null;
  performingUser: string | null;
  transactionContent: string | null;
  exceptionDate: string | null;
  approverName: string | null;
  controllerName: string | null;
  ttssPerformerName: string | null;
  relatedStaff: string | null;
  uploaderRecommendationCode: string | null;
  uploaderRecommendationName: string | null;
  teamRecommendationId: string | null;
  teamRecommendationCode: string | null;
  teamRecommendationContent: string | null;
  recommendationApprovalStatus: AssignmentApprovalStatus | null;
  recommendationApprovedBy: string | null;
  recommendationApprovedAt: string | null;
}

function base(engagementId: string): string {
  return `/api/audit/plan/engagement/${engagementId}/ttss`;
}

export async function listAuditTtssRecords(engagementId: string): Promise<AuditTtssRecordItem[]> {
  const res = await httpClient.get<ApiResponse<AuditTtssRecordItem[]>>(base(engagementId));
  return res.data.data;
}

export async function downloadAuditTtssTemplate(engagementId: string): Promise<void> {
  const res = await httpClient.get(`${base(engagementId)}/template`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = "mau_upload_ttss.xlsx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

/** Khop voi StandardToolbar.onImport (tra ve ImportResult) - TTSS khong co loi tung dong (moi dong
 * upload deu duoc tao), nen luon successCount = so dong, failureCount = 0. */
export async function uploadAuditTtssFile(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<AuditTtssRecordItem[]>>(`${base(engagementId)}/upload`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return { successCount: res.data.data.length, failureCount: 0, errors: [] };
}

export async function linkAuditTtssRecommendation(engagementId: string, ttssRecordIds: string[], recommendationId: string): Promise<void> {
  await httpClient.post(`${base(engagementId)}/link-recommendation`, { ttssRecordIds, recommendationId });
}

export async function approveAuditTtssRecommendations(engagementId: string, ttssRecordIds: string[]): Promise<string[]> {
  const res = await httpClient.post<ApiResponse<string[]>>(`${base(engagementId)}/approve-recommendations`, { ttssRecordIds });
  return res.data.data;
}
