import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  await httpClient.patch<ApiResponse<void>>("/api/auth/password", request);
}
