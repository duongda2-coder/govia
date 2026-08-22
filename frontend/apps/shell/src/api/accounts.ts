import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AccountStatus = "ACTIVE" | "LOCKED" | "DISABLED";

export interface AccountSummary {
  id: string;
  username: string;
  employeeId: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  status: AccountStatus;
  roleCodes: string[];
}

export async function listAccounts(): Promise<AccountSummary[]> {
  const res = await httpClient.get<ApiResponse<AccountSummary[]>>("/api/accounts");
  return res.data.data;
}

export async function assignAccountRoles(id: string, roleIds: string[]): Promise<void> {
  await httpClient.put(`/api/accounts/${id}/roles`, { roleIds });
}

/** Sao chep TOAN BO vai tro tu tai khoan sourceAccountId sang tai khoan id - ghi de vai tro hien co cua id. */
export async function copyAccountRoles(id: string, sourceAccountId: string): Promise<void> {
  await httpClient.post(`/api/accounts/${id}/copy-roles`, { sourceAccountId });
}

/** Tai file Excel ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAccounts(): Promise<void> {
  const res = await httpClient.get("/api/accounts/export/excel", { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = "accounts.xlsx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
