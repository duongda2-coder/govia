import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface GroupHOItem {
  id: string;
  code: string;
  name: string;
  note: string | null;
  active: boolean;
}
export interface GroupHORequest {
  code: string;
  name: string;
  note?: string | null;
  active: boolean;
}

export interface RiskTypeHOItem {
  id: string;
  groupHoId: string;
  groupHoCode: string | null;
  groupHoName: string | null;
  code: string;
  name: string;
  weight: number | null;
  active: boolean;
}
export interface RiskTypeHORequest {
  groupHoId: string;
  code: string;
  name: string;
  weight?: number | null;
  active: boolean;
}

export interface RiskCriteriaOtherItem {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  code: string;
  name: string;
  weight: number | null;
  groupHoId: string;
  groupHoCode: string | null;
  groupHoName: string | null;
  riskTypeHoId: string;
  riskTypeHoCode: string | null;
  riskTypeHoName: string | null;
  active: boolean;
}
export interface RiskCriteriaOtherRequest {
  auditObjectCategoryId: string;
  code: string;
  name: string;
  weight?: number | null;
  groupHoId: string;
  riskTypeHoId: string;
  active: boolean;
}

export interface RiskAssessmentOtherHeaderItem {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  auditObjectCode: string;
  auditObjectName: string | null;
  year: number;
  active: boolean;
}
export interface RiskAssessmentOtherHeaderRequest {
  auditObjectCategoryId: string;
  auditObjectCode: string;
  year: number;
  active: boolean;
}

export interface RiskAssessmentOtherLineItem {
  id: string;
  headerId: string;
  criteriaOtherId: string;
  criteriaOtherCode: string | null;
  criteriaOtherName: string | null;
  scaleId: string | null;
  scaleScore: number | null;
  ratingLevel: string | null;
}
export interface RiskAssessmentOtherLineRequest {
  scaleId: string | null;
}

export interface RiskAssessmentOtherRankingItem {
  headerId: string;
  year: number;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  auditObjectCode: string;
  auditObjectName: string | null;
  riskScore: number;
  rankLabel: string | null;
}

export interface RiskAssessmentOtherExpertRankItem {
  id: string;
  year: number;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  auditObjectCode: string;
  auditObjectName: string | null;
  riskScore: number | null;
  baseRankLabel: string | null;
  reRankLabel: string | null;
  reason: string | null;
  assessedDate: string | null;
  expertName: string | null;
  finalRankLabel: string | null;
  updatedBy: string | null;
}
export interface RiskAssessmentOtherExpertRankRequest {
  reRankLabel: string | null;
  reason: string | null;
  assessedDate: string | null;
  expertName: string | null;
  finalRankLabel: string | null;
}

export interface RiskCriteriaQuantitativeValueItem {
  id: string;
  year: number;
  branchCode: string;
  branchName: string | null;
  criteriaId: string;
  criteriaCode: string | null;
  criteriaName: string | null;
  entryDate: string | null;
  value: number | null;
}

export interface RiskCriteriaQualitativeValueItem {
  id: string;
  year: number;
  branchCode: string;
  branchName: string | null;
  criteriaId: string;
  criteriaCode: string | null;
  criteriaName: string | null;
  violation: string | null;
  note: string | null;
}

export interface RiskCriteriaOtherScaleItem {
  id: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  criteriaOtherId: string;
  criteriaOtherCode: string | null;
  criteriaOtherName: string | null;
  scaleScore: number;
  ratingLevel: string;
  description: string | null;
  active: boolean;
}
export interface RiskCriteriaOtherScaleRequest {
  auditObjectCategoryId: string;
  criteriaOtherId: string;
  scaleScore: number;
  ratingLevel: string;
  description?: string | null;
  active: boolean;
}

