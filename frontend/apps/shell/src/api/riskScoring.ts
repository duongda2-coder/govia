import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

/**
 * Danh muc goc "Loai doi tuong kiem toan" (sheet ZTC_Loai_Dtkt) - la CHA cua Group1 (Group1 la cha
 * cua Group2). Khac voi 4 danh muc "Doi tuong kiem toan" cu the (Unit/Subsidiary/Project/Process)
 * von la cac ban ghi doi tuong duoc kiem toan - danh muc nay chi la PHAN LOAI (CNDT/CNDL/HO/IT/DA).
 */
/** Danh muc "Doi tuong kiem toan" cu the ma "Ma doi tuong KT" tra cuu toi - xem AuditObjectCategoryItem.objectSource. */
export type AuditObjectSource = "UNIT" | "SUBSIDIARY" | "PROCESS" | "PROJECT";

export const AUDIT_OBJECT_SOURCE_OPTIONS: { value: AuditObjectSource; label: string }[] = [
  { value: "UNIT", label: "Đơn vị (HO/GSCC/CN)" },
  { value: "SUBSIDIARY", label: "Công ty con" },
  { value: "PROCESS", label: "Quy trình" },
  { value: "PROJECT", label: "Dự án/DVTN" },
];

function stripDiacritics(value: string): string {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d");
}

/** Doan objectSource tu ma/ten - cung logic voi AuditObjectSource.guess() ben backend, dung de tu
 * dien san "Doi tuong tra cuu" khi NSD go Ma/Ten, tranh bi ket o mac dinh "Du an/DVTN" ma khong biet. */
export function guessObjectSource(code: string, name: string): AuditObjectSource {
  const normalizedCode = stripDiacritics(code.trim());
  const text = stripDiacritics(`${code} ${name}`);
  if (text.includes("quy trinh")) return "PROCESS";
  if (text.includes("cong ty con")) return "SUBSIDIARY";
  // "ho" chi khop khi la CA MA (khong xet trong ten, vi nhieu tu khac cung rut gon ve "ho" sau khi
  // bo dau - "ho so", "ho tro"... - de gay nham lan).
  if (normalizedCode === "ho" || text.includes("don vi") || text.includes("hoi so") || text.includes("tru so") || text.includes("chi nhanh")) {
    return "UNIT";
  }
  return "PROJECT";
}

export interface AuditObjectCategoryItem {
  id: string;
  code: string;
  name: string;
  note: string | null;
  objectSource: AuditObjectSource;
  active: boolean;
}
export interface AuditObjectCategoryRequest {
  code: string;
  name: string;
  note?: string | null;
  objectSource?: AuditObjectSource | null;
  active: boolean;
}

export interface Group1Item {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  code: string;
  name: string;
  weight: number | null;
  validFrom: string | null;
  validTo: string | null;
  active: boolean;
}
export interface Group1Request {
  auditObjectCategoryId: string;
  code: string;
  name: string;
  weight?: number | null;
  validFrom?: string | null;
  validTo?: string | null;
  active: boolean;
}

export interface Group2Item {
  id: string;
  group1Id: string;
  group1Code: string | null;
  code: string;
  name: string;
  weight: number | null;
  validFrom: string | null;
  validTo: string | null;
  active: boolean;
}
export interface Group2Request {
  group1Id: string;
  code: string;
  name: string;
  weight?: number | null;
  validFrom?: string | null;
  validTo?: string | null;
  active: boolean;
}

export interface CriteriaQualitativeItem {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  group1Id: string;
  group1Code: string | null;
  group2Id: string | null;
  group2Code: string | null;
  code: string;
  name: string;
  weight: number | null;
  impactLevel: number | null;
  likelihoodLevel: number | null;
  includeCurrentYear: boolean;
  active: boolean;
}
export interface CriteriaQualitativeRequest {
  auditObjectCategoryId: string;
  group1Id: string;
  group2Id?: string | null;
  code: string;
  name: string;
  weight?: number | null;
  impactLevel?: number | null;
  likelihoodLevel?: number | null;
  includeCurrentYear: boolean;
  active: boolean;
}

