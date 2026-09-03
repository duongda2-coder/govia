import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, Modal, Result, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditAppendix,
  deleteAuditAppendix,
  exportAuditAppendices,
  importAuditAppendices,
  listAuditAppendices,
  updateAuditAppendix,
  type AuditAppendixItem,
  type AuditAppendixRequest,
} from "../../../api/auditAppendix";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  sampleType: string;
  appendixCode: string;
  note?: string;
  active: boolean;
}

/** Danh muc "Quan ly phu luc" (sheet ZTC_phuluc) - trong nhom "Danh muc" cua module Kiem toan noi bo. */
export function AppendixPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.APPENDIX.VIEW");
  const canCreate = hasPermission("AUDIT.APPENDIX.CREATE");
  const canEdit = hasPermission("AUDIT.APPENDIX.EDIT");
  const canDelete = hasPermission("AUDIT.APPENDIX.DELETE");
  const canExport = hasPermission("AUDIT.APPENDIX.EXPORT");
  const canImport = hasPermission("AUDIT.APPENDIX.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditAppendixItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditAppendixItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditAppendixItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditAppendixItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList] = await Promise.all([listAuditAppendices(), listMasterDataItems("BUSINESS_SEGMENT")]);
      setItems(list);
      setBusinessSegments(segmentList);
    } catch {
      message.error(t("auditAppendix.messages.loadError"));
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
      businessSegmentId: target.businessSegmentId ?? undefined,
      sampleType: target.sampleType,
      appendixCode: target.appendixCode,
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
      const request: AuditAppendixRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        sampleType: values.sampleType,
        appendixCode: values.appendixCode,
        note: values.note ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditAppendix(editing.id, request);
        message.success(t("auditAppendix.messages.updateSuccess"));
      } else {
        await createAuditAppendix(request);
        message.success(t("auditAppendix.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditAppendix.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditAppendix.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditAppendix(item.id)));
          message.success(t("auditAppendix.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditAppendix.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditAppendixItem>["columns"] = [
    { title: t("auditAppendix.columns.appendixCode"), width: 160, ...getSearchColumnProps("appendixCode", searchLabels) },
    { title: t("auditAppendix.columns.sampleType"), width: 180, ...getSearchColumnProps("sampleType", searchLabels) },
    {
      title: t("auditAppendix.columns.businessSegment"),
      width: 180,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditAppendix.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 130,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditAppendix.title")}</Typography.Title>
      <CrudTable<AuditAppendixItem>
        tableId="audit.appendix"
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
        onExportExcel={canExport ? () => exportAuditAppendices("excel") : undefined}
        onExportWord={canExport ? () => exportAuditAppendices("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditAppendices(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditAppendix.form.editTitle") : t("auditAppendix.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="businessSegmentId" label={t("auditAppendix.columns.businessSegment")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
            />
          </Form.Item>
          <Form.Item name="sampleType" label={t("auditAppendix.columns.sampleType")} rules={[{ required: true }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="appendixCode" label={t("auditAppendix.columns.appendixCode")} rules={[{ required: true }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="note" label={t("auditAppendix.columns.note")}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
