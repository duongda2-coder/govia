import { useCallback, useEffect, useState } from "react";
import { App, Alert, Button, Checkbox, Col, Modal, Row, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  assignWorkItems,
  deleteAssignment,
  listAssignments,
  listEligibleWorkItems,
  listMembersByEngagement,
  type AuditEngagementAssignmentItem,
  type AuditEngagementGroupMemberItem,
  type EligibleWorkItem,
} from "../../../../api/auditEngagementTeam";
import type { AuditEngagementItem } from "../../../../api/auditEngagement";
import { useAuth } from "../../../../auth/AuthContext";

export interface AuditEngagementAssignmentPageProps {
  engagement: AuditEngagementItem;
  onBack: () => void;
}

/** Man hinh "Phan cong nghiep vu cho thanh vien" - chi truong doan cua CKT moi duoc thao tac. */
export function AuditEngagementAssignmentPage(props: AuditEngagementAssignmentPageProps) {
  const { engagement, onBack } = props;
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { user, hasPermission } = useAuth();
  const canCreate = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE");
  const canDelete = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE");
  const isTeamLead = !!user?.employeeCode && user.employeeCode === engagement.teamLeadEmployeeCode;

  const [members, setMembers] = useState<AuditEngagementGroupMemberItem[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [selectedMember, setSelectedMember] = useState<AuditEngagementGroupMemberItem | null>(null);

  const [assignments, setAssignments] = useState<AuditEngagementAssignmentItem[]>([]);
  const [loadingAssignments, setLoadingAssignments] = useState(false);

  const [pickerOpen, setPickerOpen] = useState(false);
  const [eligible, setEligible] = useState<EligibleWorkItem[]>([]);
  const [pickedIds, setPickedIds] = useState<string[]>([]);
  const [picking, setPicking] = useState(false);

  const loadMembers = useCallback(async () => {
    setLoadingMembers(true);
    try {
      setMembers(await listMembersByEngagement(engagement.id));
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
    } finally {
      setLoadingMembers(false);
    }
  }, [engagement.id, message, t]);

  useEffect(() => {
    loadMembers();
  }, [loadMembers]);

  const loadAssignments = useCallback(
    async (member: AuditEngagementGroupMemberItem) => {
      setLoadingAssignments(true);
      try {
        setAssignments(await listAssignments(engagement.id, member.groupId, member.id));
      } catch {
        message.error(t("auditEngagement.messages.loadError"));
      } finally {
        setLoadingAssignments(false);
      }
    },
    [engagement.id, message, t],
  );

  useEffect(() => {
    if (selectedMember) loadAssignments(selectedMember);
    else setAssignments([]);
  }, [selectedMember, loadAssignments]);

  const openPicker = async () => {
    if (!selectedMember) return;
    setPickerOpen(true);
    setPickedIds([]);
    try {
      setEligible(await listEligibleWorkItems(engagement.id, selectedMember.groupId, selectedMember.id));
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
    }
  };

  const handlePick = async () => {
    if (!selectedMember || pickedIds.length === 0) return;
    setPicking(true);
    try {
      await assignWorkItems(engagement.id, selectedMember.groupId, selectedMember.id, pickedIds);
      message.success(t("auditEngagement.messages.assignSuccess"));
      setPickerOpen(false);
      await loadAssignments(selectedMember);
      await loadMembers();
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    } finally {
      setPicking(false);
    }
  };

  const handleUnassign = (assignment: AuditEngagementAssignmentItem) => {
    if (!selectedMember) return;
    deleteAssignment(engagement.id, selectedMember.groupId, selectedMember.id, assignment.id)
      .then(() => {
        message.success(t("common.deleteSuccess"));
        loadAssignments(selectedMember);
        loadMembers();
      })
      .catch(() => message.error(t("auditEngagement.messages.saveError")));
  };

  const memberColumns: TableProps<AuditEngagementGroupMemberItem>["columns"] = [
    { title: t("auditEngagement.form.groupCode"), dataIndex: "groupCode", width: 110 },
    { title: t("auditEngagement.form.memberEmployeeCode"), dataIndex: "employeeCode", width: 110 },
    { title: t("auditEngagement.form.memberEmployeeName"), dataIndex: "employeeName" },
    { title: t("auditEngagement.form.memberUsername"), dataIndex: "username", width: 120, render: (v: string | null) => v ?? "-" },
  ];

  const assignmentColumns: TableProps<AuditEngagementAssignmentItem>["columns"] = [
    { title: t("auditWorkItem.columns.phase"), dataIndex: "phase", width: 100, render: (v: string | null) => (v ? t(`auditWorkItem.phase.${v}`) : "-") },
    { title: t("auditEngagement.form.groupCode"), dataIndex: "groupName", width: 120 },
    { title: t("auditEngagement.form.assignmentSegment"), dataIndex: "businessSegmentCode", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditWorkItem.columns.code"), dataIndex: "workItemCode", width: 120 },
    { title: t("auditWorkItem.columns.name"), dataIndex: "workItemName" },
    ...(canDelete
      ? [
          {
            title: "",
            key: "actions",
            width: 80,
            render: (_v: unknown, row: AuditEngagementAssignmentItem) => (
              <Button size="small" danger type="text" onClick={() => handleUnassign(row)}>
                {t("common.delete")}
              </Button>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditEngagement.form.assignmentTitle", { code: engagement.code })}
          </Typography.Title>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>

      {!isTeamLead && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={t("auditEngagement.form.notTeamLeadWarning", { name: engagement.teamLeadEmployeeName })}
        />
      )}

      <Typography.Paragraph type="secondary">
        {t("auditEngagement.columns.auditObjectUnit")}: {engagement.auditObjectUnitCode} - {engagement.auditObjectUnitName} ·{" "}
        {t("auditEngagement.columns.teamLeadEmployee")}: {engagement.teamLeadEmployeeName} · {t("auditEngagement.columns.decisionNumber")}:{" "}
        {engagement.decisionNumber}
      </Typography.Paragraph>

      <Typography.Title level={5}>{t("auditEngagement.form.membersSectionTitle")}</Typography.Title>
      <CrudTable<AuditEngagementGroupMemberItem>
        tableId="audit.planEngagement.assignmentMembers"
        columns={memberColumns}
        dataSource={members}
        rowKey="id"
        loading={loadingMembers}
        onSelectionChange={(_keys, rows) => setSelectedMember(rows[0] ?? null)}
      />

      <Space style={{ marginTop: 24, marginBottom: 8 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          {t("auditEngagement.form.assignmentSectionTitle")}
        </Typography.Title>
        {canCreate && isTeamLead && (
          <Button type="primary" disabled={!selectedMember} onClick={openPicker}>
            {t("auditEngagement.form.chooseWorkItems")}
          </Button>
        )}
      </Space>
      <CrudTable<AuditEngagementAssignmentItem>
        tableId="audit.planEngagement.assignments"
        columns={assignmentColumns}
        dataSource={assignments}
        rowKey="id"
        loading={loadingAssignments}
      />

      <Modal
        title={t("auditEngagement.form.chooseWorkItems")}
        open={pickerOpen}
        onCancel={() => setPickerOpen(false)}
        onOk={handlePick}
        confirmLoading={picking}
        okButtonProps={{ disabled: pickedIds.length === 0 }}
        destroyOnClose
        width={640}
      >
        <Checkbox.Group style={{ width: "100%" }} value={pickedIds} onChange={(v) => setPickedIds(v as string[])}>
          <Space direction="vertical" style={{ width: "100%", maxHeight: 400, overflowY: "auto" }}>
            {eligible.map((w) => (
              <Checkbox key={w.id} value={w.id}>
                {w.code} - {w.name}
              </Checkbox>
            ))}
            {eligible.length === 0 && <Typography.Text type="secondary">{t("auditEngagement.form.noEligibleWorkItems")}</Typography.Text>}
          </Space>
        </Checkbox.Group>
      </Modal>
    </div>
  );
}
