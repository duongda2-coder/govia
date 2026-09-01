import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditExceptionMapping,
  deleteAuditExceptionMapping,
  exportAuditExceptionMappings,
  importAuditExceptionMappings,
  listAuditExceptionMappings,
  updateAuditExceptionMapping,
  type AuditExceptionMappingItem,
  type AuditExceptionMappingRequest,
} from "../../../api/auditExceptionMapping";
import { listAuditProcessStepDetails, type AuditProcessStepDetailItem } from "../../../api/auditProcessStep";
import { listAuditExceptionTypes, type AuditExceptionTypeItem } from "../../../api/auditExceptionType";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  processStepDetailId: string;
  exceptionTypeId: string;
  active: boolean;
}

/** Danh muc "Mapping ton tai sai sot" (sheet ZTC_TTSS_MAP) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function ExceptionMappingPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.EXCEPTION_MAPPING.VIEW");
  const canCreate = hasPermission("AUDIT.EXCEPTION_MAPPING.CREATE");
  const canEdit = hasPermission("AUDIT.EXCEPTION_MAPPING.EDIT");
  const canDelete = hasPermission("AUDIT.EXCEPTION_MAPPING.DELETE");
  const canExport = hasPermission("AUDIT.EXCEPTION_MAPPING.EXPORT");
  const canImport = hasPermission("AUDIT.EXCEPTION_MAPPING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditExceptionMappingItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditExceptionMappingItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [processStepDetails, setProcessStepDetails] = useState<AuditProcessStepDetailItem[]>([]);
  const [exceptionTypes, setExceptionTypes] = useState<AuditExceptionTypeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditExceptionMappingItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditExceptionMappingItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, detailList, exceptionTypeList] = await Promise.all([
        listAuditExceptionMappings(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditProcessStepDetails(),
        listAuditExceptionTypes(),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setProcessStepDetails(detailList);
      setExceptionTypes(exceptionTypeList);
    } catch {
      message.error(t("auditExceptionMapping.messages.loadError"));
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
      processStepDetailId: target.processStepDetailId,
      exceptionTypeId: target.exceptionTypeId,
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
      const request: AuditExceptionMappingRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        processStepDetailId: values.processStepDetailId,
        exceptionTypeId: values.exceptionTypeId,
        active: values.active,
      };
      if (editing) {
        await updateAuditExceptionMapping(editing.id, request);
        message.success(t("auditExceptionMapping.messages.updateSuccess"));
      } else {
        await createAuditExceptionMapping(request);
        message.success(t("auditExceptionMapping.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditExceptionMapping.messages.saveError"));
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
          : t("auditExceptionMapping.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditExceptionMapping(item.id)));
          message.success(t("auditExceptionMapping.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditExceptionMapping.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditExceptionMappingItem>["columns"] = [
    {
      title: t("auditExceptionMapping.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditExceptionMapping.columns.processStepDetail"),
      width: 180,
      ...getSearchColumnProps("processStepDetailCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditExceptionMapping.columns.exceptionTypeCode"), width: 150, ...getSearchColumnProps("exceptionTypeCode", searchLabels) },
    { title: t("auditExceptionMapping.columns.exceptionTypeName"), dataIndex: "exceptionTypeName", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditExceptionMapping.title")}</Typography.Title>
      <CrudTable<AuditExceptionMappingItem>
        tableId="audit.plan.exceptionMapping"
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
        onExportExcel={canExport ? () => exportAuditExceptionMappings("excel") : undefined}
        onExportWord={canExport ? () => exportAuditExceptionMappings("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditExceptionMappings(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditExceptionMapping.form.editTitle") : t("auditExceptionMapping.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="businessSegmentId" label={t("auditExceptionMapping.columns.businessSegment")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="processStepDetailId" label={t("auditExceptionMapping.columns.processStepDetail")} rules={[{ required: true }]}>
                <Select showSearch optionFilterProp="label" options={processStepDetails.map((d) => ({ value: d.id, label: d.code }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="exceptionTypeId" label={t("auditExceptionMapping.columns.exceptionTypeCode")} rules={[{ required: true }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  options={exceptionTypes.map((e) => ({ value: e.id, label: `${e.code} - ${e.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
