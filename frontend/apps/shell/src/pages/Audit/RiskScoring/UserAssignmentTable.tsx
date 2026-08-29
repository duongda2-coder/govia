import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  criteriaQuantitativeApi,
  userAssignmentApi,
  type CriteriaQuantitativeItem,
  type UserAssignmentItem,
  type UserAssignmentRequest,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  username: string;
  criteriaId: string;
  branchCode?: string;
  classification?: string;
  active: boolean;
}

/** Danh muc "Phan quyen User theo chi tieu dinh luong" (sheet ZTC_HSRR_DL_User). */
export function UserAssignmentTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<UserAssignmentItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<UserAssignmentItem[]>([]);
  const [criteriaOptions, setCriteriaOptions] = useState<CriteriaQuantitativeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<UserAssignmentItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<UserAssignmentItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, criteriaList] = await Promise.all([userAssignmentApi.list(), criteriaQuantitativeApi.list()]);
      setItems(list);
      setCriteriaOptions(criteriaList);
    } catch {
      message.error(t("riskScoring.messages.loadError"));
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
      username: target.username,
      criteriaId: target.criteriaId,
      branchCode: target.branchCode ?? undefined,
      classification: target.classification ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      username: "",
      criteriaId: target.criteriaId,
      branchCode: target.branchCode ?? undefined,
      classification: target.classification ?? undefined,
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
      const request: UserAssignmentRequest = {
        username: values.username,
        criteriaId: values.criteriaId,
        branchCode: values.branchCode ?? null,
        classification: values.classification ?? null,
        active: values.active,
      };
      if (editing) {
        await userAssignmentApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await userAssignmentApi.create(request);
        message.success(t("riskScoring.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoring.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => userAssignmentApi.remove(item.id)));
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<UserAssignmentItem>["columns"] = [
    { title: t("riskScoring.columns.username"), width: 150, ...getSearchColumnProps("username", searchLabels) },
    {
      title: t("riskScoring.columns.criteria"),
      width: 130,
      ...getSearchColumnProps("criteriaCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoring.columns.branchCode"),
      width: 110,
      ...getSearchColumnProps("branchCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoring.columns.classification"),
      ...getSearchColumnProps("classification", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 110,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <CrudTable<UserAssignmentItem>
        tableId="riskScoring.userAssignment"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onCopy={canCreate ? openCopy : undefined}
        copyDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => userAssignmentApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => userAssignmentApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await userAssignmentApi.importExcel(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("riskScoring.form.editTitle") : t("riskScoring.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="username" label={t("riskScoring.columns.username")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="criteriaId" label={t("riskScoring.columns.criteria")} rules={[{ required: true }]}>
            <Select options={criteriaOptions.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` }))} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="branchCode" label={t("riskScoring.columns.branchCode")}>
            <Input />
          </Form.Item>
          <Form.Item name="classification" label={t("riskScoring.columns.classification")}>
            <Input />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
