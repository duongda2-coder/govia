import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface Role {
  id: string;
  code: string;
  name: string;
  description: string | null;
  systemDefined: boolean;
}

export interface RoleRequest {
  code: string;
  name: string;
  description?: string | null;
}

export interface Permission {
  id: string;
  code: string;
  module: string;
  description: string | null;
  resourceLabel: string | null;
}

export async function listRoles(): Promise<Role[]> {
  const res = await httpClient.get<ApiResponse<Role[]>>("/api/roles");
  return res.data.data;
}

export async function createRole(request: RoleRequest): Promise<Role> {
  const res = await httpClient.post<ApiResponse<Role>>("/api/roles", request);
  return res.data.data;
}

export async function updateRole(id: string, request: RoleRequest): Promise<Role> {
  const res = await httpClient.put<ApiResponse<Role>>(`/api/roles/${id}`, request);
  return res.data.data;
}

export async function deleteRole(id: string): Promise<void> {
  await httpClient.delete(`/api/roles/${id}`);
}

/** Tai file Excel ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportRoles(): Promise<void> {
  const res = await httpClient.get("/api/roles/export/excel", { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = "roles.xlsx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export async function getRolePermissions(id: string): Promise<string[]> {
  const res = await httpClient.get<ApiResponse<string[]>>(`/api/roles/${id}/permissions`);
  return res.data.data;
}

export async function setRolePermissions(id: string, permissionCodes: string[]): Promise<void> {
  await httpClient.put(`/api/roles/${id}/permissions`, { permissionCodes });
}

export async function listPermissions(): Promise<Permission[]> {
  const res = await httpClient.get<ApiResponse<Permission[]>>("/api/permissions");
  return res.data.data;
}

export async function importRolePermissions(id: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`/api/roles/${id}/permissions/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportRolePermissions(id: string): Promise<void> {
  const res = await httpClient.get(`/api/roles/${id}/permissions/export`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = "role-permissions.xlsx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
