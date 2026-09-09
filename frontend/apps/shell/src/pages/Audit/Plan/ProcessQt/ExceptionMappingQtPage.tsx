import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditExceptionMappingQt,
  deleteAuditExceptionMappingQt,
  exportAuditExceptionMappingsQt,
  importAuditExceptionMappingsQt,
  listAuditExceptionMappingsQt,
  updateAuditExceptionMappingQt,
  type AuditExceptionMappingQtItem,
  type AuditExceptionMappingQtRequest,
} from "../../../../api/auditExceptionMappingQt";
import { listAuditProcessStepDetailsQt, type AuditProcessStepDetailQtItem } from "../../../../api/auditProcessStepQt";
import { listAuditExceptionTypesQt, type AuditExceptionTypeQtItem } from "../../../../api/auditExceptionTypeQt";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  processStepDetailId: string;
  exceptionTypeId: string;
  active: boolean;
}

/** Danh muc "Mapping ton tai sai sot quy trinh" (sheet ZTC_TTSS_MAP_QT) - trong nhom "Danh muc
 * Kiem toan quy trinh" cua "Lap ke hoach". Mo phong AuditExceptionMapping (ExceptionMappingPage),
 * nhung link toi Buoc quy trinh chi tiet quy trinh (ZTC_BQT_MAP_QT) va Ton tai sai sot quy trinh
 * (ZTC_TTSS_QT) thay vi cac bang goc. Man hinh cuoi cung khep lai module "Kiem toan quy trinh". */
export function ExceptionMappingQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.VIEW");
  const canCreate = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.CREATE");
  const canEdit = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.EDIT");
  const canDelete = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.DELETE");
  const canExport = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.EXPORT");
  const canImport = hasPermission("AUDIT.EXCEPTION_MAPPING_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditExceptionMappingQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditExceptionMappingQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [processStepDetails, setProcessStepDetails] = useState<AuditProcessStepDetailQtItem[]>([]);
  const [exceptionTypes, setExceptionTypes] = useState<AuditExceptionTypeQtItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditExceptionMappingQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditExceptionMappingQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, detailList, exceptionTypeList] = await Promise.all([
        listAuditExceptionMappingsQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditProcessStepDetailsQt(),
        listAuditExceptionTypesQt(),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setProcessStepDetails(detailList);
      setExceptionTypes(exceptionTypeList);
    } catch {
      message.error(t("auditExceptionMappingQt.messages.loadError"));
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
      const request: AuditExceptionMappingQtRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        processStepDetailId: values.processStepDetailId,
        exceptionTypeId: values.exceptionTypeId,
        active: values.active,
      };
      if (editing) {
        await updateAuditExceptionMappingQt(editing.id, request);
        message.success(t("auditExceptionMappingQt.messages.updateSuccess"));
      } else {
        await createAuditExceptionMappingQt(request);
        message.success(t("auditExceptionMappingQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditExceptionMappingQt.messages.saveError"));
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
          : t("auditExceptionMappingQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditExceptionMappingQt(item.id)));
          message.success(t("auditExceptionMappingQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditExceptionMappingQt.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditExceptionMappingQtItem>["columns"] = [
    {
      title: t("auditExceptionMappingQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditExceptionMappingQt.columns.processStepDetail"),
      width: 180,
      ...getSearchColumnProps("processStepDetailCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditExceptionMappingQt.columns.exceptionTypeCode"), width: 150, ...getSearchColumnProps("exceptionTypeCode", searchLabels) },
    { title: t("auditExceptionMappingQt.columns.exceptionTypeName"), dataIndex: "exceptionTypeName", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditExceptionMappingQt.title")}</Typography.Title>
      <CrudTable<AuditExceptionMappingQtItem>
        tableId="audit.plan.exceptionMappingQt"
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
        onExportExcel={canExport ? () => exportAuditExceptionMappingsQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditExceptionMappingsQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditExceptionMappingsQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditExceptionMappingQt.form.editTitle") : t("auditExceptionMappingQt.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="businessSegmentId" label={t("auditExceptionMappingQt.columns.businessSegment")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="processStepDetailId" label={t("auditExceptionMappingQt.columns.processStepDetail")} rules={[{ required: true }]}>
                <Select showSearch optionFilterProp="label" options={processStepDetails.map((d) => ({ value: d.id, label: d.code }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="exceptionTypeId" label={t("auditExceptionMappingQt.columns.exceptionTypeCode")} rules={[{ required: true }]}>
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
