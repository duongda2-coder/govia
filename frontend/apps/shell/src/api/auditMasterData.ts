import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface MasterDataCategoryInfo {
  code: string;
  label: string;
  group: string;
  groupLabel: string;
}

export interface MasterDataItem {
  id: string;
  category: string;
  code: string;
  name: string;
  description: string | null;
  parentId: string | null;
  validFrom: string | null;
  validTo: string | null;
  sortOrder: number | null;
  active: boolean;
}

export interface MasterDataItemRequest {
  code: string;
  name: string;
  description?: string | null;
  parentId?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  sortOrder?: number | null;
  active: boolean;
}

export async function listMasterDataCategories(): Promise<MasterDataCategoryInfo[]> {
  const res = await httpClient.get<ApiResponse<MasterDataCategoryInfo[]>>("/api/audit/master-data/categories");
  return res.data.data;
}

export async function listMasterDataItems(category: string): Promise<MasterDataItem[]> {
  const res = await httpClient.get<ApiResponse<MasterDataItem[]>>(`/api/audit/master-data/${category}`);
  return res.data.data;
}

export async function createMasterDataItem(category: string, request: MasterDataItemRequest): Promise<MasterDataItem> {
  const res = await httpClient.post<ApiResponse<MasterDataItem>>(`/api/audit/master-data/${category}`, request);
  return res.data.data;
}

export async function updateMasterDataItem(category: string, id: string, request: MasterDataItemRequest): Promise<MasterDataItem> {
  const res = await httpClient.put<ApiResponse<MasterDataItem>>(`/api/audit/master-data/${category}/${id}`, request);
  return res.data.data;
}

export async function deleteMasterDataItem(category: string, id: string): Promise<void> {
  await httpClient.delete(`/api/audit/master-data/${category}/${id}`);
}

export async function importMasterDataItems(category: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`/api/audit/master-data/${category}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportMasterDataItems(category: string, kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`/api/audit/master-data/${category}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? `${category.toLowerCase()}.xlsx` : `${category.toLowerCase()}.docx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
