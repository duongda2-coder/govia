import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type EmployeeStatus = "ACTIVE" | "ON_LEAVE" | "TERMINATED" | "PENDING_APPROVAL" | "REJECTED";
export type Gender = "MALE" | "FEMALE" | "OTHER";
export type EmployeeRankLevel = "N1" | "N2" | "N3" | "N4" | "N5" | "N6";
export type EmployeeEducationLevel =
  | "DOCTOR_OF_SCIENCE"
  | "DOCTORATE"
  | "MASTER"
  | "BACHELOR"
  | "ENGINEER"
  | "COLLEGE"
  | "INTERMEDIATE"
  | "ELEMENTARY"
  | "HIGH_SCHOOL"
  | "SECONDARY_SCHOOL";
export type EmployeePoliticalLevel = "ELEMENTARY" | "INTERMEDIATE" | "ADVANCED";
export type EmployeeAuditorClassification = "TYPE_1" | "TYPE_2" | "TYPE_3";

export interface Employee {
  id: string;
  employeeCode: string;
  fullName: string;
  email: string | null;
  personalEmail: string | null;
  phone: string | null;
  orgUnitId: string | null;
  orgUnitCode: string | null;
  orgUnitName: string | null;
  positionId: string | null;
  positionCode: string | null;
  positionName: string | null;
  hireDate: string | null;
  status: EmployeeStatus;
  dateOfBirth: string | null;
  gender: Gender | null;
  idNumber: string | null;
  managerId: string | null;
  managerCode: string | null;
  managerName: string | null;
  rankLevel: EmployeeRankLevel | null;
  ethnicity: string | null;
  hometown: string | null;
  partyJoinDate: string | null;
  auditDeptJoinDate: string | null;
  priorWorkHistory: string | null;
  educationLevel: EmployeeEducationLevel | null;
  politicalLevel: EmployeePoliticalLevel | null;
  foreignLanguageLevel: string | null;
  itSkillLevel: string | null;
  auditorClassification: EmployeeAuditorClassification | null;
  teamLeadCapable: boolean;
  auditedBranches: string | null;
  otherDuties: string | null;
  relatedPersonBranches: string | null;
  onLeave: boolean;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  /** Username tai khoan dang nhap gan voi nhan vien nay - null neu chua co tai khoan. */
  username: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface EmployeeRequest {
  employeeCode: string;
  fullName: string;
  email?: string | null;
  personalEmail?: string | null;
  phone?: string | null;
  orgUnitId?: string | null;
  positionId?: string | null;
  hireDate?: string | null;
  dateOfBirth?: string | null;
  gender?: Gender | null;
  idNumber?: string | null;
  managerId?: string | null;
  rankLevel?: EmployeeRankLevel | null;
  ethnicity?: string | null;
  hometown?: string | null;
  partyJoinDate?: string | null;
  auditDeptJoinDate?: string | null;
  priorWorkHistory?: string | null;
  educationLevel?: EmployeeEducationLevel | null;
  politicalLevel?: EmployeePoliticalLevel | null;
  foreignLanguageLevel?: string | null;
  itSkillLevel?: string | null;
  auditorClassification?: EmployeeAuditorClassification | null;
  teamLeadCapable: boolean;
  auditedBranches?: string | null;
  otherDuties?: string | null;
  relatedPersonBranches?: string | null;
  onLeave: boolean;
  businessSegmentId?: string | null;
}

export interface CreateUserAccountRequest {
  username: string;
  password: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface EmployeeListParams {
  orgUnitId?: string;
  status?: EmployeeStatus;
  keyword?: string;
  employeeCode?: string;
  fullName?: string;
  positionName?: string;
  phone?: string;
  email?: string;
  orgUnitName?: string;
  managerName?: string;
  page?: number;
  size?: number;
  /** Dinh dang Spring Pageable: "field,asc" hoac "field,desc" (vd "orgUnit.name,asc"). */
  sort?: string;
}

export async function listEmployees(params: EmployeeListParams): Promise<PageResponse<Employee>> {
  const res = await httpClient.get<ApiResponse<PageResponse<Employee>>>("/api/employees", { params });
  return res.data.data;
}

export async function createEmployee(request: EmployeeRequest): Promise<Employee> {
  const res = await httpClient.post<ApiResponse<Employee>>("/api/employees", request);
  return res.data.data;
}

export async function updateEmployee(id: string, request: EmployeeRequest): Promise<Employee> {
  const res = await httpClient.put<ApiResponse<Employee>>(`/api/employees/${id}`, request);
  return res.data.data;
}

export async function changeEmployeeStatus(id: string, status: EmployeeStatus): Promise<Employee> {
  const res = await httpClient.patch<ApiResponse<Employee>>(`/api/employees/${id}/status`, { status });
  return res.data.data;
}

export async function deleteEmployee(id: string): Promise<void> {
  await httpClient.delete(`/api/employees/${id}`);
}

export async function createEmployeeAccount(id: string, request: CreateUserAccountRequest): Promise<void> {
  await httpClient.post(`/api/employees/${id}/account`, request);
}

/** Chi SUPER_ADMIN goi duoc - dung khi nhan vien quen mat khau va nho admin dat lai ho. */
export async function resetEmployeeAccountPassword(id: string, newPassword: string): Promise<void> {
  await httpClient.patch(`/api/employees/${id}/account/reset-password`, { newPassword });
}

/** Import Excel dung DUNG mau da xuat (xem exportEmployees) - moi dong loi duoc tra ve rieng, khong lam hong ca file. */
export async function importEmployees(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>("/api/employees/import", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportEmployees(
  kind: "excel" | "word",
  params: Omit<EmployeeListParams, "page" | "size">,
): Promise<void> {
  const res = await httpClient.get(`/api/employees/export/${kind}`, { params, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "employees.xlsx" : "employees.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
