import type { ApiResponse } from "@govia/ui-kit";
import type { AuditWorkPhase } from "./auditWorkItem";
import { httpClient } from "./client";

export type AuditEngagementGroupCode = "DIEUHANH" | "NTINDUNG" | "TINDUNG";

export interface AuditEngagementGroupItem {
  id: string;
  auditEngagementId: string;
  engagementCode: string;
  groupCode: AuditEngagementGroupCode;
  groupName: string;
  leaderEmployeeId: string;
  leaderEmployeeCode: string | null;
  leaderEmployeeName: string | null;
  memberCount: number;
  workItemCount: number;
}

export interface AuditEngagementGroupMemberItem {
  id: string;
  groupId: string;
  groupCode: AuditEngagementGroupCode;
  groupName: string;
  auditEngagementId: string;
  engagementCode: string;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  department: string | null;
  username: string | null;
  leaderEmployeeId: string;
  leaderEmployeeName: string | null;
  businessSegment1Id: string | null;
  businessSegment1Code: string | null;
  businessSegment2Id: string | null;
  businessSegment2Code: string | null;
  businessSegment3Id: string | null;
  businessSegment3Code: string | null;
}

export interface AuditEngagementAssignmentItem {
  id: string;
  groupMemberId: string;
  groupId: string;
  groupName: string;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  workItemId: string;
  phase: AuditWorkPhase | null;
  businessSegmentCode: string | null;
  workItemCode: string | null;
  workItemName: string | null;
}

export interface EligibleWorkItem {
  id: string;
  phase: AuditWorkPhase | null;
  code: string;
  name: string;
}

const base = (engagementId: string) => `/api/audit/plan/engagement/${engagementId}`;

export async function listGroups(engagementId: string): Promise<AuditEngagementGroupItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementGroupItem[]>>(`${base(engagementId)}/groups`);
  return res.data.data;
}

export async function addGroup(engagementId: string, groupCode: AuditEngagementGroupCode, leaderEmployeeId: string): Promise<AuditEngagementGroupItem> {
  const res = await httpClient.post<ApiResponse<AuditEngagementGroupItem>>(`${base(engagementId)}/groups`, { groupCode, leaderEmployeeId });
  return res.data.data;
}

export async function deleteGroup(engagementId: string, groupId: string): Promise<void> {
  await httpClient.delete(`${base(engagementId)}/groups/${groupId}`);
}

export async function listMembersByEngagement(engagementId: string): Promise<AuditEngagementGroupMemberItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementGroupMemberItem[]>>(`${base(engagementId)}/members`);
  return res.data.data;
}

export async function listMembers(engagementId: string, groupId: string): Promise<AuditEngagementGroupMemberItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementGroupMemberItem[]>>(`${base(engagementId)}/groups/${groupId}/members`);
  return res.data.data;
}

export interface MemberRequest {
  employeeId: string;
  businessSegment1Id?: string | null;
  businessSegment2Id?: string | null;
  businessSegment3Id?: string | null;
}

export async function addMember(engagementId: string, groupId: string, request: MemberRequest): Promise<AuditEngagementGroupMemberItem> {
  const res = await httpClient.post<ApiResponse<AuditEngagementGroupMemberItem>>(`${base(engagementId)}/groups/${groupId}/members`, request);
  return res.data.data;
}

export async function updateMember(engagementId: string, groupId: string, memberId: string, request: MemberRequest): Promise<AuditEngagementGroupMemberItem> {
  const res = await httpClient.put<ApiResponse<AuditEngagementGroupMemberItem>>(`${base(engagementId)}/groups/${groupId}/members/${memberId}`, request);
  return res.data.data;
}

export async function deleteMember(engagementId: string, groupId: string, memberId: string): Promise<void> {
  await httpClient.delete(`${base(engagementId)}/groups/${groupId}/members/${memberId}`);
}

export async function listEligibleWorkItems(engagementId: string, groupId: string, memberId: string): Promise<EligibleWorkItem[]> {
  const res = await httpClient.get<ApiResponse<EligibleWorkItem[]>>(`${base(engagementId)}/groups/${groupId}/members/${memberId}/eligible-work-items`);
  return res.data.data;
}

export async function listAssignments(engagementId: string, groupId: string, memberId: string): Promise<AuditEngagementAssignmentItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementAssignmentItem[]>>(`${base(engagementId)}/groups/${groupId}/members/${memberId}/assignments`);
  return res.data.data;
}

export async function assignWorkItems(engagementId: string, groupId: string, memberId: string, workItemIds: string[]): Promise<AuditEngagementAssignmentItem[]> {
  const res = await httpClient.post<ApiResponse<AuditEngagementAssignmentItem[]>>(`${base(engagementId)}/groups/${groupId}/members/${memberId}/assignments`, { workItemIds });
  return res.data.data;
}

export async function deleteAssignment(engagementId: string, groupId: string, memberId: string, assignmentId: string): Promise<void> {
  await httpClient.delete(`${base(engagementId)}/groups/${groupId}/members/${memberId}/assignments/${assignmentId}`);
}
