import { useCallback, useEffect, useState } from "react";
import { App, Button, Drawer, Form, Modal, Select } from "antd";
import type { TableProps } from "antd";
import { TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  addGroup,
  deleteGroup,
  listGroups,
  type AuditEngagementGroupCode,
  type AuditEngagementGroupItem,
} from "../../../../api/auditEngagementTeam";
import type { EmployeeOption } from "../../../../api/auditEngagement";
import type { MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditEngagementGroupMembersDrawer } from "./AuditEngagementGroupMembersDrawer";

const ALL_GROUP_CODES: AuditEngagementGroupCode[] = ["DIEUHANH", "NTINDUNG", "TINDUNG"];

interface FormValues {
  groupCode: AuditEngagementGroupCode;
  leaderEmployeeId: string;
}

export interface AuditEngagementGroupsDrawerProps {
  open: boolean;
  engagementId: string;
  engagementCode: string;
  employees: EmployeeOption[];
  businessSegments: MasterDataItem[];
  onClose: () => void;
}

/** Man hinh "Danh sach nhom cua dot kiem toan" (nut "Danh sach nhom" o man hinh danh sach CKT). */
export function AuditEngagementGroupsDrawer(props: AuditEngagementGroupsDrawerProps) {
  const { open, engagementId, engagementCode, employees, businessSegments, onClose } = props;
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.CREATE");
  const canDelete = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.DELETE");

  const [items, setItems] = useState<AuditEngagementGroupItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementGroupItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const [membersGroup, setMembersGroup] = useState<AuditEngagementGroupItem | null>(null);
  const [membersOpen, setMembersOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listGroups(engagementId));
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, message, t]);

  useEffect(() => {
    if (open) load();
    if (!open) {
      setItems([]);
      setSelected([]);
    }
  }, [open, load]);

  const usedCodes = new Set(items.map((g) => g.groupCode));
  const groupLeaderCapableEmployees = employees.filter((e) => e.truongNhomCapable);

  const openCreate = () => {
    form.resetFields();
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await addGroup(engagementId, values.groupCode, values.leaderEmployeeId);
      message.success(t("auditEngagement.messages.groupAddSuccess"));
      setModalOpen(false);
      await load();
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("auditEngagement.deleteGroupConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteGroup(engagementId, item.id)));
          message.success(t("common.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditEngagement.messages.groupDeleteError"));
        }
      },
    });
  };

  const openMembers = (group: AuditEngagementGroupItem) => {
    setMembersGroup(group);
    setMembersOpen(true);
  };

  const columns: TableProps<AuditEngagementGroupItem>["columns"] = [
    { title: t("auditEngagement.form.groupCode"), dataIndex: "groupCode", width: 120 },
    { title: t("auditEngagement.form.groupName"), dataIndex: "groupName", width: 160 },
    { title: t("auditEngagement.form.groupLeaderCode"), dataIndex: "leaderEmployeeCode", width: 110 },
    { title: t("auditEngagement.form.groupLeaderName"), dataIndex: "leaderEmployeeName" },
    { title: t("auditEngagement.form.groupLeaderUsername"), dataIndex: "leaderUsername", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.form.memberCount"), dataIndex: "memberCount", width: 110 },
    { title: t("auditEngagement.form.workItemCount"), dataIndex: "workItemCount", width: 140 },
    {
      title: "",
      key: "actions",
      width: 160,
      render: (_v, row) => (
        <Button size="small" icon={<TeamOutlined />} onClick={() => openMembers(row)}>
          {t("auditEngagement.form.viewMembers")}
        </Button>
      ),
    },
  ];

  return (
    <>
      <Drawer title={t("auditEngagement.form.groupsTitle", { code: engagementCode })} open={open} onClose={onClose} width={960} destroyOnClose>
        <CrudTable<AuditEngagementGroupItem>
          tableId="audit.planEngagement.groups"
          columns={columns}
          dataSource={items}
          rowKey="id"
          loading={loading}
          onAdd={canCreate && usedCodes.size < 3 ? openCreate : undefined}
          onDelete={canDelete ? handleDelete : undefined}
          deleteDisabled={selected.length === 0}
          onSelectionChange={(_keys, rows) => setSelected(rows)}
        />

        <Modal
          title={t("auditEngagement.form.addGroupTitle")}
          open={modalOpen}
          onCancel={() => setModalOpen(false)}
          onOk={handleSubmit}
          confirmLoading={submitting}
          destroyOnClose
          width={480}
        >
          <Form<FormValues> form={form} layout="vertical">
            <Form.Item name="groupCode" label={t("auditEngagement.form.groupName")} rules={[{ required: true }]}>
              <Select options={ALL_GROUP_CODES.filter((c) => !usedCodes.has(c)).map((c) => ({ value: c, label: c }))} />
            </Form.Item>
            <Form.Item name="leaderEmployeeId" label={t("auditEngagement.form.groupLeaderName")} rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={groupLeaderCapableEmployees.map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` }))}
              />
            </Form.Item>
          </Form>
        </Modal>
      </Drawer>

      <AuditEngagementGroupMembersDrawer
        open={membersOpen}
        engagementId={engagementId}
        group={membersGroup}
        employees={employees}
        businessSegments={businessSegments}
        onClose={() => setMembersOpen(false)}
        onChanged={load}
      />
    </>
  );
}
