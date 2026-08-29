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
  groupHoId: string | null;
  groupHoCode: string | null;
  groupHoName: string | null;
  riskTypeHoId: string | null;
  riskTypeHoCode: string | null;
  riskTypeHoName: string | null;
  active: boolean;
}
export interface RiskCriteriaOtherRequest {
  auditObjectCategoryId: string;
  code: string;
  name: string;
  weight?: number | null;
  groupHoId?: string | null;
  riskTypeHoId?: string | null;
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

/** Ban "phang" cua RiskAssessmentOtherHeaderItem - 1 item ung voi 1 chi tieu (line) cua 1 header,
 * dung cho man hinh danh sach hien 1 dong/1 chi tieu (dung dinh dang voi file Excel export/import). */
export interface RiskAssessmentOtherRowItem {
  headerId: string;
  auditObjectCategoryId: string;
  auditObjectCategoryCode: string | null;
  auditObjectCategoryName: string | null;
  auditObjectCode: string;
  auditObjectName: string | null;
  year: number;
  active: boolean;
  lineId: string;
  criteriaOtherId: string;
  criteriaOtherCode: string | null;
  criteriaOtherName: string | null;
  scaleId: string | null;
  scaleScore: number | null;
  ratingLevel: string | null;
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
export interface RiskCriteriaQuantitativeValueRequest {
  criteriaId: string;
  branchCode: string;
  year: number;
  entryDate?: string | null;
  value?: number | null;
}

/** Ban "wide" cua RiskCriteriaQuantitativeValueItem - 1 item ung voi 1 chi nhanh/nam, dung dinh
 * dang voi sheet DL_Nhaptructiep / mau DL_HSRR_Upload (tung chi tieu la 1 cot thay vi 1 dong). */
export interface RiskCriteriaQuantitativeWideRowItem {
  branchCode: string;
  branchName: string | null;
  year: number;
  entryDate: string | null;
  valuesByCriteriaCode: Record<string, number | null>;
}
export interface RiskCriteriaQuantitativeWideRowRequest {
  branchCode: string;
  year: number;
  entryDate?: string | null;
  valuesByCriteriaCode: Record<string, number | null>;
}

export interface RiskCriteriaQualitativeValueItem {
  id: string;
  year: number;
  branchCode: string;
  branchName: string | null;
  criteriaId: string;
  criteriaCode: string | null;
  criteriaName: string | null;
  group1Code: string | null;
  group2Code: string | null;
  violation: string | null;
  note: string | null;
}
export interface RiskCriteriaQualitativeValueRequest {
  criteriaId: string;
  branchCode: string;
  year: number;
  violation?: string | null;
  note?: string | null;
}

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_DL) - moi chi tieu dinh luong la 1 entry trong
 * scoresByCriteriaCode (da la diem dong gop, khong phai gia tri HSRR tho). */
export interface RiskBranchScoreQuantitativeRowItem {
  branchCode: string;
  branchName: string | null;
  year: number;
  totalScore: number;
  rankLabel: string | null;
  scoresByCriteriaCode: Record<string, number | null>;
}

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_DT) - moi nhom cap 2 la 1 entry trong scoresByGroup2Code
 * (da la tong diem dong gop cua cac chi tieu thuoc nhom do, khong phai gia tri HSRR tho). */
export interface RiskBranchScoreQualitativeRowItem {
  branchCode: string;
  branchName: string | null;
  year: number;
  totalScore: number;
  rankLabel: string | null;
  scoresByGroup2Code: Record<string, number | null>;
}

/** 1 dong = 1 chi nhanh/nam (sheet CT_Diem_All) - moi nghiep vu la 1 entry trong
 * scoresByBusinessLineCode (da la diem quy doi gop dinh tinh + dinh luong). */
export interface RiskBranchScoreCombinedRowItem {
  branchCode: string;
  branchName: string | null;
  year: number;
  totalScore: number;
  rankLabel: string | null;
  scoresByBusinessLineCode: Record<string, number | null>;
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
  async rows(): Promise<RiskAssessmentOtherRowItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherRowItem[]>>(`${BASE}/assessment-other/rows`);
    return res.data.data;
  },
  async lines(headerId: string): Promise<RiskAssessmentOtherLineItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherLineItem[]>>(`${BASE}/assessment-other/${headerId}/lines`);
    return res.data.data;
  },
  async updateLine(headerId: string, lineId: string, request: RiskAssessmentOtherLineRequest): Promise<RiskAssessmentOtherLineItem> {
    const res = await httpClient.put<ApiResponse<RiskAssessmentOtherLineItem>>(`${BASE}/assessment-other/${headerId}/lines/${lineId}`, request);
    return res.data.data;
  },
  async deleteLine(headerId: string, lineId: string): Promise<void> {
    await httpClient.delete(`${BASE}/assessment-other/${headerId}/lines/${lineId}`);
  },
};

/** "Bang xep hang cham diem rui ro khac" (sheet ZTC_BXHRR_KHAC) - man hinh CHI XEM, tinh dong theo 1 nam. */
export const riskAssessmentOtherRankingApi = {
  async list(year: number): Promise<RiskAssessmentOtherRankingItem[]> {
    const res = await httpClient.get<ApiResponse<RiskAssessmentOtherRankingItem[]>>(`${BASE}/assessment-other-ranking`, { params: { year } });
    return res.data.data;
  },
};

