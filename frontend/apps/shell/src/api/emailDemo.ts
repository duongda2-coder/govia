import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface EmailDemoSetting {
  host: string | null;
  port: number;
  username: string | null;
  passwordSet: boolean;
  smtpAuth: boolean;
  starttls: boolean;
  ssl: boolean;
  fromAddress: string;
  fromName: string | null;
  saved: boolean;
}

export interface EmailDemoSettingRequest {
  host: string;
  port: number;
  username?: string | null;
  password?: string | null;
  smtpAuth: boolean;
  starttls: boolean;
  ssl: boolean;
  fromAddress: string;
  fromName?: string | null;
}

export interface EmailDemoSendRequest {
  to: string;
  cc?: string | null;
  subject: string;
  body?: string | null;
  html: boolean;
  includeActions: boolean;
  detailUrl?: string | null;
}

export type EmailDemoActionStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface EmailDemoLog {
  id: string;
  toAddress: string;
  ccAddress: string | null;
  subject: string;
  body: string | null;
  html: boolean;
  success: boolean;
  errorMessage: string | null;
  smtpHost: string | null;
  sentBy: string | null;
  sentAt: string;
  detailUrl: string | null;
  actionStatus: EmailDemoActionStatus | null;
  actionAt: string | null;
}

const BASE = "/api/admin/email-demo";

export async function getEmailDemoSetting(): Promise<EmailDemoSetting> {
  const res = await httpClient.get<ApiResponse<EmailDemoSetting>>(`${BASE}/setting`);
  return res.data.data;
}

export async function saveEmailDemoSetting(payload: EmailDemoSettingRequest): Promise<EmailDemoSetting> {
  const res = await httpClient.put<ApiResponse<EmailDemoSetting>>(`${BASE}/setting`, payload);
  return res.data.data;
}

export async function testEmailDemoConnection(): Promise<void> {
  await httpClient.post(`${BASE}/setting/test`);
}

export async function sendEmailDemo(payload: EmailDemoSendRequest): Promise<EmailDemoLog> {
  const res = await httpClient.post<ApiResponse<EmailDemoLog>>(`${BASE}/send`, payload);
  return res.data.data;
}

export async function listEmailDemoLogs(): Promise<EmailDemoLog[]> {
  const res = await httpClient.get<ApiResponse<EmailDemoLog[]>>(`${BASE}/logs`);
  return res.data.data;
}
