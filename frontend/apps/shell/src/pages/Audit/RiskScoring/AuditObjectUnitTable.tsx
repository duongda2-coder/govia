import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  auditObjectUnitApi,
  AUDIT_UNIT_TYPE_OPTIONS,
  type AuditObjectUnitItem,
  type AuditObjectUnitRequest,
  type AuditUnitType,
} from "../../../api/riskScoring";
import { groupHOApi, type GroupHOItem } from "../../../api/riskScoringExec";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  code: string;
  name: string;
  unitType: AuditUnitType;
  establishedDate?: dayjs.Dayjs;
  restructureDate?: dayjs.Dayjs;
  restructureNote?: string;
  totalStaff?: number;
  leaderCount?: number;
  staffCount?: number;
  rankValue?: number;
  defenseLineGroupId?: string;
  operatingRegulation?: string;
  mainFunction?: string;
  keyFindings?: string;
  active: boolean;
}

/** Danh muc "Doi tuong kiem toan - Don vi" (sheet ZTC_DTKT1: HO/Giam sat CC/Chi nhanh). */
export function AuditObjectUnitTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditObjectUnitItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditObjectUnitItem[]>([]);
  const [groupHOOptions, setGroupHOOptions] = useState<GroupHOItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditObjectUnitItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditObjectUnitItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, groupHOList] = await Promise.all([auditObjectUnitApi.list(), groupHOApi.list()]);
      setItems(list);
      setGroupHOOptions(groupHOList);
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
      code: target.code,
      name: target.name,
      unitType: target.unitType,
      establishedDate: target.establishedDate ? dayjs(target.establishedDate) : undefined,
      restructureDate: target.restructureDate ? dayjs(target.restructureDate) : undefined,
      restructureNote: target.restructureNote ?? undefined,
      totalStaff: target.totalStaff ?? undefined,
      leaderCount: target.leaderCount ?? undefined,
      staffCount: target.staffCount ?? undefined,
      rankValue: target.rankValue ?? undefined,
      defenseLineGroupId: target.defenseLineGroupId ?? undefined,
      operatingRegulation: target.operatingRegulation ?? undefined,
      mainFunction: target.mainFunction ?? undefined,
      keyFindings: target.keyFindings ?? undefined,
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
      const request: AuditObjectUnitRequest = {
        code: values.code,
        name: values.name,
        unitType: values.unitType,
        establishedDate: values.establishedDate ? values.establishedDate.format("YYYY-MM-DD") : null,
        restructureDate: values.restructureDate ? values.restructureDate.format("YYYY-MM-DD") : null,
        restructureNote: values.restructureNote ?? null,
        totalStaff: values.totalStaff ?? null,
        leaderCount: values.leaderCount ?? null,
        staffCount: values.staffCount ?? null,
        rankValue: values.rankValue ?? null,
        defenseLineGroupId: values.defenseLineGroupId ?? null,
        operatingRegulation: values.operatingRegulation ?? null,
        mainFunction: values.mainFunction ?? null,
        keyFindings: values.keyFindings ?? null,
        active: values.active,
      };
      if (editing) {
        await auditObjectUnitApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await auditObjectUnitApi.create(request);
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
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await auditObjectUnitApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditObjectUnitItem>["columns"] = [
    { title: t("riskScoring.columns.code"), width: 100, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("riskScoring.columns.unitType"),
      dataIndex: "unitType",
      width: 140,
      render: (v: AuditUnitType) => AUDIT_UNIT_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v,
    },
    { title: t("riskScoring.columns.totalStaff"), dataIndex: "totalStaff", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.leaderCount"), dataIndex: "leaderCount", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.staffCount"), dataIndex: "staffCount", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.rankValue"), dataIndex: "rankValue", width: 100, render: (v: number | null) => v ?? "-" },
    {
      title: t("riskScoring.columns.defenseLineGroup"),
      dataIndex: "defenseLineGroupCode",
      width: 130,
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoring.columns.infoUpdatedDate"),
      dataIndex: "infoUpdatedDate",
      width: 130,
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
      <CrudTable<AuditObjectUnitItem>
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => auditObjectUnitApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => auditObjectUnitApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await auditObjectUnitApi.importExcel(file);
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
        width={680}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
            <Input maxLength={10} />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoring.columns.name")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="unitType" label={t("riskScoring.columns.unitType")} rules={[{ required: true }]}>
            <Select options={AUDIT_UNIT_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="establishedDate" label={t("riskScoring.columns.establishedDate")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="restructureDate" label={t("riskScoring.columns.restructureDate")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="restructureNote" label={t("riskScoring.columns.restructureNote")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="totalStaff" label={t("riskScoring.columns.totalStaff")}>
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>
          <Form.Item name="leaderCount" label={t("riskScoring.columns.leaderCount")}>
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>
          <Form.Item name="staffCount" label={t("riskScoring.columns.staffCount")}>
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>
          <Form.Item name="rankValue" label={t("riskScoring.columns.rankValue")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="defenseLineGroupId" label={t("riskScoring.columns.defenseLineGroup")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={groupHOOptions.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))}
            />
          </Form.Item>
          <Form.Item name="operatingRegulation" label={t("riskScoring.columns.operatingRegulation")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="mainFunction" label={t("riskScoring.columns.mainFunction")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="keyFindings" label={t("riskScoring.columns.keyFindings")}>
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
