import { useCallback, useEffect, useState } from "react";
import { App, Form, Modal, Result, Select, Switch, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import type { ApprovalMatrixRule } from "../../api/approvalMatrix";
import {
  createApprovalMatrixRule,
  deleteApprovalMatrixRule,
  listApprovalMatrixRules,
  updateApprovalMatrixRule,
} from "../../api/approvalMatrix";
import type { EmployeeRankLevel } from "../../api/employees";
import { listOrgUnits, type OrganizationUnit } from "../../api/orgUnits";
import { useAuth } from "../../auth/AuthContext";

const RANK_LEVELS: EmployeeRankLevel[] = ["N1", "N2", "N3", "N4", "N5", "N6"];

interface FormValues {
  orgUnitId?: string;
  finalApprovalLevel: EmployeeRankLevel;
  requireFinalSuperAdminStep: boolean;
  active: boolean;
}

export function ApprovalMatrixPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("WORKFLOW.APPROVAL_MATRIX.VIEW");
  const canManage = hasPermission("WORKFLOW.APPROVAL_MATRIX.MANAGE");

  const [rules, setRules] = useState<ApprovalMatrixRule[]>([]);
  const [orgUnits, setOrgUnits] = useState<OrganizationUnit[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<ApprovalMatrixRule[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ApprovalMatrixRule | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [ruleList, unitList] = await Promise.all([listApprovalMatrixRules(), listOrgUnits()]);
      setRules(ruleList);
      setOrgUnits(unitList);
    } catch {
      message.error(t("workflow.approvalMatrix.messages.loadError"));
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
    form.setFieldsValue({ requireFinalSuperAdminStep: true, active: true });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      orgUnitId: target.orgUnitId ?? undefined,
      finalApprovalLevel: target.finalApprovalLevel,
      requireFinalSuperAdminStep: target.requireFinalSuperAdminStep,
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
      const request = {
        orgUnitId: values.orgUnitId || null,
        finalApprovalLevel: values.finalApprovalLevel,
        requireFinalSuperAdminStep: values.requireFinalSuperAdminStep,
        active: values.active,
      };
      if (editing) {
        await updateApprovalMatrixRule(editing.id, request);
        message.success(t("workflow.approvalMatrix.messages.updateSuccess"));
      } else {
        await createApprovalMatrixRule(request);
        message.success(t("workflow.approvalMatrix.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("workflow.approvalMatrix.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("workflow.approvalMatrix.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteApprovalMatrixRule(target.id);
          message.success(t("workflow.approvalMatrix.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("workflow.approvalMatrix.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<ApprovalMatrixRule>["columns"] = [
    {
      title: t("workflow.approvalMatrix.columns.scope"),
      key: "scope",
      render: (_: unknown, record) =>
        record.orgUnitId ? (
          `${record.orgUnitCode} - ${record.orgUnitName}`
        ) : (
          <Tag color="blue">{t("workflow.approvalMatrix.defaultScope")}</Tag>
        ),
    },
    {
      title: t("workflow.approvalMatrix.columns.finalApprovalLevel"),
      dataIndex: "finalApprovalLevel",
      width: 160,
    },
    {
      title: t("workflow.approvalMatrix.columns.active"),
      dataIndex: "active",
      width: 120,
      render: (v: boolean) => <Tag color={v ? "green" : "default"}>{v ? t("common.active") : t("common.inactive")}</Tag>,
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("workflow.approvalMatrix.title")}</Typography.Title>
      <Typography.Paragraph type="secondary">{t("workflow.approvalMatrix.description")}</Typography.Paragraph>

      <CrudTable<ApprovalMatrixRule>
        columns={columns}
        dataSource={rules}
        rowKey="id"
        loading={loading}
        onAdd={canManage ? openCreate : undefined}
        onEdit={canManage ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canManage ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <Modal
        title={editing ? t("workflow.approvalMatrix.form.editTitle") : t("workflow.approvalMatrix.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="orgUnitId" label={t("workflow.approvalMatrix.form.orgUnit")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder={t("workflow.approvalMatrix.form.orgUnitPlaceholder")}
              options={orgUnits.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
            />
          </Form.Item>
          <Form.Item
            name="finalApprovalLevel"
            label={t("workflow.approvalMatrix.form.finalApprovalLevel")}
            rules={[{ required: true }]}
          >
            <Select options={RANK_LEVELS.map((level) => ({ value: level, label: level }))} />
          </Form.Item>
          <Form.Item
            name="requireFinalSuperAdminStep"
            label={t("workflow.approvalMatrix.form.requireFinalStep")}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
