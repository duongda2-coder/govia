import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditEngagementItem } from "./auditEngagement";

/** Phan he "Đánh giá chất lượng" (DGCL_CN.xlsx) - xem AuditDgclController. */
const BASE = "/api/audit/dgcl";

export type DgclAppendix = "PL01A" | "PL01B" | "PL01F";
export const DGCL_APPENDICES: DgclAppendix[] = ["PL01A", "PL01B", "PL01F"];
export const DGCL_ATTACHMENT_ENTITY = "AUDIT_DGCL_LINE";
export const DGCL_TEAM_SUBJECT = "TEAM";

export interface DgclCapability {
  canEvaluate: boolean;
  canControl: boolean;
}

export interface DgclSubjectRow {
  subjectKey: string;
  team: boolean;
  engagementCode: string;
  employeeId: string | null;
  employeeCode: string | null;
  employeeName: string;
  segmentCodes: string | null;
  role: "TD" | "TV" | null;
  pl01aScore: number | null;
  pl01bScore: number | null;
  pl01fScore: number | null;
  bonusPoints: number | null;
  penaltyPoints: number | null;
  classification: string | null;
  /** test 30.9: diem/xep loai la cua phieu da Luu; cac co nay cho biet phieu da "Xac nhan hoan thanh" chua. */
  pl01aConfirmed: boolean;
  pl01bConfirmed: boolean;
  pl01fConfirmed: boolean;
  confirmedCount: number;
  controlledCount: number;
  evaluatorNames: string | null;
  controllerNames: string | null;
}

/** kind (chi PL01F): HEADER, NOTE, SCORE_A, SCORE_B, SCORE_QUALITY, DEDUCTION_GROUP, DEDUCTION, WEIGHTED_TOTAL,
 * BONUS_GROUP, BONUS, PENALTY_GROUP, PENALTY, GRAND_TOTAL, CLASSIFICATION. */
export interface DgclLine {
  key: string;
  stt: string | null;
  content: string;
  header: boolean;
  segment: string | null;
  kind: string | null;
  rate: number | null;
  /** Cot "tick" cua file 01A/pl01b: tieu chi can cham (phieu moi tu tich san). */
  tick: boolean;
  required: boolean;
  compliant: boolean;
  nonCompliant: boolean;
  checked: boolean;
  violationCount: number | null;
  maxScore: number | null;
  detail: string | null;
  document: string | null;
  note: string | null;
  evaluatorName: string | null;
  calcMax: number | null;
  calcDeduction: number | null;
  calcPoints: number | null;
  calcRatio: number | null;
  attachmentEntityId: string;
}

export interface DgclSummary {
  requiredCount: number | null;
  compliantCount: number | null;
  nonCompliantCount: number | null;
  ratio: number | null;
  score: number | null;
  weightedMax: number | null;
  bonusPoints: number | null;
  penaltyPoints: number | null;
  classification: string | null;
}

export interface DgclSheet {
  engagementId: string;
  engagementCode: string;
  subjectKey: string;
  team: boolean;
  subjectName: string;
  segmentCodes: string | null;
  appendix: DgclAppendix;
  lines: DgclLine[];
  summary: DgclSummary;
  /** false = phieu chua luu lan nao, NDTH/Tuân thủ dang la gia tri tich san. */
  saved: boolean;
  evaluatorName: string | null;
  confirmed: boolean;
  confirmedBy: string | null;
  confirmedAt: string | null;
  controlled: boolean;
  controlledBy: string | null;
  controlledAt: string | null;
  canEdit: boolean;
  canConfirm: boolean;
  canUnconfirm: boolean;
  canControl: boolean;
  canUncontrol: boolean;
}

export interface DgclLineInput {
  key: string;
  required: boolean;
  compliant: boolean;
  nonCompliant: boolean;
  checked: boolean;
  violationCount: number | null;
  maxScore: number | null;
  detail: string | null;
  document: string | null;
  note: string | null;
}

export type DgclSheetAction = "confirm" | "unconfirm" | "control" | "uncontrol";

export async function listDgclEngagements(): Promise<AuditEngagementItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementItem[]>>(`${BASE}/engagements`);
  return res.data.data;
}

export async function getDgclCapability(): Promise<DgclCapability> {
  const res = await httpClient.get<ApiResponse<DgclCapability>>(`${BASE}/capability`);
  return res.data.data;
}

export async function listDgclSubjects(engagementId: string): Promise<DgclSubjectRow[]> {
  const res = await httpClient.get<ApiResponse<DgclSubjectRow[]>>(`${BASE}/engagements/${engagementId}/subjects`);
  return res.data.data;
}

const sheetUrl = (engagementId: string, subjectKey: string, appendix: DgclAppendix) =>
  `${BASE}/engagements/${engagementId}/subjects/${subjectKey}/${appendix}`;

export async function getDgclSheet(engagementId: string, subjectKey: string, appendix: DgclAppendix): Promise<DgclSheet> {
  const res = await httpClient.get<ApiResponse<DgclSheet>>(sheetUrl(engagementId, subjectKey, appendix));
  return res.data.data;
}

export async function saveDgclSheet(engagementId: string, subjectKey: string, appendix: DgclAppendix, lines: DgclLineInput[]): Promise<DgclSheet> {
  const res = await httpClient.put<ApiResponse<DgclSheet>>(sheetUrl(engagementId, subjectKey, appendix), { lines });
  return res.data.data;
}

export async function runDgclSheetAction(engagementId: string, subjectKey: string, appendix: DgclAppendix, action: DgclSheetAction): Promise<DgclSheet> {
  const res = await httpClient.post<ApiResponse<DgclSheet>>(`${sheetUrl(engagementId, subjectKey, appendix)}/${action}`);
  return res.data.data;
}

function downloadBlob(data: Blob, fileName: string) {
  const blobUrl = window.URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

export async function exportDgclPl04b1(engagementId: string, engagementCode: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/engagements/${engagementId}/pl04b1`, { responseType: "blob" });
  downloadBlob(res.data as Blob, `PL04B1_${engagementCode}.xlsx`);
}

/** Nut "Xuất PL01A/PL01B/PL01F" (test 30.9): phieu dang xem do vao mau FORM_PL01A/B/F. */
export async function exportDgclSheet(engagementId: string, subjectKey: string, appendix: DgclAppendix, fileName: string): Promise<void> {
  const res = await httpClient.get(`${sheetUrl(engagementId, subjectKey, appendix)}/export`, { responseType: "blob" });
  downloadBlob(res.data as Blob, fileName);
}
