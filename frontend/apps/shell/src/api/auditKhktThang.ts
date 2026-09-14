import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditKhktThangRowItem {
  year: number;
  auditObjectCode: string;
  auditObjectName: string;
  auditObjectCategoryCode: string | null;
  rankLabel: string | null;
  onBalanceSheetLoan: number | null;
  fundingSource: number | null;
  creditScale: number | null;
  fundingScale: number | null;
  geographicArea: string | null;
  businessSegmentCodes: string[];
  month1: boolean;
  month2: boolean;
  month3: boolean;
  month4: boolean;
  month5: boolean;
  month6: boolean;
  month7: boolean;
  month8: boolean;
  month9: boolean;
  month10: boolean;
  month11: boolean;
  month12: boolean;
  note: string | null;
}

export interface AuditKhktThangUpdateRequest {
  month1: boolean;
  month2: boolean;
  month3: boolean;
  month4: boolean;
  month5: boolean;
  month6: boolean;
  month7: boolean;
  month8: boolean;
  month9: boolean;
  month10: boolean;
  month11: boolean;
  month12: boolean;
  note: string | null;
}

const BASE = "/api/audit/plan/khkt-thang";

export async function listAuditKhktThang(year: number): Promise<AuditKhktThangRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktThangRowItem[]>>(BASE, { params: { year } });
  return res.data.data;
}

export async function updateAuditKhktThang(
  year: number,
  auditObjectCode: string,
  request: AuditKhktThangUpdateRequest,
): Promise<AuditKhktThangRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhktThangRowItem>>(
    `${BASE}/${year}/${encodeURIComponent(auditObjectCode)}`,
    request,
  );
  return res.data.data;
}
