import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditExceptionTypeQt,
  deleteAuditExceptionTypeQt,
  exportAuditExceptionTypesQt,
  importAuditExceptionTypesQt,
  listAuditExceptionTypesQt,
  updateAuditExceptionTypeQt,
  type AuditExceptionTypeQtItem,
  type AuditExceptionTypeQtRequest,
} from "../../../../api/auditExceptionTypeQt";
import type { AuditExceptionCategory, AuditLevel } from "../../../../api/auditExceptionType";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  code: string;
  name: string;
  category?: AuditExceptionCategory;
  impactLevel?: AuditLevel;
  classificationBasis?: string;
  applicableYear?: number;
  active: boolean;
}

const CATEGORIES: AuditExceptionCategory[] = ["RISK_MANAGEMENT", "INTERNAL_CONTROL"];
const LEVELS: AuditLevel[] = ["HIGH", "MEDIUM", "LOW"];

/** Danh muc "Ton tai sai sot quy trinh" (sheet ZTC_TTSS_QT) - trong nhom "Danh muc Kiem toan quy
 * trinh" cua "Lap ke hoach". Mo phong AuditExceptionType (ExceptionTypePage), tach bang rieng cho
 * quy trinh, mo tu nut "Quy trinh" tren man hinh Loai ton tai sai sot. */
export function ExceptionTypeQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const canView = hasPermission("AUDIT.EXCEPTION_TYPE_QT.VIEW");
  const canCreate = hasPermission("AUDIT.EXCEPTION_TYPE_QT.CREATE");
  const canEdit = hasPermission("AUDIT.EXCEPTION_TYPE_QT.EDIT");
  const canDelete = hasPermission("AUDIT.EXCEPTION_TYPE_QT.DELETE");
  const canExport = hasPermission("AUDIT.EXCEPTION_TYPE_QT.EXPORT");
  const canImport = hasPermission("AUDIT.EXCEPTION_TYPE_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditExceptionTypeQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditExceptionTypeQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [selectedYear, setSelectedYear] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditExceptionTypeQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditExceptionTypeQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, yearList] = await Promise.all([
        listAuditExceptionTypesQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listMasterDataItems("YEAR"),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setYears(yearList);
    } catch {
      message.error(t("auditExceptionTypeQt.messages.loadError"));
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
      category: target.category ?? undefined,
      impactLevel: target.impactLevel ?? undefined,
      classificationBasis: target.classificationBasis ?? undefined,
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
      const request: AuditExceptionTypeQtRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        name: values.name,
        category: values.category ?? null,
        impactLevel: values.impactLevel ?? null,
        classificationBasis: values.classificationBasis ?? null,
        applicableYear: values.applicableYear ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditExceptionTypeQt(editing.id, request);
        message.success(t("auditExceptionTypeQt.messages.updateSuccess"));
      } else {
        await createAuditExceptionTypeQt(request);
        message.success(t("auditExceptionTypeQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditExceptionTypeQt.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditExceptionTypeQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditExceptionTypeQt(item.id)));
          message.success(t("auditExceptionTypeQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditExceptionTypeQt.messages.deleteError"));
        }
      },
    });
  };

  const displayedItems = selectedYear == null ? items : items.filter((i) => i.applicableYear === selectedYear);

  const columns: TableProps<AuditExceptionTypeQtItem>["columns"] = [
    {
      title: t("auditExceptionTypeQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditExceptionTypeQt.columns.code"), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditExceptionTypeQt.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditExceptionTypeQt.columns.category"),
      dataIndex: "category",
      width: 140,
      render: (v: AuditExceptionCategory | null) => (v ? t(`auditExceptionType.category.${v}`) : "-"),
    },
    {
      title: t("auditExceptionTypeQt.columns.impactLevel"),
      dataIndex: "impactLevel",
      width: 150,
      render: (v: AuditLevel | null) => (v ? t(`auditExceptionType.level.${v}`) : "-"),
    },
    { title: t("auditExceptionTypeQt.columns.classificationBasis"), dataIndex: "classificationBasis", render: (v: string | null) => v ?? "-" },
    { title: t("auditExceptionTypeQt.columns.applicableYear"), dataIndex: "applicableYear", width: 100, render: (v: number | null) => v ?? "-" },
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
          {t("auditExceptionTypeQt.title")}
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
          <Button onClick={() => navigate("/audit/plan/master-data/exception-type")}>{t("common.backToStandard")}</Button>
        </Space>
      </div>
      <CrudTable<AuditExceptionTypeQtItem>
        tableId="audit.plan.exceptionTypeQt"
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
        onExportExcel={canExport ? () => exportAuditExceptionTypesQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditExceptionTypesQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditExceptionTypesQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditExceptionTypeQt.form.editTitle") : t("auditExceptionTypeQt.form.createTitle")}
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
              <Form.Item name="code" label={t("auditExceptionTypeQt.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditExceptionTypeQt.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditExceptionTypeQt.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="category" label={t("auditExceptionTypeQt.columns.category")}>
                <Select allowClear options={CATEGORIES.map((v) => ({ value: v, label: t(`auditExceptionType.category.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="impactLevel" label={t("auditExceptionTypeQt.columns.impactLevel")}>
                <Select allowClear options={LEVELS.map((v) => ({ value: v, label: t(`auditExceptionType.level.${v}`) }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="classificationBasis" label={t("auditExceptionTypeQt.columns.classificationBasis")}>
            <Input maxLength={255} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="applicableYear" label={t("auditExceptionTypeQt.columns.applicableYear")}>
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
