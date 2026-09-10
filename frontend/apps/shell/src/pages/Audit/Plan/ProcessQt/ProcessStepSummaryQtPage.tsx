import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditProcessStepSummaryQt,
  deleteAuditProcessStepSummaryQt,
  exportAuditProcessStepSummariesQt,
  importAuditProcessStepSummariesQt,
  listAuditProcessStepSummariesQt,
  updateAuditProcessStepSummaryQt,
  type AuditProcessStepSummaryQtItem,
  type AuditProcessStepSummaryQtRequest,
} from "../../../../api/auditProcessStepQt";
import { listAuditWorkItemsQt, type AuditWorkItemQtItem } from "../../../../api/auditWorkItemQt";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  code: string;
  name: string;
  workItemId?: string;
  applicableYear?: number;
  active: boolean;
}

/** Danh muc "Buoc quy trinh tong hop" (sheet ZTC_BQT_TH_QT) - trong nhom "Danh muc Kiem toan quy
 * trinh" cua "Lap ke hoach". Mo phong AuditProcessStepSummary (ProcessStepSummaryPage), nhung link
 * toi Cong viec quy trinh (WorkItemQtPage/ZTC_CV_QT) thay vi Cong viec kiem toan goc. */
export function ProcessStepSummaryQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.VIEW");
  const canCreate = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.CREATE");
  const canEdit = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.EDIT");
  const canDelete = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.DELETE");
  const canExport = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.EXPORT");
  const canImport = hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditProcessStepSummaryQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditProcessStepSummaryQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [workItems, setWorkItems] = useState<AuditWorkItemQtItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [selectedYear, setSelectedYear] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessStepSummaryQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditProcessStepSummaryQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, workItemList, yearList] = await Promise.all([
        listAuditProcessStepSummariesQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditWorkItemsQt(),
        listMasterDataItems("YEAR"),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setWorkItems(workItemList);
      setYears(yearList);
    } catch {
      message.error(t("auditProcessStepSummaryQt.messages.loadError"));
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
    form.setFieldsValue({ active: true, applicableYear: selectedYear });
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
      applicableYear: target.applicableYear ?? undefined,
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
      const request: AuditProcessStepSummaryQtRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        name: values.name,
        workItemId: values.workItemId ?? null,
        applicableYear: values.applicableYear ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditProcessStepSummaryQt(editing.id, request);
        message.success(t("auditProcessStepSummaryQt.messages.updateSuccess"));
      } else {
        await createAuditProcessStepSummaryQt(request);
        message.success(t("auditProcessStepSummaryQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditProcessStepSummaryQt.messages.saveError"));
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
          : t("auditProcessStepSummaryQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditProcessStepSummaryQt(item.id)));
          message.success(t("auditProcessStepSummaryQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditProcessStepSummaryQt.messages.deleteError"));
        }
      },
    });
  };

  const displayedItems = selectedYear == null ? items : items.filter((i) => i.applicableYear === selectedYear);

  const columns: TableProps<AuditProcessStepSummaryQtItem>["columns"] = [
    {
      title: t("auditProcessStepSummaryQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditProcessStepSummaryQt.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditProcessStepSummaryQt.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditProcessStepSummaryQt.columns.workItem"),
      width: 220,
      render: (_, record) => (record.workItemCode ? `${record.workItemCode} - ${record.workItemName}` : "-"),
    },
    { title: t("auditProcessStepSummaryQt.columns.applicableYear"), dataIndex: "applicableYear", width: 100, render: (v: number | null) => v ?? "-" },
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
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessStepSummaryQt.title")}
        </Typography.Title>
        <Space>
          <Select
            allowClear
            placeholder={t("common.selectYear")}
            style={{ width: 120 }}
            value={selectedYear}
            onChange={(v) => setSelectedYear(v)}
            options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
          />
        </Space>
      </div>
      <CrudTable<AuditProcessStepSummaryQtItem>
        tableId="audit.plan.processStepSummaryQt"
        columns={columns}
        dataSource={displayedItems}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportAuditProcessStepSummariesQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditProcessStepSummariesQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditProcessStepSummariesQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditProcessStepSummaryQt.form.editTitle") : t("auditProcessStepSummaryQt.form.createTitle")}
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
              <Form.Item name="code" label={t("auditProcessStepSummaryQt.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={30} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditProcessStepSummaryQt.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditProcessStepSummaryQt.columns.name")} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="workItemId" label={t("auditProcessStepSummaryQt.columns.workItem")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={workItems.map((w) => ({ value: w.id, label: `${w.code} - ${w.name}` }))}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="applicableYear" label={t("auditProcessStepSummaryQt.columns.applicableYear")}>
                <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="active" label={t("common.active")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
