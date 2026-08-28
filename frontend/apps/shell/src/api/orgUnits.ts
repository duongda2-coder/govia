import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface OrganizationUnit {
  id: string;
  code: string;
  name: string;
  type: string | null;
  levelCode: string | null;
  parentId: string | null;
  managerEmployeeId: string | null;
  managerEmployeeName: string | null;
  active: boolean;
}

export interface OrganizationUnitRequest {
  code: string;
  name: string;
  type?: string | null;
  levelCode?: string | null;
  parentId?: string | null;
  managerEmployeeId?: string | null;
}

export async function listOrgUnits(): Promise<OrganizationUnit[]> {
  const res = await httpClient.get<ApiResponse<OrganizationUnit[]>>("/api/org-units");
  return res.data.data;
}

export async function createOrgUnit(request: OrganizationUnitRequest): Promise<OrganizationUnit> {
  const res = await httpClient.post<ApiResponse<OrganizationUnit>>("/api/org-units", request);
  return res.data.data;
}

export async function updateOrgUnit(id: string, request: OrganizationUnitRequest): Promise<OrganizationUnit> {
  const res = await httpClient.put<ApiResponse<OrganizationUnit>>(`/api/org-units/${id}`, request);
  return res.data.data;
}

export async function setOrgUnitActive(id: string, active: boolean): Promise<OrganizationUnit> {
  const res = await httpClient.patch<ApiResponse<OrganizationUnit>>(`/api/org-units/${id}/active`, { active });
  return res.data.data;
}

export async function deleteOrgUnit(id: string): Promise<void> {
  await httpClient.delete(`/api/org-units/${id}`);
}

export async function importOrgUnits(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>("/api/org-units/import", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportOrgUnits(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`/api/org-units/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "org-units.xlsx" : "org-units.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