export interface CriteriaQuantitativeItem {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  group1Id: string;
  group1Code: string | null;
  group2Id: string | null;
  group2Code: string | null;
  code: string;
  name: string;
  criteriaType: number | null;
  businessThreshold: number | null;
  viewThreshold: number | null;
  score20: number | null;
  score40: number | null;
  score60: number | null;
  score80: number | null;
  score100: number | null;
  scoringGuide: string | null;
  includeCurrentYear: boolean;
  active: boolean;
}
export interface CriteriaQuantitativeRequest {
  auditObjectCategoryId: string;
  group1Id: string;
  group2Id?: string | null;
  code: string;
  name: string;
  criteriaType?: number | null;
  businessThreshold?: number | null;
  viewThreshold?: number | null;
  score20?: number | null;
  score40?: number | null;
  score60?: number | null;
  score80?: number | null;
  score100?: number | null;
  scoringGuide?: string | null;
  includeCurrentYear: boolean;
  active: boolean;
}

export interface FrequencyCoefficientItem {
  id: string;
  code: string;
  fromYear: number | null;
  toYear: number | null;
  label: string;
  value: number | null;
  bonusPoint: number | null;
  repeat: boolean;
  repeatCount: string | null;
  repeatRiskPoint: number | null;
  active: boolean;
}
export interface FrequencyCoefficientRequest {
  code: string;
  fromYear?: number | null;
  toYear?: number | null;
  label: string;
  value?: number | null;
  bonusPoint?: number | null;
  repeat: boolean;
  repeatCount?: string | null;
  repeatRiskPoint?: number | null;
  active: boolean;
}

export interface WeightByBusinessItem {
  id: string;
  businessCode: string;
  qualitativeWeight: number | null;
  quantitativeWeight: number | null;
  fromYear: number | null;
  toYear: number | null;
  active: boolean;
}
export interface WeightByBusinessRequest {
  businessCode: string;
  qualitativeWeight?: number | null;
  quantitativeWeight?: number | null;
  fromYear?: number | null;
  toYear?: number | null;
  active: boolean;
}

export interface WeightByBusinessSegmentItem {
  id: string;
  segmentCode: string;
  qualitativeWeight: number | null;
  quantitativeWeight: number | null;
  fromYear: number | null;
  toYear: number | null;
  active: boolean;
}
export interface WeightByBusinessSegmentRequest {
  segmentCode: string;
  qualitativeWeight?: number | null;
  quantitativeWeight?: number | null;
  fromYear?: number | null;
  toYear?: number | null;
  active: boolean;
}

export interface UserAssignmentItem {
  id: string;
  username: string;
  criteriaId: string;
  criteriaCode: string | null;
  branchCode: string | null;
  classification: string | null;
  active: boolean;
}
export interface UserAssignmentRequest {
  username: string;
  criteriaId: string;
  branchCode?: string | null;
  classification?: string | null;
  active: boolean;
}

export interface MatrixItem {
  id: string;
  frequencyLevel: number;
  frequencyLabel: string;
  scoreLowSeverity: number | null;
  scoreMediumSeverity: number | null;
  scoreHighSeverity: number | null;
  active: boolean;
}
export interface MatrixRequest {
  frequencyLevel: number;
  frequencyLabel: string;
  scoreLowSeverity?: number | null;
  scoreMediumSeverity?: number | null;
  scoreHighSeverity?: number | null;
  active: boolean;
}

export interface ScoreRankItem {
  id: string;
  scoreFrom: number;
  scoreTo: number;
  rankLabel: string;
  fromYear: number;
  toYear: number;
  active: boolean;
}
export interface ScoreRankRequest {
  scoreFrom: number;
  scoreTo: number;
  rankLabel: string;
  fromYear: number;
  toYear?: number | null;
  active: boolean;
}

