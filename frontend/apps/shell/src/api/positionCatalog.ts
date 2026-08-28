import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { MasterDataItem, MasterDataItemRequest } from "./auditMasterData";

export type { MasterDataItem, MasterDataItemRequest };

const BASE = "/api/people/positions";

export async function listPositionCatalog(): Promise<MasterDataItem[]> {
  const res = await httpClient.get<ApiResponse<MasterDataItem[]>>(BASE);
  return res.data.data;
}

export async function createPositionCatalogItem(request: MasterDataItemRequest): Promise<MasterDataItem> {
  const res = await httpClient.post<ApiResponse<MasterDataItem>>(BASE, request);
  return res.data.data;
}

export async function updatePositionCatalogItem(id: string, request: MasterDataItemRequest): Promise<MasterDataItem> {
  const res = await httpClient.put<ApiResponse<MasterDataItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deletePositionCatalogItem(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importPositionCatalog(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportPositionCatalog(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "positions.xlsx" : "positions.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
