import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditProcessStepSummary,
  deleteAuditProcessStepSummary,
  exportAuditProcessStepSummaries,
  importAuditProcessStepSummaries,
  listAuditProcessStepSummaries,
  updateAuditProcessStepSummary,
  type AuditProcessStepSummaryItem,
  type AuditProcessStepSummaryRequest,
} from "../../../api/auditProcessStep";
import { listAuditWorkItems, type AuditWorkItemItem } from "../../../api/auditWorkItem";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  code: string;
  name: string;
  workItemId?: string;
  active: boolean;
}

/** Danh muc "Buoc quy trinh tong hop" (sheet ZTB_BQT_TH) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function ProcessStepSummaryPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.VIEW");
  const canCreate = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.CREATE");
  const canEdit = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.EDIT");
  const canDelete = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.DELETE");
  const canExport = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.EXPORT");
  const canImport = hasPermission("AUDIT.PROCESS_STEP_SUMMARY.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditProcessStepSummaryItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditProcessStepSummaryItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [workItems, setWorkItems] = useState<AuditWorkItemItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessStepSummaryItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditProcessStepSummaryItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, workItemList] = await Promise.all([
        listAuditProcessStepSummaries(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditWorkItems(),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setWorkItems(workItemList);
    } catch {
      message.error(t("auditProcessStepSummary.messages.loadError"));
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
      code: target.code,
      name: target.name,
      workItemId: target.workItemId ?? undefined,
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
      const request: AuditProcessStepSummaryRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        name: values.name,
        workItemId: values.workItemId ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditProcessStepSummary(editing.id, request);
        message.success(t("auditProcessStepSummary.messages.updateSuccess"));
      } else {
        await createAuditProcessStepSummary(request);
        message.success(t("auditProcessStepSummary.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditProcessStepSummary.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1
          ? t("common.deleteConfirmTitleCount", { count: selected.length })
          : t("auditProcessStepSummary.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditProcessStepSummary(item.id)));
          message.success(t("auditProcessStepSummary.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditProcessStepSummary.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditProcessStepSummaryItem>["columns"] = [
    {
      title: t("auditProcessStepSummary.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditProcessStepSummary.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditProcessStepSummary.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditProcessStepSummary.columns.workItem"),
      width: 220,
      render: (_, record) => (record.workItemCode ? `${record.workItemCode} - ${record.workItemName}` : "-"),
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
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditProcessStepSummary.title")}</Typography.Title>
      <CrudTable<AuditProcessStepSummaryItem>
        tableId="audit.plan.processStepSummary"
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
        onExportExcel={canExport ? () => exportAuditProcessStepSummaries("excel") : undefined}
        onExportWord={canExport ? () => exportAuditProcessStepSummaries("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditProcessStepSummaries(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditProcessStepSummary.form.editTitle") : t("auditProcessStepSummary.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={640}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label={t("auditProcessStepSummary.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={30} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditProcessStepSummary.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditProcessStepSummary.columns.name")} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="workItemId" label={t("auditProcessStepSummary.columns.workItem")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={workItems.map((w) => ({ value: w.id, label: `${w.code} - ${w.name}` }))}
            />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