/**
 * Factory dung chung cho sub-module "Cham Diem" - cung mo hinh voi factory cua Master Data CDRR
 * (xem api/riskScoring.ts) nhung namespace rieng /scoring vi day la sub-module khac, phan quyen doc lap.
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

const BASE = "/api/audit/risk-scoring/scoring";

export const groupHOApi = createResourceApi<GroupHOItem, GroupHORequest>(`${BASE}/group-ho`, "risk_score_group_ho");
export const riskTypeHOApi = createResourceApi<RiskTypeHOItem, RiskTypeHORequest>(`${BASE}/risk-type-ho`, "risk_score_type_ho");
export const riskCriteriaOtherApi = createResourceApi<RiskCriteriaOtherItem, RiskCriteriaOtherRequest>(
  `${BASE}/criteria-other`,
  "risk_score_criteria_other",
);
export const riskCriteriaOtherScaleApi = createResourceApi<RiskCriteriaOtherScaleItem, RiskCriteriaOtherScaleRequest>(
  `${BASE}/criteria-other-scale`,
  "risk_score_criteria_other_scale",
);

const assessmentOtherResource = createResourceApi<RiskAssessmentOtherHeaderItem, RiskAssessmentOtherHeaderRequest>(
  `${BASE}/assessment-other`,
  "risk_score_assessment_other",
);
export const riskAssessmentOtherApi = {
  ...assessmentOtherResource,
  async lines(headerId: string): Promise<RiskAssessmentOtherLineItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherLineItem[]>>(`${BASE}/assessment-other/${headerId}/lines`);
    return res.data.data;
  },
  async updateLine(headerId: string, lineId: string, request: RiskAssessmentOtherLineRequest): Promise<RiskAssessmentOtherLineItem> {
    const res = await httpClient.put<ApiResponse<RiskAssessmentOtherLineItem>>(`${BASE}/assessment-other/${headerId}/lines/${lineId}`, request);
    return res.data.data;
  },
};

/** "Bang xep hang cham diem rui ro khac" (sheet ZTC_BXHRR_KHAC) - man hinh CHI XEM, tinh dong theo 1 nam. */
export const riskAssessmentOtherRankingApi = {
  async list(year: number): Promise<RiskAssessmentOtherRankingItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherRankingItem[]>>(`${BASE}/assessment-other-ranking`, { params: { year } });
    return res.data.data;
  },
};

/** "Xep hang rui ro theo y kien chuyen gia cua DTKT khac" (sheet ZTC_XHRR_KHAC_CG). */
export const riskAssessmentOtherExpertRankApi = {
  async list(year: number): Promise<RiskAssessmentOtherExpertRankItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherExpertRankItem[]>>(`${BASE}/assessment-other-expert-rank`, { params: { year } });
    return res.data.data;
  },
  /** "Nut cap nhat du lieu tu nguon" - keo lai Diem rui ro/Xep loai moi nhat, giu nguyen cac truong chuyen gia da nhap. */
  async sync(year: number): Promise<RiskAssessmentOtherExpertRankItem[]> {
    const res = await httpClient.post<ApiResponse<RiskAssessmentOtherExpertRankItem[]>>(`${BASE}/assessment-other-expert-rank/sync`, null, {
      params: { year },
    });
    return res.data.data;
  },
  async update(id: string, request: RiskAssessmentOtherExpertRankRequest): Promise<RiskAssessmentOtherExpertRankItem> {
    const res = await httpClient.put<ApiResponse<RiskAssessmentOtherExpertRankItem>>(`${BASE}/assessment-other-expert-rank/${id}`, request);
    return res.data.data;
  },
};

/** "Ho so rui ro dinh luong" (sheet ZTC_HSRR, mau DL_HSRR_Upload) - wide-format, co phan quyen theo user/chi tieu. */
export const riskCriteriaQuantitativeValueApi = {
  async list(year: number): Promise<RiskCriteriaQuantitativeValueItem[]> {
    const res = await httpClient.get<ApiResponse<RiskCriteriaQuantitativeValueItem[]>>(`${BASE}/hsrr/quantitative`, { params: { year } });
    return res.data.data;
  },
  async importExcel(file: File): Promise<ImportResult> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/hsrr/quantitative/import`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data.data;
  },
};

/** "Ho so rui ro dinh tinh" (sheet ZTC_HSRR, mau DT_HSRR_Upload) - long-format. */
export const riskCriteriaQualitativeValueApi = {
  async list(year: number): Promise<RiskCriteriaQualitativeValueItem[]> {
    const res = await httpClient.get<ApiResponse<RiskCriteriaQualitativeValueItem[]>>(`${BASE}/hsrr/qualitative`, { params: { year } });
    return res.data.data;
  },
  async importExcel(file: File): Promise<ImportResult> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/hsrr/qualitative/import`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data.data;
  },
};
