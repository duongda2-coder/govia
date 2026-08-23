import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface ProcessDefinitionSummary {
  id: string;
  key: string;
  name: string | null;
  version: number;
}

export interface ProcessInstanceSummary {
  id: string;
  processDefinitionKey: string;
  businessKey: string | null;
  startTime: string;
  endTime: string | null;
}

export interface TaskSummary {
  id: string;
  name: string;
  processInstanceId: string;
  processDefinitionId: string;
  businessKey: string | null;
  assignee: string | null;
  owner: string | null;
  /** null (chưa uỷ quyền lần nào), "PENDING" (đang uỷ quyền), "RESOLVED" (người được uỷ quyền đã xong). */
  delegationState: "PENDING" | "RESOLVED" | null;
  parentTaskId: string | null;
  createTime: string;
  dueDate: string | null;
}

export interface StartProcessRequest {
  processDefinitionKey: string;
  businessKey?: string | null;
  variables?: Record<string, unknown>;
}

export async function listProcessDefinitions(): Promise<ProcessDefinitionSummary[]> {
  const res = await httpClient.get<ApiResponse<ProcessDefinitionSummary[]>>("/api/workflow/process-definitions");
  return res.data.data;
}

/** Trien khai 1 file BPMN moi (dinh dang .bpmn20.xml/.bpmn) - danh cho man hinh quan tri. */
export async function deployProcessDefinition(file: File): Promise<ProcessDefinitionSummary> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ProcessDefinitionSummary>>(
    "/api/workflow/process-definitions/deploy",
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return res.data.data;
}

export async function startProcessInstance(request: StartProcessRequest): Promise<ProcessInstanceSummary> {
  const res = await httpClient.post<ApiResponse<ProcessInstanceSummary>>("/api/workflow/instances/start", request);
  return res.data.data;
}

export async function listProcessInstances(): Promise<ProcessInstanceSummary[]> {
  const res = await httpClient.get<ApiResponse<ProcessInstanceSummary[]>>("/api/workflow/instances");
  return res.data.data;
}

export async function cancelProcessInstance(id: string, reason?: string): Promise<void> {
  await httpClient.post(`/api/workflow/instances/${id}/cancel`, null, reason ? { params: { reason } } : undefined);
}

/** Task dang giao cho chinh nguoi dang dang nhap ("Viec cua toi"). */
export async function listMyTasks(): Promise<TaskSummary[]> {
  const res = await httpClient.get<ApiResponse<TaskSummary[]>>("/api/workflow/tasks/my");
  return res.data.data;
}

/** Toan bo task dang mo trong tenant - can quyen WORKFLOW.TASK.VIEW_ALL (giam sat/quan tri). */
export async function listAllTasks(): Promise<TaskSummary[]> {
  const res = await httpClient.get<ApiResponse<TaskSummary[]>>("/api/workflow/tasks");
  return res.data.data;
}

export async function claimTask(id: string): Promise<void> {
  await httpClient.post(`/api/workflow/tasks/${id}/claim`);
}

export async function completeTask(id: string, variables?: Record<string, unknown>): Promise<void> {
  await httpClient.post(`/api/workflow/tasks/${id}/complete`, { variables });
}

/** Chuyển tiếp (forward): giao hẳn task cho người khác, không quay lại người cũ. */
export async function reassignTask(id: string, assigneeUserId: string): Promise<void> {
  await httpClient.post(`/api/workflow/tasks/${id}/reassign`, { assigneeUserId });
}

/** Uỷ quyền: giao tạm thời - người được uỷ quyền phải gọi resolveTask khi xong, task quay lại người uỷ quyền. */
export async function delegateTask(id: string, delegateUserId: string): Promise<void> {
  await httpClient.post(`/api/workflow/tasks/${id}/delegate`, { delegateUserId });
}

/** Người được uỷ quyền báo đã xong phần của mình - task quay về người uỷ quyền để tự hoàn tất. */
export async function resolveTask(id: string, variables?: Record<string, unknown>): Promise<void> {
  await httpClient.post(`/api/workflow/tasks/${id}/resolve`, { variables });
}
