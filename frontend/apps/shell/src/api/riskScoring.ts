import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type ObjectType = "CNDT" | "CNDL" | "HO" | "CNTT" | "DA";

export const OBJECT_TYPE_OPTIONS: { value: ObjectType; label: string }[] = [
  { value: "CNDT", label: "Chi nhánh - Định tính" },
  { value: "CNDL", label: "Chi nhánh - Định lượng" },
  { value: "HO", label: "Hội sở" },
  { value: "CNTT", label: "Công nghệ thông tin" },
  { value: "DA", label: "Dự án" },
];

export interface Group1Item {
  id: string;
  objectType: ObjectType;
  code: string;
  name: string;
  weight: number | null;
  validFrom: string | null;
  validTo: string | null;
  active: boolean;
}
export interface Group1Request {
  objectType: ObjectType;
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
  objectType: ObjectType;
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
  objectType: ObjectType;
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
  objectType: ObjectType;
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
  objectType: ObjectType;
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
