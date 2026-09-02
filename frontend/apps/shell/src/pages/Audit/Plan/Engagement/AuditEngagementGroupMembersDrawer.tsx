import { useCallback, useEffect, useState } from "react";
import { App, Col, Drawer, Form, Modal, Row, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  addMember,
  deleteMember,
  listMembers,
  updateMember,
  type AuditEngagementGroupItem,
  type AuditEngagementGroupMemberItem,
} from "../../../../api/auditEngagementTeam";
import type { EmployeeOption } from "../../../../api/auditEngagement";
import type { MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  employeeId: string;
  businessSegment1Id?: string;
  businessSegment2Id?: string;
  businessSegment3Id?: string;
}

export interface AuditEngagementGroupMembersDrawerProps {
  open: boolean;
  engagementId: string;
  group: AuditEngagementGroupItem | null;
  employees: EmployeeOption[];
  businessSegments: MasterDataItem[];
  onClose: () => void;
  onChanged: () => void;
}

/** Man hinh "Danh sach thanh vien trong nhom" (nut "Xem thanh vien nhom" o Drawer nhom). */
export function AuditEngagementGroupMembersDrawer(props: AuditEngagementGroupMembersDrawerProps) {
  const { open, engagementId, group, employees, businessSegments, onClose, onChanged } = props;
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE");
  const canEdit = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.EDIT");
  const canDelete = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE");

  const [items, setItems] = useState<AuditEngagementGroupMemberItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementGroupMemberItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditEngagementGroupMemberItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    if (!group) return;
    setLoading(true);
    try {
      setItems(await listMembers(engagementId, group.id));
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, group, message, t]);

  useEffect(() => {
    if (open && group) load();
    if (!open) {
      setItems([]);
      setSelected([]);
    }
  }, [open, group, load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      employeeId: target.employeeId,
      businessSegment1Id: target.businessSegment1Id ?? undefined,
      businessSegment2Id: target.businessSegment2Id ?? undefined,
      businessSegment3Id: target.businessSegment3Id ?? undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!group) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request = {
        employeeId: values.employeeId,
        businessSegment1Id: values.businessSegment1Id ?? null,
        businessSegment2Id: values.businessSegment2Id ?? null,
        businessSegment3Id: values.businessSegment3Id ?? null,
      };
      if (editing) {
        await updateMember(engagementId, group.id, editing.id, request);
        message.success(t("auditEngagement.messages.updateSuccess"));
      } else {
        await addMember(engagementId, group.id, request);
        message.success(t("auditEngagement.messages.memberAddSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
      onChanged();
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (!group || selected.length === 0) return;
    modal.confirm({
      title: t("auditEngagement.deleteMemberConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteMember(engagementId, group.id, item.id)));
          message.success(t("common.deleteSuccess"));
          setSelected([]);
          await load();
          onChanged();
        } catch {
          message.error(t("auditEngagement.messages.saveError"));
        }
      },
    });
  };

  const segmentOptions = (excludeIds: (string | undefined)[]) =>
    businessSegments.filter((s) => !excludeIds.includes(s.id)).map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }));

  const columns: TableProps<AuditEngagementGroupMemberItem>["columns"] = [
    { title: t("auditEngagement.form.memberEmployeeCode"), dataIndex: "employeeCode", width: 110 },
    { title: t("auditEngagement.form.memberEmployeeName"), dataIndex: "employeeName" },
    { title: t("auditEngagement.form.memberDepartment"), dataIndex: "department", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.form.memberUsername"), dataIndex: "username", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.form.segment1"), dataIndex: "businessSegment1Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.form.segment2"), dataIndex: "businessSegment2Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.form.segment3"), dataIndex: "businessSegment3Code", width: 100, render: (v: string | null) => v ?? "-" },
  ];

  return (
    <Drawer
      title={group ? t("auditEngagement.form.membersTitle", { groupName: group.groupName }) : ""}
      open={open}
      onClose={onClose}
      width={900}
      destroyOnClose
    >
      {group && (
        <>
          <Typography.Paragraph type="secondary">
            {t("auditEngagement.form.groupLeaderLabel")}: {group.leaderEmployeeName} ({group.leaderEmployeeCode})
          </Typography.Paragraph>
          <CrudTable<AuditEngagementGroupMemberItem>
            tableId="audit.planEngagement.groupMembers"
            columns={columns}
            dataSource={items}
            rowKey="id"
            loading={loading}
            onAdd={canCreate ? openCreate : undefined}
            onEdit={canEdit ? openEdit : undefined}
            editDisabled={selected.length !== 1}
            onDelete={canDelete ? handleDelete : undefined}
            deleteDisabled={selected.length === 0}
            onSelectionChange={(_keys, rows) => setSelected(rows)}
          />
        </>
      )}

      <Modal
        title={editing ? t("auditEngagement.form.changeMemberTitle") : t("auditEngagement.form.addMemberTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="employeeId" label={t("auditEngagement.form.memberUserId")} rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={employees.map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` }))} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.businessSegment2Id !== cur.businessSegment2Id || prev.businessSegment3Id !== cur.businessSegment3Id}>
                {() => (
                  <Form.Item name="businessSegment1Id" label={t("auditEngagement.form.segment1")}>
                    <Select allowClear options={segmentOptions([form.getFieldValue("businessSegment2Id"), form.getFieldValue("businessSegment3Id")])} />
                  </Form.Item>
                )}
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.businessSegment1Id !== cur.businessSegment1Id || prev.businessSegment3Id !== cur.businessSegment3Id}>
                {() => (
                  <Form.Item name="businessSegment2Id" label={t("auditEngagement.form.segment2")}>
                    <Select allowClear options={segmentOptions([form.getFieldValue("businessSegment1Id"), form.getFieldValue("businessSegment3Id")])} />
                  </Form.Item>
                )}
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.businessSegment1Id !== cur.businessSegment1Id || prev.businessSegment2Id !== cur.businessSegment2Id}>
                {() => (
                  <Form.Item name="businessSegment3Id" label={t("auditEngagement.form.segment3")}>
                    <Select allowClear options={segmentOptions([form.getFieldValue("businessSegment1Id"), form.getFieldValue("businessSegment2Id")])} />
                  </Form.Item>
                )}
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </Drawer>
  );
}
