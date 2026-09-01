import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditExceptionType,
  deleteAuditExceptionType,
  exportAuditExceptionTypes,
  importAuditExceptionTypes,
  listAuditExceptionTypes,
  updateAuditExceptionType,
  type AuditExceptionCategory,
  type AuditExceptionTypeItem,
  type AuditExceptionTypeRequest,
  type AuditLevel,
} from "../../../api/auditExceptionType";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  code: string;
  name: string;
  category?: AuditExceptionCategory;
  impactLevel?: AuditLevel;
  classificationBasis?: string;
  active: boolean;
}

const CATEGORIES: AuditExceptionCategory[] = ["RISK_MANAGEMENT", "INTERNAL_CONTROL"];
const LEVELS: AuditLevel[] = ["HIGH", "MEDIUM", "LOW"];

/** Danh muc "Loai ton tai sai sot" (sheet ZTC_TTSS) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function ExceptionTypePage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.EXCEPTION_TYPE.VIEW");
  const canCreate = hasPermission("AUDIT.EXCEPTION_TYPE.CREATE");
  const canEdit = hasPermission("AUDIT.EXCEPTION_TYPE.EDIT");
  const canDelete = hasPermission("AUDIT.EXCEPTION_TYPE.DELETE");
  const canExport = hasPermission("AUDIT.EXCEPTION_TYPE.EXPORT");
  const canImport = hasPermission("AUDIT.EXCEPTION_TYPE.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditExceptionTypeItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditExceptionTypeItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditExceptionTypeItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditExceptionTypeItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList] = await Promise.all([listAuditExceptionTypes(), listMasterDataItems("BUSINESS_SEGMENT")]);
      setItems(list);
      setBusinessSegments(segmentList);
    } catch {
      message.error(t("auditExceptionType.messages.loadError"));
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
      category: target.category ?? undefined,
      impactLevel: target.impactLevel ?? undefined,
      classificationBasis: target.classificationBasis ?? undefined,
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
      const request: AuditExceptionTypeRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        name: values.name,
        category: values.category ?? null,
        impactLevel: values.impactLevel ?? null,
        classificationBasis: values.classificationBasis ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditExceptionType(editing.id, request);
        message.success(t("auditExceptionType.messages.updateSuccess"));
      } else {
        await createAuditExceptionType(request);
        message.success(t("auditExceptionType.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditExceptionType.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditExceptionType.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditExceptionType(item.id)));
          message.success(t("auditExceptionType.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditExceptionType.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditExceptionTypeItem>["columns"] = [
    {
      title: t("auditExceptionType.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditExceptionType.columns.code"), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditExceptionType.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditExceptionType.columns.category"),
      dataIndex: "category",
      width: 140,
      render: (v: AuditExceptionCategory | null) => (v ? t(`auditExceptionType.category.${v}`) : "-"),
    },
    {
      title: t("auditExceptionType.columns.impactLevel"),
      dataIndex: "impactLevel",
      width: 150,
      render: (v: AuditLevel | null) => (v ? t(`auditExceptionType.level.${v}`) : "-"),
    },
    { title: t("auditExceptionType.columns.classificationBasis"), dataIndex: "classificationBasis", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditExceptionType.title")}</Typography.Title>
      <CrudTable<AuditExceptionTypeItem>
        tableId="audit.plan.exceptionType"
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
        onExportExcel={canExport ? () => exportAuditExceptionTypes("excel") : undefined}
        onExportWord={canExport ? () => exportAuditExceptionTypes("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditExceptionTypes(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditExceptionType.form.editTitle") : t("auditExceptionType.form.createTitle")}
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
              <Form.Item name="code" label={t("auditExceptionType.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditExceptionType.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditExceptionType.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="category" label={t("auditExceptionType.columns.category")}>
                <Select allowClear options={CATEGORIES.map((v) => ({ value: v, label: t(`auditExceptionType.category.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="impactLevel" label={t("auditExceptionType.columns.impactLevel")}>
                <Select allowClear options={LEVELS.map((v) => ({ value: v, label: t(`auditExceptionType.level.${v}`) }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="classificationBasis" label={t("auditExceptionType.columns.classificationBasis")}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
