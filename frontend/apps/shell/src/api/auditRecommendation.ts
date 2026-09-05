import type { ApiResponse } from "@govia/ui-kit";
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
