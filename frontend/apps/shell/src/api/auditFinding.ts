import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

const BASE = "/api/audit/findings";

export interface AuditFindingItem {
  id: string;
  branchCode: string;
  branchName: string | null;
  title: string;
  description: string | null;
  severity: string;
  severityName: string | null;
  detectedDate: string;
  active: boolean;
}

export interface AuditFindingRequest {
  branchCode: string;
  title: string;
  description?: string | null;
  severity: string;
  detectedDate: string;
  active: boolean;
}

export const auditFindingApi = {
  async list(): Promise<AuditFindingItem[]> {
    const res = await httpClient.get<ApiResponse<AuditFindingItem[]>>(BASE);
    return res.data.data;
  },
  async create(request: AuditFindingRequest): Promise<AuditFindingItem> {
    const res = await httpClient.post<ApiResponse<AuditFindingItem>>(BASE, request);
    return res.data.data;
  },
  async update(id: string, request: AuditFindingRequest): Promise<AuditFindingItem> {
    const res = await httpClient.put<ApiResponse<AuditFindingItem>>(`${BASE}/${id}`, request);
    return res.data.data;
  },
  async remove(id: string): Promise<void> {
    await httpClient.delete(`${BASE}/${id}`);
  },
};
