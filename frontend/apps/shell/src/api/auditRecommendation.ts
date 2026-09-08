import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditRecommendationItem {
  id: string;
  engagementId: string;
  code: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  content: string;
}

export interface AuditRecommendationRequest {
  businessSegmentId: string | null;
  content: string;
}

function base(engagementId: string): string {
  return `/api/audit/plan/engagement/${engagementId}/ttss/recommendations`;
}

export async function listAuditRecommendations(engagementId: string): Promise<AuditRecommendationItem[]> {
  const res = await httpClient.get<ApiResponse<AuditRecommendationItem[]>>(base(engagementId));
  return res.data.data;
}

export async function createAuditRecommendation(engagementId: string, request: AuditRecommendationRequest): Promise<AuditRecommendationItem> {
  const res = await httpClient.post<ApiResponse<AuditRecommendationItem>>(base(engagementId), request);
  return res.data.data;
}

export async function deleteAuditRecommendation(engagementId: string, recommendationId: string): Promise<void> {
  await httpClient.delete(`${base(engagementId)}/${recommendationId}`);
}

export async function downloadAuditRecommendationTemplate(engagementId: string): Promise<void> {
  const res = await httpClient.get(`${base(engagementId)}/template`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = "mau_kien_nghi.xlsx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

/** Khop voi StandardToolbar.onImport (tra ve ImportResult) - giong uploadAuditTtssFile, khong co
 * loi tung dong (dong thieu Ma/Noi dung bi bo qua am tham o backend), nen luon failureCount = 0. */
export async function uploadAuditRecommendationFile(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<AuditRecommendationItem[]>>(`${base(engagementId)}/upload`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return { successCount: res.data.data.length, failureCount: 0, errors: [] };
}
