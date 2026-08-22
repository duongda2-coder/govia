import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface Position {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export interface PositionRequest {
  code: string;
  name: string;
}

export async function listPositions(): Promise<Position[]> {
  const res = await httpClient.get<ApiResponse<Position[]>>("/api/positions");
  return res.data.data;
}

export async function createPosition(request: PositionRequest): Promise<Position> {
  const res = await httpClient.post<ApiResponse<Position>>("/api/positions", request);
  return res.data.data;
}

export async function updatePosition(id: string, request: PositionRequest): Promise<Position> {
  const res = await httpClient.put<ApiResponse<Position>>(`/api/positions/${id}`, request);
  return res.data.data;
}

export async function setPositionActive(id: string, active: boolean): Promise<Position> {
  const res = await httpClient.patch<ApiResponse<Position>>(`/api/positions/${id}/active`, { active });
  return res.data.data;
}

export async function importPositions(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>("/api/positions/import", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportPositions(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`/api/positions/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "positions.xlsx" : "positions.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
