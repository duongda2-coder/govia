import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface DocumentLibraryItem {
  id: string;
  documentNumber: string;
  documentName: string;
  issueDate: string | null;
  effectiveDate: string | null;
  issuerPositionId: string | null;
  issuerPositionName: string | null;
  businessActivity: string | null;
  topic: string | null;
  replacedDocument: string | null;
  amendedDocument: string | null;
  legalBasis: string | null;
  expired: boolean;
  expiryDate: string | null;
  content: string | null;
}

export interface DocumentLibraryRequest {
  documentNumber: string;
  documentName: string;
  issueDate: string | null;
  effectiveDate: string | null;
  issuerPositionId: string | null;
  businessActivity: string | null;
  topic: string | null;
  replacedDocument: string | null;
  amendedDocument: string | null;
  legalBasis: string | null;
  expired: boolean;
  expiryDate: string | null;
  content: string | null;
}

const BASE = "/api/audit/master-data/document-library";

export async function listDocumentLibrary(): Promise<DocumentLibraryItem[]> {
  const res = await httpClient.get<ApiResponse<DocumentLibraryItem[]>>(BASE);
  return res.data.data;
}

export async function createDocumentLibrary(request: DocumentLibraryRequest): Promise<DocumentLibraryItem> {
  const res = await httpClient.post<ApiResponse<DocumentLibraryItem>>(BASE, request);
  return res.data.data;
}

export async function updateDocumentLibrary(id: string, request: DocumentLibraryRequest): Promise<DocumentLibraryItem> {
  const res = await httpClient.put<ApiResponse<DocumentLibraryItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteDocumentLibrary(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importDocumentLibrary(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportDocumentLibrary(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_document_library.xlsx" : "audit_document_library.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
