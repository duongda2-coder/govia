import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditBranchStaff,
  deleteAuditBranchStaff,
  exportAuditBranchStaff,
  importAuditBranchStaff,
  listAuditBranchStaff,
  updateAuditBranchStaff,
  type AuditBranchStaffItem,
  type AuditBranchStaffRequest,
} from "../../../api/auditBranchStaff";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  staffName: string;
  position?: string;
  priority?: number;
  note?: string;
  active: boolean;
}

const PRIORITIES = [1, 2, 3, 4, 5];

/** Danh muc "Chuc danh can bo chi nhanh" (sheet ZTC_CN_NV) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function BranchStaffPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.BRANCH_STAFF.VIEW");
  const canCreate = hasPermission("AUDIT.BRANCH_STAFF.CREATE");
  const canEdit = hasPermission("AUDIT.BRANCH_STAFF.EDIT");
  const canDelete = hasPermission("AUDIT.BRANCH_STAFF.DELETE");
  const canExport = hasPermission("AUDIT.BRANCH_STAFF.EXPORT");
  const canImport = hasPermission("AUDIT.BRANCH_STAFF.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditBranchStaffItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditBranchStaffItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditBranchStaffItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditBranchStaffItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditBranchStaff());
    } catch {
      message.error(t("auditBranchStaff.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ active: true });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      branchCode: target.branchCode,
      staffName: target.staffName,
      position: target.position ?? undefined,
      priority: target.priority ?? undefined,
      note: target.note ?? undefined,
      active: target.active,
    });
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
      const request: AuditBranchStaffRequest = {
        branchCode: values.branchCode,
        staffName: values.staffName,
        position: values.position ?? null,
        priority: values.priority ?? null,
        note: values.note ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditBranchStaff(editing.id, request);
        message.success(t("auditBranchStaff.messages.updateSuccess"));
      } else {
        await createAuditBranchStaff(request);
        message.success(t("auditBranchStaff.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditBranchStaff.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditBranchStaff.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditBranchStaff(item.id)));
          message.success(t("auditBranchStaff.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditBranchStaff.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditBranchStaffItem>["columns"] = [
    { title: t("auditBranchStaff.columns.branchCode"), width: 120, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditBranchStaff.columns.staffName"), ...getSearchColumnProps("staffName", searchLabels) },
    { title: t("auditBranchStaff.columns.position"), dataIndex: "position", render: (v: string | null) => v ?? "-" },
    { title: t("auditBranchStaff.columns.priority"), dataIndex: "priority", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditBranchStaff.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 110,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditBranchStaff.title")}</Typography.Title>
      <CrudTable<AuditBranchStaffItem>
        tableId="audit.plan.branchStaff"
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
        onExportExcel={canExport ? () => exportAuditBranchStaff("excel") : undefined}
        onExportWord={canExport ? () => exportAuditBranchStaff("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditBranchStaff(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditBranchStaff.form.editTitle") : t("auditBranchStaff.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="branchCode" label={t("auditBranchStaff.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="staffName" label={t("auditBranchStaff.columns.staffName")} rules={[{ required: true }]}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="position" label={t("auditBranchStaff.columns.position")}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="priority" label={t("auditBranchStaff.columns.priority")}>
                <Select allowClear options={PRIORITIES.map((v) => ({ value: v, label: v }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="note" label={t("auditBranchStaff.columns.note")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
