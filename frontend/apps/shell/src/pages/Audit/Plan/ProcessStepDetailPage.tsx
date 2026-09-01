import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditProcessStepDetail,
  deleteAuditProcessStepDetail,
  exportAuditProcessStepDetails,
  importAuditProcessStepDetails,
  listAuditProcessStepDetails,
  listAuditProcessStepSummaries,
  updateAuditProcessStepDetail,
  type AuditProcessStepDetailItem,
  type AuditProcessStepDetailRequest,
  type AuditProcessStepSummaryItem,
} from "../../../api/auditProcessStep";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  processStepSummaryId?: string;
  code: string;
  active: boolean;
}

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function ProcessStepDetailPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PROCESS_STEP_DETAIL.VIEW");
  const canCreate = hasPermission("AUDIT.PROCESS_STEP_DETAIL.CREATE");
  const canEdit = hasPermission("AUDIT.PROCESS_STEP_DETAIL.EDIT");
  const canDelete = hasPermission("AUDIT.PROCESS_STEP_DETAIL.DELETE");
  const canExport = hasPermission("AUDIT.PROCESS_STEP_DETAIL.EXPORT");
  const canImport = hasPermission("AUDIT.PROCESS_STEP_DETAIL.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditProcessStepDetailItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditProcessStepDetailItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [summaries, setSummaries] = useState<AuditProcessStepSummaryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessStepDetailItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditProcessStepDetailItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, summaryList] = await Promise.all([
        listAuditProcessStepDetails(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditProcessStepSummaries(),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setSummaries(summaryList);
    } catch {
      message.error(t("auditProcessStepDetail.messages.loadError"));
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
      processStepSummaryId: target.processStepSummaryId ?? undefined,
      code: target.code,
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
      const request: AuditProcessStepDetailRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        code: values.code,
        active: values.active,
      };
      if (editing) {
        await updateAuditProcessStepDetail(editing.id, request);
        message.success(t("auditProcessStepDetail.messages.updateSuccess"));
      } else {
        await createAuditProcessStepDetail(request);
        message.success(t("auditProcessStepDetail.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditProcessStepDetail.messages.saveError"));
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
          : t("auditProcessStepDetail.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditProcessStepDetail(item.id)));
          message.success(t("auditProcessStepDetail.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditProcessStepDetail.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditProcessStepDetailItem>["columns"] = [
    {
      title: t("auditProcessStepDetail.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditProcessStepDetail.columns.processStepSummary"),
      width: 220,
      render: (_, record) => (record.processStepSummaryCode ? `${record.processStepSummaryCode} - ${record.processStepSummaryName}` : "-"),
    },
    { title: t("auditProcessStepDetail.columns.code"), width: 160, ...getSearchColumnProps("code", searchLabels) },
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
      <Typography.Title level={4}>{t("auditProcessStepDetail.title")}</Typography.Title>
      <CrudTable<AuditProcessStepDetailItem>
        tableId="audit.plan.processStepDetail"
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
        onExportExcel={canExport ? () => exportAuditProcessStepDetails("excel") : undefined}
        onExportWord={canExport ? () => exportAuditProcessStepDetails("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditProcessStepDetails(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditProcessStepDetail.form.editTitle") : t("auditProcessStepDetail.form.createTitle")}
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
              <Form.Item name="code" label={t("auditProcessStepDetail.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditProcessStepDetail.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="processStepSummaryId" label={t("auditProcessStepDetail.columns.processStepSummary")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={summaries.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
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
