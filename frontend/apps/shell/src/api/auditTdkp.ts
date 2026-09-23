import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AssignmentApprovalStatus } from "./auditWorkManagement";

/** Nhóm màn hình "Theo dõi khắc phục" (TDKP) - file 6.TDKP_29.5.2026.xlsx: phân công, kiến nghị HĐTV/TGĐ, chi nhánh, nghị quyết, đơn vị, báo cáo. */
const BASE = "/api/audit/tdkp";

export type TdkpStatus = "DONE" | "IN_PROGRESS" | "NOT_STARTED";
export type TdkpTarget = "HDTV" | "TGD";
export type TdkpDeadlineState = "ON_TIME" | "OVERDUE";

// ===================== helpers =====================

async function get<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  const res = await httpClient.get<ApiResponse<T>>(`${BASE}${path}`, { params });
  return res.data.data;
}

async function send<T>(method: "post" | "put", path: string, body?: unknown, params?: Record<string, unknown>): Promise<T> {
  const res = await httpClient.request<ApiResponse<T>>({ method, url: `${BASE}${path}`, data: body, params });
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

async function download(path: string, filename: string, options?: { method?: "get" | "post"; params?: Record<string, unknown>; body?: unknown }): Promise<void> {
  const res = await httpClient.request({
    method: options?.method ?? "get",
    url: `${BASE}${path}`,
    params: options?.params,
    data: options?.body,
    responseType: "blob",
  });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}

// ===================== lookups (list dùng chung) =====================

export interface TdkpOption {
  id: string;
  code: string;
  name: string;
}

export interface TdkpUnitOption extends TdkpOption {
  unitType: string;
}

export interface TdkpEmployeeOption {
  id: string;
  code: string;
  name: string;
  username: string | null;
  departmentName: string | null;
}

export interface TdkpLookups {
  recommendationTypes: TdkpOption[];
  businessSegments: TdkpOption[];
  geographicAreas: TdkpOption[];
  units: TdkpUnitOption[];
  employees: TdkpEmployeeOption[];
}

export const getTdkpLookups = () => get<TdkpLookups>("/lookups");

// ===================== sheet 1: phân công =====================

export type TdkpAssignmentScope = "CEO" | "BRANCH";

export interface TdkpAssignmentItem {
  id: string;
  scope: TdkpAssignmentScope;
  recommendationTypeId: string | null;
  recommendationTypeName: string | null;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  targetObject: TdkpTarget | null;
  targetObjectLabel: string | null;
  auditObjectUnitId: string | null;
  auditObjectUnitCode: string | null;
  auditObjectUnitName: string | null;
  geographicAreaId: string | null;
  geographicAreaName: string | null;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  username: string | null;
  departmentName: string | null;
  startDate: string | null;
  endDate: string | null;
}

export interface TdkpAssignmentRequest {
  scope: TdkpAssignmentScope;
  recommendationTypeId: string | null;
  businessSegmentId: string | null;
  targetObject: TdkpTarget | null;
  auditObjectUnitId: string | null;
  geographicAreaId: string | null;
  employeeId: string;
  startDate: string | null;
  endDate: string | null;
}

export const listTdkpAssignments = (scope: TdkpAssignmentScope) => get<TdkpAssignmentItem[]>("/assignment", { scope });
export const createTdkpAssignment = (request: TdkpAssignmentRequest) => send<TdkpAssignmentItem>("post", "/assignment", request);
export const updateTdkpAssignment = (id: string, request: TdkpAssignmentRequest) => send<TdkpAssignmentItem>("put", `/assignment/${id}`, request);
export const deleteTdkpAssignment = (id: string) => remove(`/assignment/${id}`);
export const exportTdkpAssignments = (scope: TdkpAssignmentScope) =>
  download("/assignment/export/excel", `tdkp_phan_cong_${scope.toLowerCase()}.xlsx`, { params: { scope } });

// ===================== sheet 2/3: kiến nghị HĐTV/TGĐ =====================

export type TdkpCeoVariant = "all" | "kh";

export interface TdkpCeoItem {
  id: string;
  scope: "ALL" | "KH";
  sourceId: string | null;
  reportNumber: string | null;
  reportDate: string | null;
  content: string;
  recommendationTypeId: string | null;
  recommendationTypeName: string | null;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  targetObject: TdkpTarget | null;
  targetObjectLabel: string | null;
  executingUnitId: string | null;
  executingUnitName: string | null;
  directive: string | null;
  deadline: string | null;
  status: TdkpStatus | null;
  statusLabel: string | null;
  evaluation: string | null;
  deadlineState: TdkpDeadlineState | null;
  deadlineStateLabel: string | null;
  lastEditedDate: string | null;
  lastEditedBy: string | null;
  note: string | null;
}

export interface TdkpCeoRequest {
  reportNumber: string | null;
  reportDate: string | null;
  content: string;
  recommendationTypeId: string | null;
  businessSegmentId: string | null;
  targetObject: TdkpTarget | null;
  executingUnitId: string | null;
  directive: string | null;
  deadline: string | null;
  status: TdkpStatus | null;
  evaluation: string | null;
  note: string | null;
}

const ceoPath = (variant: TdkpCeoVariant) => (variant === "all" ? "/ceo-all" : "/ceo-kh");

export const listTdkpCeo = (variant: TdkpCeoVariant) => get<TdkpCeoItem[]>(ceoPath(variant));
export const createTdkpCeo = (variant: TdkpCeoVariant, request: TdkpCeoRequest) => send<TdkpCeoItem>("post", ceoPath(variant), request);
export const updateTdkpCeo = (variant: TdkpCeoVariant, id: string, request: TdkpCeoRequest) => send<TdkpCeoItem>("put", `${ceoPath(variant)}/${id}`, request);
export const deleteTdkpCeo = (variant: TdkpCeoVariant, id: string) => remove(`${ceoPath(variant)}/${id}`);
export const exportTdkpCeo = (variant: TdkpCeoVariant) => download(`${ceoPath(variant)}/export/excel`, `tdkp_ceo_${variant}.xlsx`);
export const importTdkpCeo = (variant: TdkpCeoVariant, file: File) => importFile(`${ceoPath(variant)}/import`, file);
export const transferTdkpCeoFromAll = () => send<{ transferred: number; skipped: number }>("post", "/ceo-kh/transfer-from-all");
export const transferTdkpCeoAllFromReportIssuance = () =>
  send<{ transferred: number; skipped: number }>("post", "/ceo-all/transfer-from-report-issuance");

// ===================== sheet 4: chi nhánh =====================

export interface TdkpBranchRecommendationItem {
  id: string;
  managementCode: string;
  auditObjectUnitId: string;
  branchName: string | null;
  branchCode: string | null;
  auditYear: number | null;
  engagementId: string | null;
  content: string;
  recommendationTypeId: string | null;
  recommendationTypeName: string | null;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  deadline: string | null;
  editContent: string | null;
  status: TdkpStatus | null;
  statusLabel: string | null;
  evaluation: string | null;
  deadlineState: TdkpDeadlineState | null;
  deadlineStateLabel: string | null;
  lastEditedDate: string | null;
  note: string | null;
  defectCount: number;
  defectDoneCount: number;
}

export interface TdkpBranchRecommendationRequest {
  auditObjectUnitId: string;
  auditYear: number | null;
  content: string;
  recommendationTypeId: string | null;
  businessSegmentId: string | null;
  deadline: string | null;
  editContent: string | null;
  status: TdkpStatus | null;
  evaluation: string | null;
  note: string | null;
}

export interface TdkpBranchDefectItem {
  id: string;
  branchRecommendationId: string;
  managementCode: string;
  defectContent: string | null;
  customerEntry: string | null;
  creditContract: string | null;
  defectCode: string | null;
  defectType: string | null;
  recommendationDefectStatus: TdkpStatus | null;
  recommendationDefectStatusLabel: string | null;
  customerStatus: TdkpStatus | null;
  customerStatusLabel: string | null;
  relatedStaff: string | null;
}

export interface TdkpBranchDefectRequest {
  branchRecommendationId: string;
  defectContent: string | null;
  customerEntry: string | null;
  creditContract: string | null;
  defectCode: string | null;
  defectType: string | null;
  customerStatus: TdkpStatus | null;
  relatedStaff: string | null;
}

export const listTdkpBranchRecommendations = () => get<TdkpBranchRecommendationItem[]>("/branch/recommendations");
export const createTdkpBranchRecommendation = (request: TdkpBranchRecommendationRequest) =>
  send<TdkpBranchRecommendationItem>("post", "/branch/recommendations", request);
export const updateTdkpBranchRecommendation = (id: string, request: TdkpBranchRecommendationRequest) =>
  send<TdkpBranchRecommendationItem>("put", `/branch/recommendations/${id}`, request);
export const deleteTdkpBranchRecommendation = (id: string) => remove(`/branch/recommendations/${id}`);
export const exportTdkpBranchRecommendations = () => download("/branch/recommendations/export/excel", "tdkp_cn_kien_nghi.xlsx");
export const importTdkpBranchRecommendations = (file: File) => importFile("/branch/recommendations/import", file);
export const transferTdkpBranchFromExecution = (year?: number) =>
  send<{ recommendationsCreated: number; defectsCreated: number; skipped: number }>("post", "/branch/transfer", undefined, year ? { year } : undefined);

export const listTdkpBranchDefects = (recommendationId?: string) => get<TdkpBranchDefectItem[]>("/branch/defects", recommendationId ? { recommendationId } : undefined);
export const createTdkpBranchDefect = (request: TdkpBranchDefectRequest) => send<TdkpBranchDefectItem>("post", "/branch/defects", request);
export const updateTdkpBranchDefect = (id: string, request: TdkpBranchDefectRequest) => send<TdkpBranchDefectItem>("put", `/branch/defects/${id}`, request);
export const deleteTdkpBranchDefect = (id: string) => remove(`/branch/defects/${id}`);
export const exportTdkpBranchDefects = () => download("/branch/defects/export/excel", "tdkp_cn_sai_sot.xlsx");
export const listTdkpBranchStaffOptions = (recommendationId: string) => get<string[]>(`/branch/recommendations/${recommendationId}/staff-options`);

// ===================== sheet 5: nghị quyết HĐTV =====================

export interface TdkpResolutionItem {
  id: string;
  code: string;
  resolutionNumber: string;
  issueDate: string | null;
  content: string | null;
  workDetail: string | null;
  fieldArea: string | null;
  unitId: string | null;
  unitCode: string | null;
  unitName: string | null;
  contactPerson: string | null;
  relatedResolution: string | null;
  completionDeadline: string | null;
  completionDeadlineBasis: string | null;
  implementation: string | null;
  progressStatus: TdkpStatus | null;
  progressStatusLabel: string | null;
  reason: string | null;
  issuanceEvaluation: string | null;
  completionDate: string | null;
  resolutionState: TdkpDeadlineState | null;
  resolutionStateLabel: string | null;
  followUpGroup: string | null;
  followUpGroupLabel: string | null;
  followerName: string | null;
  proposal: string | null;
  proposalReason: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
  approvalStatusLabel: string | null;
}

export interface TdkpResolutionRequest {
  resolutionNumber: string;
  issueDate: string | null;
  content: string | null;
  workDetail: string | null;
  fieldArea: string | null;
  unitId: string | null;
  contactPerson: string | null;
  relatedResolution: string | null;
  completionDeadline: string | null;
  completionDeadlineBasis: string | null;
  implementation: string | null;
  progressStatus: TdkpStatus | null;
  reason: string | null;
  issuanceEvaluation: string | null;
  completionDate: string | null;
  followUpGroup: string | null;
  followerName: string | null;
  proposal: string | null;
  proposalReason: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
}

export const listTdkpResolutions = () => get<TdkpResolutionItem[]>("/resolution");
export const createTdkpResolution = (request: TdkpResolutionRequest) => send<TdkpResolutionItem>("post", "/resolution", request);
export const updateTdkpResolution = (id: string, request: TdkpResolutionRequest) => send<TdkpResolutionItem>("put", `/resolution/${id}`, request);
export const deleteTdkpResolution = (id: string) => remove(`/resolution/${id}`);
export const exportTdkpResolutions = () => download("/resolution/export/excel", "tdkp_nghi_quyet.xlsx");
export const importTdkpResolutions = (file: File) => importFile("/resolution/import", file);

// ===================== sheet 6: kiến nghị của đơn vị đối với KTNB =====================

export interface TdkpUnitRecommendationItem {
  id: string;
  code: string;
  reportNumber: string | null;
  reportDate: string | null;
  unitId: string | null;
  unitCode: string | null;
  unitName: string | null;
  recommendationTarget: string | null;
  content: string;
  deadline: string | null;
  implementation: string | null;
  status: TdkpStatus | null;
  statusLabel: string | null;
  evaluation: string | null;
  lastEditedDate: string | null;
  deadlineState: TdkpDeadlineState | null;
  deadlineStateLabel: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
  approvalStatusLabel: string | null;
}

export interface TdkpUnitRecommendationRequest {
  reportNumber: string | null;
  reportDate: string | null;
  unitId: string | null;
  recommendationTarget: string | null;
  content: string;
  deadline: string | null;
  implementation: string | null;
  status: TdkpStatus | null;
  evaluation: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
}

export const listTdkpUnitRecommendations = () => get<TdkpUnitRecommendationItem[]>("/unit-recommendation");
export const createTdkpUnitRecommendation = (request: TdkpUnitRecommendationRequest) =>
  send<TdkpUnitRecommendationItem>("post", "/unit-recommendation", request);
export const updateTdkpUnitRecommendation = (id: string, request: TdkpUnitRecommendationRequest) =>
  send<TdkpUnitRecommendationItem>("put", `/unit-recommendation/${id}`, request);
export const deleteTdkpUnitRecommendation = (id: string) => remove(`/unit-recommendation/${id}`);
export const exportTdkpUnitRecommendations = () => download("/unit-recommendation/export/excel", "tdkp_kien_nghi_don_vi.xlsx");
export const importTdkpUnitRecommendations = (file: File) => importFile("/unit-recommendation/import", file);

// ===================== sheet 7-12: báo cáo =====================

export type TdkpReportType = "BC01" | "BC02" | "BC03" | "BC04" | "BC05";

export interface TdkpReportRequest {
  type: TdkpReportType;
  asOfDate: string | null;
  year: number | null;
  branchUnitId: string | null;
  attachedReportNumber: string | null;
  attachedReportDate: string | null;
  minutesDate: string | null;
  decisionNumber: string | null;
  decisionDate: string | null;
}

export interface TdkpReportData {
  type: TdkpReportType;
  asOfDate: string;
  subtitle: string | null;
  heading: string | null;
  headers: string[];
  rows: string[][];
  totals: string[] | null;
  stats: string[][] | null;
}

export interface TdkpReportArchiveItem {
  id: string;
  reportType: TdkpReportType;
  reportTypeTitle: string;
  title: string;
  asOfDate: string | null;
  note: string | null;
  createdBy: string | null;
  createdAt: string | null;
  attachmentCount: number;
}

export interface TdkpReportArchiveRequest {
  reportType: TdkpReportType;
  title: string;
  asOfDate: string | null;
  note: string | null;
}

export const previewTdkpReport = (request: TdkpReportRequest) => send<TdkpReportData>("post", "/report/preview", request);
export const exportTdkpReport = (request: TdkpReportRequest) =>
  download("/report/export", `tdkp_${request.type.toLowerCase()}.xlsx`, { method: "post", body: request });
export const listTdkpReportArchive = () => get<TdkpReportArchiveItem[]>("/report/archive");
export const createTdkpReportArchive = (request: TdkpReportArchiveRequest) => send<TdkpReportArchiveItem>("post", "/report/archive", request);
export const updateTdkpReportArchive = (id: string, request: TdkpReportArchiveRequest) => send<TdkpReportArchiveItem>("put", `/report/archive/${id}`, request);
export const deleteTdkpReportArchive = (id: string) => remove(`/report/archive/${id}`);
