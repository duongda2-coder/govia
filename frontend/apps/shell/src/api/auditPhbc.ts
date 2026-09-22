import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

/** Màn hình "Phát hành báo cáo" (file ztc_phbc.xlsx): danh sách báo cáo đã phát hành + kiến nghị đính kèm từng báo cáo. */
const BASE = "/api/audit/phbc";

export type PhbcIssuingUnit = "KTNB" | "BKS";
export type PhbcRecommendationCode = "M01" | "M02" | "M03" | "M04";
export type PhbcRecommendationTarget = "HDTV" | "TGD" | "ALL";

// ===================== helpers =====================

async function get<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  const res = await httpClient.get<ApiResponse<T>>(`${BASE}${path}`, { params });
  return res.data.data;
}

async function send<T>(method: "post" | "put", path: string, body?: unknown): Promise<T> {
  const res = await httpClient.request<ApiResponse<T>>({ method, url: `${BASE}${path}`, data: body });
  return res.data.data;
}

async function remove(path: string): Promise<void> {
  await httpClient.delete(`${BASE}${path}`);
}

async function importFile(path: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}${path}`, formData, { headers: { "Content-Type": "multipart/form-data" } });
  return res.data.data;
}

async function download(path: string, filename: string): Promise<void> {
  const res = await httpClient.get(`${BASE}${path}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

// ===================== lookups (list dùng chung) =====================

export interface PhbcOption {
  id: string;
  code: string;
  name: string;
}

export interface PhbcLookups {
  businessSegments: PhbcOption[];
  executingUnits: PhbcOption[];
}

export const getPhbcLookups = () => get<PhbcLookups>("/lookups");

// ===================== 1. màn hình Phát hành báo cáo =====================

export interface PhbcReportItem {
  id: string;
  reportNumber: string;
  reportDate: string | null;
  reportName: string;
  summaryContent: string | null;
  issuingUnit: PhbcIssuingUnit | null;
  issuingUnitLabel: string | null;
  recommendationCount: number;
}

export interface PhbcReportRequest {
  reportNumber: string;
  reportDate: string | null;
  reportName: string;
  summaryContent: string | null;
  issuingUnit: PhbcIssuingUnit | null;
}

export const listPhbcReports = () => get<PhbcReportItem[]>("/reports");
export const createPhbcReport = (request: PhbcReportRequest) => send<PhbcReportItem>("post", "/reports", request);
export const updatePhbcReport = (id: string, request: PhbcReportRequest) => send<PhbcReportItem>("put", `/reports/${id}`, request);
export const deletePhbcReport = (id: string) => remove(`/reports/${id}`);
export const exportPhbcReports = () => download("/reports/export/excel", "phbc_bao_cao.xlsx");
export const importPhbcReports = (file: File) => importFile("/reports/import", file);

// ===================== 2. kiến nghị trong báo cáo =====================

export interface PhbcRecommendationItem {
  id: string;
  reportIssuanceId: string;
  reportNumber: string | null;
  recommendationCode: PhbcRecommendationCode;
  recommendationName: string;
  content: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  target: PhbcRecommendationTarget | null;
  targetLabel: string | null;
  executingUnitId: string | null;
  executingUnitCode: string | null;
  executingUnitName: string | null;
  deadline: string | null;
}

export interface PhbcRecommendationRequest {
  reportIssuanceId: string;
  recommendationCode: PhbcRecommendationCode;
  content: string;
  businessSegmentId: string | null;
  target: PhbcRecommendationTarget | null;
  executingUnitId: string | null;
  executingUnitName: string | null;
  deadline: string | null;
}

export const listPhbcRecommendations = (reportIssuanceId?: string) =>
  get<PhbcRecommendationItem[]>("/recommendations", reportIssuanceId ? { reportIssuanceId } : undefined);
export const createPhbcRecommendation = (request: PhbcRecommendationRequest) => send<PhbcRecommendationItem>("post", "/recommendations", request);
export const updatePhbcRecommendation = (id: string, request: PhbcRecommendationRequest) => send<PhbcRecommendationItem>("put", `/recommendations/${id}`, request);
export const deletePhbcRecommendation = (id: string) => remove(`/recommendations/${id}`);
export const exportPhbcRecommendations = () => download("/recommendations/export/excel", "phbc_kien_nghi.xlsx");
export const importPhbcRecommendations = (file: File) => importFile("/recommendations/import", file);
