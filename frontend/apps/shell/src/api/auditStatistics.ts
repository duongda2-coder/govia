import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface YearSegmentStatRow {
  year: number | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  ttssCount: number;
  materialTtssCount: number;
  recommendationCount: number;
}

export interface TtssDetailRow {
  engagementCode: string | null;
  findingCode: string | null;
  findingName: string | null;
  material: boolean;
  customerName: string | null;
  amount: number | null;
  exceptionDate: string | null;
}

export interface EngagementSegmentStatRow {
  engagementCode: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  ttssCount: number;
  materialTtssCount: number;
  recommendationCount: number;
}

export interface EmployeeStatRow {
  employeeUsername: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  year: number | null;
  ttssCount: number;
  materialTtssCount: number;
  recommendationCount: number;
}

export interface UnitStatRow {
  auditObjectUnitId: string;
  unitCode: string | null;
  unitName: string | null;
  engagementCount: number;
  ttssCount: number;
  materialTtssCount: number;
  recommendationCount: number;
}

export interface UnitDetailStatRow {
  decisionNumber: string | null;
  year: number | null;
  ttssCount: number;
  materialTtssCount: number;
  recommendationCount: number;
  completedRecommendationCount: number;
  riskRank: string | null;
}

export interface EngagementOption {
  code: string;
}

export interface EmployeeCodeOption {
  employeeCode: string;
  fullName: string;
}

const BASE = "/api/audit/statistics";

export async function listStatisticsYearOptions(): Promise<number[]> {
  const res = await httpClient.get<ApiResponse<number[]>>(`${BASE}/lookups/years`);
  return res.data.data;
}

export async function listStatisticsEngagementOptions(): Promise<EngagementOption[]> {
  const res = await httpClient.get<ApiResponse<EngagementOption[]>>(`${BASE}/lookups/engagements`);
  return res.data.data;
}

export async function listStatisticsEmployeeOptions(): Promise<EmployeeCodeOption[]> {
  const res = await httpClient.get<ApiResponse<EmployeeCodeOption[]>>(`${BASE}/lookups/employees`);
  return res.data.data;
}

export async function fetchStatsByYear(year?: number): Promise<YearSegmentStatRow[]> {
  const res = await httpClient.get<ApiResponse<YearSegmentStatRow[]>>(`${BASE}/by-year`, { params: { year } });
  return res.data.data;
}

export async function fetchTtssDetail(year: number, businessSegmentId: string): Promise<TtssDetailRow[]> {
  const res = await httpClient.get<ApiResponse<TtssDetailRow[]>>(`${BASE}/by-year/ttss-detail`, { params: { year, businessSegmentId } });
  return res.data.data;
}

export async function fetchStatsByEngagement(engagementCode?: string): Promise<EngagementSegmentStatRow[]> {
  const res = await httpClient.get<ApiResponse<EngagementSegmentStatRow[]>>(`${BASE}/by-engagement`, { params: { engagementCode } });
  return res.data.data;
}

export async function fetchStatsByEmployee(employeeCode?: string): Promise<EmployeeStatRow[]> {
  const res = await httpClient.get<ApiResponse<EmployeeStatRow[]>>(`${BASE}/by-employee`, { params: { employeeCode } });
  return res.data.data;
}

export async function fetchStatsByUnit(): Promise<UnitStatRow[]> {
  const res = await httpClient.get<ApiResponse<UnitStatRow[]>>(`${BASE}/by-unit`);
  return res.data.data;
}

export async function fetchUnitDetail(auditObjectUnitId: string): Promise<UnitDetailStatRow[]> {
  const res = await httpClient.get<ApiResponse<UnitDetailStatRow[]>>(`${BASE}/by-unit/${auditObjectUnitId}/detail`);
  return res.data.data;
}

/** Tai file Excel ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
async function downloadExcel(url: string, params: Record<string, unknown>, filename: string): Promise<void> {
  const res = await httpClient.get(url, { params, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export const exportStatsByYear = (year?: number) => downloadExcel(`${BASE}/by-year/export`, { year }, "thong_ke_theo_nam.xlsx");
export const exportStatsByEngagement = (engagementCode?: string) =>
  downloadExcel(`${BASE}/by-engagement/export`, { engagementCode }, "thong_ke_theo_ckt.xlsx");
export const exportStatsByEmployee = (employeeCode?: string) =>
  downloadExcel(`${BASE}/by-employee/export`, { employeeCode }, "thong_ke_theo_thanh_vien.xlsx");
export const exportStatsByUnit = () => downloadExcel(`${BASE}/by-unit/export`, {}, "thong_ke_theo_don_vi.xlsx");
