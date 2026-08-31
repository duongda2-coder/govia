import { useCallback, useEffect, useState } from "react";
import { App, Badge, Button, DatePicker, Form, Input, Modal, Select, Switch } from "antd";
import { PaperClipOutlined } from "@ant-design/icons";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, fetchAttachmentCounts, useClientSearchColumn } from "@govia/ui-kit";
import { auditFindingApi, type AuditFindingItem, type AuditFindingRequest } from "../../../api/auditFinding";
import { auditObjectUnitApi, type AuditObjectUnitItem } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { httpClient } from "../../../api/client";
import { useAuth } from "../../../auth/AuthContext";
import { AuditFindingAttachmentDrawer } from "./AuditFindingAttachmentDrawer";

const ATTACHMENT_ENTITY = "AUDIT_FINDING";

interface FormValues {
  branchCode: string;
  title: string;
  description?: string;
  severity: string;
  detectedDate: dayjs.Dayjs;
  active: boolean;
}

/** "Phat hien kiem toan" gan voi 1 chi nhanh - nguon du lieu that cho tool "get_audit_findings" va
 * (qua dinh kem evidence) "get_evidence" cua AI Agent (xem docs/audit-tools-contract.md). */
export function AuditFindingTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.FINDING.VIEW");
  const canCreate = hasPermission("AUDIT.FINDING.CREATE");
  const canEdit = hasPermission("AUDIT.FINDING.EDIT");
  const canDelete = hasPermission("AUDIT.FINDING.DELETE");
  const { getSearchColumnProps } = useClientSearchColumn<AuditFindingItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditFindingItem[]>([]);
  const [branches, setBranches] = useState<AuditObjectUnitItem[]>([]);
  const [severities, setSeverities] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditFindingItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditFindingItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [attachmentCounts, setAttachmentCounts] = useState<Record<string, number>>({});
  const [attachmentTarget, setAttachmentTarget] = useState<AuditFindingItem | null>(null);
  const [form] = Form.useForm<FormValues>();

  const branchOptions = branches.map((b) => ({ value: b.code, label: `${b.code} - ${b.name}` }));
  const severityOptions = severities.map((s) => ({ value: s.code, label: s.name }));

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await auditFindingApi.list();
      setItems(list);
      setAttachmentCounts(await fetchAttachmentCounts(httpClient, ATTACHMENT_ENTITY, list.map((i) => i.id)));
    } catch {
      message.error(t("riskScoringExec.finding.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (!canView) return;
    load();
    auditObjectUnitApi.list().then(setBranches).catch(() => setBranches([]));
    listMasterDataItems("RISK_LEVEL").then(setSeverities).catch(() => setSeverities([]));
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
      title: target.title,
      description: target.description ?? undefined,
      severity: target.severity,
      detectedDate: dayjs(target.detectedDate),
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
      const request: AuditFindingRequest = {
        branchCode: values.branchCode,
        title: values.title,
        description: values.description || null,
        severity: values.severity,
        detectedDate: values.detectedDate.format("YYYY-MM-DD"),
        active: values.active,
      };
      if (editing) {
        await auditFindingApi.update(editing.id, request);
        message.success(t("riskScoringExec.finding.messages.updateSuccess"));
      } else {
        await auditFindingApi.create(request);
        message.success(t("riskScoringExec.finding.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoringExec.finding.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("riskScoringExec.finding.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => auditFindingApi.remove(item.id)));
          message.success(t("riskScoringExec.finding.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.finding.messages.deleteError"));
        }
      },
    });
  };

  const handleAttachmentClick = () => {
    const target = selected[0];
    if (!target) return;
    setAttachmentTarget(target);
  };

  const columns: TableProps<AuditFindingItem>["columns"] = [
    { title: t("riskScoringExec.hsrr.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    {
      title: t("riskScoringExec.hsrr.branchName"),
      width: 200,
      ...getSearchColumnProps("branchName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.finding.title"), ...getSearchColumnProps("title", searchLabels) },
    {
      title: t("riskScoringExec.finding.severity"),
      dataIndex: "severityName",
      width: 130,
      render: (v: string | null, record) => v ?? record.severity,
    },
    { title: t("riskScoringExec.finding.detectedDate"), dataIndex: "detectedDate", width: 130 },
    {
      title: t("common.attachment"),
      key: "attachments",
      width: 100,
      align: "center",
      render: (_, record) => {
        const count = attachmentCounts[record.id] ?? 0;
        return (
          <Button type="text" size="small" onClick={() => setAttachmentTarget(record)}>
            <Badge count={count} size="small" showZero color={count > 0 ? "#2563eb" : "#d9d9d9"} offset={[6, -1]}>
              <PaperClipOutlined style={{ fontSize: 16 }} />
            </Badge>
          </Button>
        );
      },
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
      <CrudTable<AuditFindingItem>
        tableId="riskScoringExec.finding"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onAttachment={handleAttachmentClick}
        attachmentDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <Modal
        title={editing ? t("riskScoringExec.finding.editTitle") : t("riskScoringExec.finding.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="branchCode" label={t("riskScoringExec.hsrr.branchCode")} rules={[{ required: true }]}>
            <Select options={branchOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="title" label={t("riskScoringExec.finding.title")} rules={[{ required: true }, { max: 500 }]}>
            <Input maxLength={500} />
          </Form.Item>
          <Form.Item name="description" label={t("riskScoringExec.finding.description")}>
            <Input.TextArea rows={3} maxLength={2000} />
          </Form.Item>
          <Form.Item name="severity" label={t("riskScoringExec.finding.severity")} rules={[{ required: true }]}>
            <Select options={severityOptions} />
          </Form.Item>
          <Form.Item name="detectedDate" label={t("riskScoringExec.finding.detectedDate")} rules={[{ required: true }]}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <AuditFindingAttachmentDrawer
        open={attachmentTarget != null}
        finding={attachmentTarget}
        onClose={() => setAttachmentTarget(null)}
        onCountChange={(findingId, count) => setAttachmentCounts((prev) => ({ ...prev, [findingId]: count }))}
      />
    </div>
  );
}