export interface AuditObjectUnitItem {
  id: string;
  code: string;
  name: string;
  unitType: string;
  auditObjectCategoryId: string | null;
  auditObjectCategoryCode: string | null;
  establishedDate: string | null;
  restructureDate: string | null;
  restructureNote: string | null;
  totalStaff: number | null;
  leaderCount: number | null;
  staffCount: number | null;
  rankValue: number | null;
  defenseLineGroupId: string | null;
  defenseLineGroupCode: string | null;
  operatingRegulation: string | null;
  mainFunction: string | null;
  keyFindings: string | null;
  infoUpdatedDate: string | null;
  active: boolean;
}
export interface AuditObjectUnitRequest {
  code: string;
  name: string;
  unitType: string;
  auditObjectCategoryId?: string | null;
  establishedDate?: string | null;
  restructureDate?: string | null;
  restructureNote?: string | null;
  totalStaff?: number | null;
  leaderCount?: number | null;
  staffCount?: number | null;
  rankValue?: number | null;
  defenseLineGroupId?: string | null;
  operatingRegulation?: string | null;
  mainFunction?: string | null;
  keyFindings?: string | null;
  active: boolean;
}

export interface AuditObjectSubsidiaryItem {
  id: string;
  code: string;
  name: string;
  companyType: string | null;
  establishedDate: string | null;
  staffCount: number | null;
  leaderCount: number | null;
  inspectionYear: number | null;
  inspectionResult: string | null;
  inspectionRecommendation: string | null;
  auditYear: number | null;
  auditResult: string | null;
  auditRecommendation: string | null;
  revenue: number | null;
  cost: number | null;
  profit: number | null;
  salaryFund: number | null;
  active: boolean;
}
export interface AuditObjectSubsidiaryRequest {
  code: string;
  name: string;
  companyType?: string | null;
  establishedDate?: string | null;
  staffCount?: number | null;
  leaderCount?: number | null;
  inspectionYear?: number | null;
  inspectionResult?: string | null;
  inspectionRecommendation?: string | null;
  auditYear?: number | null;
  auditResult?: string | null;
  auditRecommendation?: string | null;
  revenue?: number | null;
  cost?: number | null;
  profit?: number | null;
  salaryFund?: number | null;
  active: boolean;
}

export interface AuditObjectProjectItem {
  id: string;
  code: string;
  name: string;
  projectType: string | null;
  approvalAuthority: string | null;
  purpose: string | null;
  investmentValue: number | null;
  provider: string | null;
  relatedParties: string | null;
  inspectionYear: number | null;
  inspectionResult: string | null;
  inspectionRecommendation: string | null;
  auditYear: number | null;
  auditResult: string | null;
  auditRecommendation: string | null;
  active: boolean;
}
export interface AuditObjectProjectRequest {
  code: string;
  name: string;
  projectType?: string | null;
  approvalAuthority?: string | null;
  purpose?: string | null;
  investmentValue?: number | null;
  provider?: string | null;
  relatedParties?: string | null;
  inspectionYear?: number | null;
  inspectionResult?: string | null;
  inspectionRecommendation?: string | null;
  auditYear?: number | null;
  auditResult?: string | null;
  auditRecommendation?: string | null;
  active: boolean;
}

export interface AuditObjectProcessItem {
  id: string;
  segmentCode: string | null;
  code: string;
  name: string;
  referenceDocument: string | null;
  auditResult: string | null;
  eventNote: string | null;
  incidentNote: string | null;
  reviewResult: string | null;
  active: boolean;
}
export interface AuditObjectProcessRequest {
  segmentCode?: string | null;
  code: string;
  name: string;
  referenceDocument?: string | null;
  auditResult?: string | null;
  eventNote?: string | null;
  incidentNote?: string | null;
  reviewResult?: string | null;
  active: boolean;
}

/**
 * Factory dung chung cho ca 10 danh muc cua module Cham diem rui ro - moi danh muc co hinh dang
 * du lieu rieng (xem cac interface o tren) nhung deu dung chung 1 bo endpoint CRUD + export/import,
 * nen chi can 1 factory thay vi lap lai 10 lan cac ham list/create/update/delete/export/import.
 */
