import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditWorkItemQt,
  deleteAuditWorkItemQt,
  exportAuditWorkItemsQt,
  importAuditWorkItemsQt,
  listAuditWorkItemsQt,
  updateAuditWorkItemQt,
  type AuditWorkItemQtItem,
  type AuditWorkItemQtRequest,
} from "../../../../api/auditWorkItemQt";
import type { AuditWorkPhase } from "../../../../api/auditWorkItem";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  phase?: AuditWorkPhase;
  businessSegmentId?: string;
  code: string;
  detailCode?: string;
  name: string;
  applicableYear?: number;
  workSetCode?: string;
  workType?: string;
  active: boolean;
}

const PHASES: AuditWorkPhase[] = ["CBKT", "THKT", "DCKT"];

/** Danh muc "Bang ma cong viec quy trinh" (sheet ZTC_CV_QT) - trong nhom "Danh muc Kiem toan quy
 * trinh" cua "Lap ke hoach". Mo phong AuditWorkItem (WorkItemPage) nhung tach bang rieng cho quy
 * trinh, mo tu nut "Quy trinh" tren man hinh Cong viec kiem toan. */
export function WorkItemQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const canView = hasPermission("AUDIT.WORK_ITEM_QT.VIEW");
  const canCreate = hasPermission("AUDIT.WORK_ITEM_QT.CREATE");
  const canEdit = hasPermission("AUDIT.WORK_ITEM_QT.EDIT");
  const canDelete = hasPermission("AUDIT.WORK_ITEM_QT.DELETE");
  const canExport = hasPermission("AUDIT.WORK_ITEM_QT.EXPORT");
  const canImport = hasPermission("AUDIT.WORK_ITEM_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditWorkItemQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditWorkItemQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [selectedYear, setSelectedYear] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditWorkItemQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditWorkItemQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, yearList] = await Promise.all([
        listAuditWorkItemsQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listMasterDataItems("YEAR"),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setYears(yearList);
    } catch {
      message.error(t("auditWorkItemQt.messages.loadError"));
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
      phase: target.phase ?? undefined,
      businessSegmentId: target.businessSegmentId ?? undefined,
      code: target.code,
      detailCode: target.detailCode ?? undefined,
      name: target.name,
      applicableYear: target.applicableYear ?? undefined,
      workSetCode: target.workSetCode ?? undefined,
      workType: target.workType ?? undefined,
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
      const request: AuditWorkItemQtRequest = {
        phase: values.phase ?? null,
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        detailCode: values.detailCode ?? null,
        name: values.name,
        applicableYear: values.applicableYear ?? null,
        workSetCode: values.workSetCode ?? null,
        workType: values.workType ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditWorkItemQt(editing.id, request);
        message.success(t("auditWorkItemQt.messages.updateSuccess"));
      } else {
        await createAuditWorkItemQt(request);
        message.success(t("auditWorkItemQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditWorkItemQt.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditWorkItemQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditWorkItemQt(item.id)));
          message.success(t("auditWorkItemQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditWorkItemQt.messages.deleteError"));
        }
      },
    });
  };

  const displayedItems = selectedYear == null ? items : items.filter((i) => i.applicableYear === selectedYear);

  const columns: TableProps<AuditWorkItemQtItem>["columns"] = [
    {
      title: t("auditWorkItemQt.columns.phase"),
      dataIndex: "phase",
      width: 140,
      render: (v: AuditWorkPhase | null) => (v ? t(`auditWorkItem.phase.${v}`) : "-"),
    },
    {
      title: t("auditWorkItemQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditWorkItemQt.columns.code"), width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditWorkItemQt.columns.detailCode"), width: 120, ...getSearchColumnProps("detailCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditWorkItemQt.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("auditWorkItemQt.columns.applicableYear"), dataIndex: "applicableYear", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditWorkItemQt.columns.workSetCode"), dataIndex: "workSetCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditWorkItemQt.columns.workType"), dataIndex: "workType", width: 120, render: (v: string | null) => v ?? "-" },
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
          {t("auditWorkItemQt.title")}
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
          <Button onClick={() => navigate("/audit/plan/master-data/work-item")}>{t("common.backToStandard")}</Button>
        </Space>
      </div>
      <CrudTable<AuditWorkItemQtItem>
        tableId="audit.plan.workItemQt"
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
        onExportExcel={canExport ? () => exportAuditWorkItemsQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditWorkItemsQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditWorkItemsQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditWorkItemQt.form.editTitle") : t("auditWorkItemQt.form.createTitle")}
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
              <Form.Item name="phase" label={t("auditWorkItemQt.columns.phase")}>
                <Select allowClear options={PHASES.map((v) => ({ value: v, label: t(`auditWorkItem.phase.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditWorkItemQt.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label={t("auditWorkItemQt.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="detailCode" label={t("auditWorkItemQt.columns.detailCode")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditWorkItemQt.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="applicableYear" label={t("auditWorkItemQt.columns.applicableYear")}>
                <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="workSetCode" label={t("auditWorkItemQt.columns.workSetCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="workType" label={t("auditWorkItemQt.columns.workType")}>
                <Input maxLength={20} />
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
