import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditActionType = "CREATE" | "UPDATE" | "DELETE" | "LOGIN" | "LOGOUT" | "EXPORT" | "APPROVE" | "REJECT";

export interface ActivityLogItem {
  id: string;
  entityName: string;
  entityId: string | null;
  action: AuditActionType;
  detail: string | null;
  performedBy: string | null;
  performedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ActivityLogListParams {
  entityName?: string;
  action?: AuditActionType;
  performedBy?: string;
  entityId?: string;
  dateFrom?: string;
  dateTo?: string;
  keyword?: string;
  page?: number;
  size?: number;
  /** Dinh dang Spring Pageable: "field,asc" hoac "field,desc" (vd "createdAt,desc"). */
  sort?: string;
}

const BASE = "/api/admin/activity-log";

export async function listActivityLogs(params: ActivityLogListParams): Promise<PageResponse<ActivityLogItem>> {
  const res = await httpClient.get<ApiResponse<PageResponse<ActivityLogItem>>>(BASE, { params });
  return res.data.data;
}

export async function exportActivityLogs(kind: "excel" | "word", params: Omit<ActivityLogListParams, "page" | "size" | "sort">): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "activity_log.xlsx" : "activity_log.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