function createResourceApi<TItem, TRequest>(basePath: string, fileBaseName: string) {
  return {
    async list(): Promise<TItem[]> {
      const res = await httpClient.get<ApiResponse<TItem[]>>(basePath);
      return res.data.data;
    },
    async create(request: TRequest): Promise<TItem> {
      const res = await httpClient.post<ApiResponse<TItem>>(basePath, request);
      return res.data.data;
    },
    async update(id: string, request: TRequest): Promise<TItem> {
      const res = await httpClient.put<ApiResponse<TItem>>(`${basePath}/${id}`, request);
      return res.data.data;
    },
    async remove(id: string): Promise<void> {
      await httpClient.delete(`${basePath}/${id}`);
    },
    async importExcel(file: File): Promise<ImportResult> {
      const formData = new FormData();
      formData.append("file", file);
      const res = await httpClient.post<ApiResponse<ImportResult>>(`${basePath}/import`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return res.data.data;
    },
    /** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
    async exportFile(kind: "excel" | "word"): Promise<void> {
      const res = await httpClient.get(`${basePath}/export/${kind}`, { responseType: "blob" });
      const blobUrl = window.URL.createObjectURL(res.data as Blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = kind === "excel" ? `${fileBaseName}.xlsx` : `${fileBaseName}.docx`;
      link.click();
      window.URL.revokeObjectURL(blobUrl);
    },
  };
}

// Sub-module "Master data CDRR" ben trong module Cham diem rui ro - namespace rieng de danh cho
// sub-module "Cham diem" (thuc hien cham diem, dung du lieu tu day) se them sau nay, tranh dung
// duong dan voi cac endpoint CRUD danh muc o day.
const BASE = "/api/audit/risk-scoring/master-data";

export const auditObjectCategoryApi = createResourceApi<AuditObjectCategoryItem, AuditObjectCategoryRequest>(
  `${BASE}/audit-object-category`, "risk_score_audit_object_category");
export const group1Api = createResourceApi<Group1Item, Group1Request>(`${BASE}/group1`, "risk_score_group1");
export const group2Api = createResourceApi<Group2Item, Group2Request>(`${BASE}/group2`, "risk_score_group2");
export const criteriaQualitativeApi = createResourceApi<CriteriaQualitativeItem, CriteriaQualitativeRequest>(
  `${BASE}/criteria-qualitative`, "risk_score_criteria_qualitative");
export const criteriaQuantitativeApi = createResourceApi<CriteriaQuantitativeItem, CriteriaQuantitativeRequest>(
  `${BASE}/criteria-quantitative`, "risk_score_criteria_quantitative");
export const frequencyCoefficientApi = createResourceApi<FrequencyCoefficientItem, FrequencyCoefficientRequest>(
  `${BASE}/frequency-coefficient`, "risk_score_frequency_coefficient");
export const weightByBusinessApi = createResourceApi<WeightByBusinessItem, WeightByBusinessRequest>(
  `${BASE}/weight-by-business`, "risk_score_weight_by_business");
export const weightByBusinessSegmentApi = createResourceApi<WeightByBusinessSegmentItem, WeightByBusinessSegmentRequest>(
  `${BASE}/weight-by-business-segment`, "risk_score_weight_by_business_segment");
export const userAssignmentApi = createResourceApi<UserAssignmentItem, UserAssignmentRequest>(
  `${BASE}/user-assignment`, "risk_score_user_assignment");
export const matrixApi = createResourceApi<MatrixItem, MatrixRequest>(`${BASE}/matrix`, "risk_score_matrix");
export const rankApi = createResourceApi<ScoreRankItem, ScoreRankRequest>(`${BASE}/rank`, "risk_score_rank");

// 4 danh muc "Doi tuong kiem toan" (sheet ZTC_DTKT1-4, file "Cham diem - master data (1).xlsx") -
// them vao sub-module Master Data CDRR da co san, dung chung permission AUDIT.RISK_SCORING.*
export const auditObjectUnitApi = createResourceApi<AuditObjectUnitItem, AuditObjectUnitRequest>(
  `${BASE}/audit-object-unit`, "risk_score_audit_object_unit");
export const auditObjectSubsidiaryApi = createResourceApi<AuditObjectSubsidiaryItem, AuditObjectSubsidiaryRequest>(
  `${BASE}/audit-object-subsidiary`, "risk_score_audit_object_subsidiary");
export const auditObjectProjectApi = createResourceApi<AuditObjectProjectItem, AuditObjectProjectRequest>(
  `${BASE}/audit-object-project`, "risk_score_audit_object_project");
export const auditObjectProcessApi = createResourceApi<AuditObjectProcessItem, AuditObjectProcessRequest>(
  `${BASE}/audit-object-process`, "risk_score_audit_object_process");