/** "Ket qua cham diem rui ro theo chi nhanh" (sheet CT_Diem_DL/DT/All) - man hinh CHI XEM, tinh
 * dong theo 1 nam tu du lieu Ho so rui ro. */
export const riskBranchScoreApi = {
  async listQuantitative(year: number): Promise<RiskBranchScoreQuantitativeRowItem[]> {
    const res = await httpClient.get<ApiResponse<RiskBranchScoreQuantitativeRowItem[]>>(`${BASE}/branch-score/quantitative`, { params: { year } });
    return res.data.data;
  },
  async listQualitative(year: number): Promise<RiskBranchScoreQualitativeRowItem[]> {
    const res = await httpClient.get<ApiResponse<RiskBranchScoreQualitativeRowItem[]>>(`${BASE}/branch-score/qualitative`, { params: { year } });
    return res.data.data;
  },
  async listCombined(year: number): Promise<RiskBranchScoreCombinedRowItem[]> {
    const res = await httpClient.get<ApiResponse<RiskBranchScoreCombinedRowItem[]>>(`${BASE}/branch-score/combined`, { params: { year } });
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

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
async function downloadHsrrFile(path: string, year: number, kind: "excel" | "word", fileBaseName: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/${path}/export/${kind}`, { params: { year }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? `${fileBaseName}.xlsx` : `${fileBaseName}.docx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

/** "Ho so rui ro dinh luong" (sheet ZTC_HSRR, mau DL_HSRR_Upload) - wide-format, co phan quyen theo user/chi tieu. */
export const riskCriteriaQuantitativeValueApi = {
  async list(year: number): Promise<RiskCriteriaQuantitativeValueItem[]> {
    const res = await httpClient.get<ApiResponse<RiskCriteriaQuantitativeValueItem[]>>(`${BASE}/hsrr/quantitative`, { params: { year } });
    return res.data.data;
  },
  async create(request: RiskCriteriaQuantitativeValueRequest): Promise<RiskCriteriaQuantitativeValueItem> {
    const res = await httpClient.post<ApiResponse<RiskCriteriaQuantitativeValueItem>>(`${BASE}/hsrr/quantitative`, request);
    return res.data.data;
  },
  async update(id: string, request: RiskCriteriaQuantitativeValueRequest): Promise<RiskCriteriaQuantitativeValueItem> {
    const res = await httpClient.put<ApiResponse<RiskCriteriaQuantitativeValueItem>>(`${BASE}/hsrr/quantitative/${id}`, request);
    return res.data.data;
  },
  async remove(id: string): Promise<void> {
    await httpClient.delete(`${BASE}/hsrr/quantitative/${id}`);
  },
  async importExcel(file: File): Promise<ImportResult> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/hsrr/quantitative/import`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data.data;
  },
  exportFile: (year: number, kind: "excel" | "word") => downloadHsrrFile("quantitative", year, kind, "risk_score_criteria_quantitative_value"),
  async listWide(year: number): Promise<RiskCriteriaQuantitativeWideRowItem[]> {
    const res = await httpClient.get<ApiResponse<RiskCriteriaQuantitativeWideRowItem[]>>(`${BASE}/hsrr/quantitative/wide`, { params: { year } });
    return res.data.data;
  },
  async saveWideRow(request: RiskCriteriaQuantitativeWideRowRequest): Promise<RiskCriteriaQuantitativeWideRowItem> {
    const res = await httpClient.put<ApiResponse<RiskCriteriaQuantitativeWideRowItem>>(`${BASE}/hsrr/quantitative/wide`, request);
    return res.data.data;
  },
};

/** "Ho so rui ro dinh tinh" (sheet ZTC_HSRR, mau DT_HSRR_Upload) - long-format. */
export const riskCriteriaQualitativeValueApi = {
  async list(year: number): Promise<RiskCriteriaQualitativeValueItem[]> {
    const res = await httpClient.get<ApiResponse<RiskCriteriaQualitativeValueItem[]>>(`${BASE}/hsrr/qualitative`, { params: { year } });
    return res.data.data;
  },
  async create(request: RiskCriteriaQualitativeValueRequest): Promise<RiskCriteriaQualitativeValueItem> {
    const res = await httpClient.post<ApiResponse<RiskCriteriaQualitativeValueItem>>(`${BASE}/hsrr/qualitative`, request);
    return res.data.data;
  },
  async update(id: string, request: RiskCriteriaQualitativeValueRequest): Promise<RiskCriteriaQualitativeValueItem> {
    const res = await httpClient.put<ApiResponse<RiskCriteriaQualitativeValueItem>>(`${BASE}/hsrr/qualitative/${id}`, request);
    return res.data.data;
  },
  async remove(id: string): Promise<void> {
    await httpClient.delete(`${BASE}/hsrr/qualitative/${id}`);
  },
  async importExcel(file: File): Promise<ImportResult> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/hsrr/qualitative/import`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data.data;
  },
  exportFile: (year: number, kind: "excel" | "word") => downloadHsrrFile("qualitative", year, kind, "risk_score_criteria_qualitative_value"),
};
