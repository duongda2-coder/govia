import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface GroupHOItem {
  id: string;
  code: string;
  name: string;
  note: string | null;
  active: boolean;
}
export interface GroupHORequest {
  code: string;
  name: string;
  note?: string | null;
  active: boolean;
}

/**
 * Factory dung chung cho sub-module "Cham Diem" - cung mo hinh voi factory cua Master Data CDRR
 * (xem api/riskScoring.ts) nhung namespace rieng /scoring vi day la sub-module khac, phan quyen doc lap.
 */
function createResourceApi<TItem, TRequest>(basePath: string, fileBaseName: string) {
  return {
    async list(): Promise<TItem[]> {
      const res = await httpClient.get<ApiResponse<TItem[]>>(basePath);
      return res.data.data;
    },
    async create(request: TRequest): Promise<TItem> {
      const res = await httpClient.post<ApiResponse<TItem>>(basePath, request);
      return res.data.data;
    },
    async update(id: string, request: TRequest): Promise<TItem> {
      const res = await httpClient.put<ApiResponse<TItem>>(`${basePath}/${id}`, request);
      return res.data.data;
    },
    async remove(id: string): Promise<void> {
      await httpClient.delete(`${basePath}/${id}`);
    },
    async importExcel(file: File): Promise<ImportResult> {
      const formData = new FormData();
      formData.append("file", file);
      const res = await httpClient.post<ApiResponse<ImportResult>>(`${basePath}/import`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return res.data.data;
    },
    /** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
    async exportFile(kind: "excel" | "word"): Promise<void> {
      const res = await httpClient.get(`${basePath}/export/${kind}`, { responseType: "blob" });
      const blobUrl = window.URL.createObjectURL(res.data as Blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = kind === "excel" ? `${fileBaseName}.xlsx` : `${fileBaseName}.docx`;
      link.click();
      window.URL.revokeObjectURL(blobUrl);
    },
  };
}

const BASE = "/api/audit/risk-scoring/scoring";

export const groupHOApi = createResourceApi<GroupHOItem, GroupHORequest>(`${BASE}/group-ho`, "risk_score_group_ho");
