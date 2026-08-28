import { useCallback, useEffect, useState } from "react";
import { App, Badge, Button, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { PaperClipOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, fetchAttachmentCounts, useClientSearchColumn } from "@govia/ui-kit";
import {
  createDocumentLibrary,
  deleteDocumentLibrary,
  exportDocumentLibrary,
  importDocumentLibrary,
  listDocumentLibrary,
  updateDocumentLibrary,
  type DocumentLibraryItem,
  type DocumentLibraryRequest,
} from "../../../api/documentLibrary";
import { listPositionCatalog, type MasterDataItem as PositionItem } from "../../../api/positionCatalog";
import { httpClient } from "../../../api/client";
import { useAuth } from "../../../auth/AuthContext";
import { DocumentAttachmentDrawer } from "./DocumentAttachmentDrawer";

const ATTACHMENT_ENTITY = "AUDIT_DOCUMENT_LIBRARY";

interface FormValues {
  documentNumber: string;
  documentName: string;
  issueDate?: dayjs.Dayjs;
  effectiveDate?: dayjs.Dayjs;
  issuerPositionId?: string;
  businessActivity?: string;
  topic?: string;
  replacedDocument?: string;
  amendedDocument?: string;
  legalBasis?: string;
  expired: boolean;
  expiryDate?: dayjs.Dayjs;
  content?: string;
}

/** Danh muc "Thu vien tai lieu" (sheet ZTC_TVTL) - trong nhom "Danh muc" cua module Kiem toan noi bo. */
export function DocumentLibraryPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.DOCUMENT_LIBRARY.VIEW");
  const canCreate = hasPermission("AUDIT.DOCUMENT_LIBRARY.CREATE");
  const canEdit = hasPermission("AUDIT.DOCUMENT_LIBRARY.EDIT");
  const canDelete = hasPermission("AUDIT.DOCUMENT_LIBRARY.DELETE");
  const canExport = hasPermission("AUDIT.DOCUMENT_LIBRARY.EXPORT");
  const canImport = hasPermission("AUDIT.DOCUMENT_LIBRARY.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<DocumentLibraryItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<DocumentLibraryItem[]>([]);
  const [positions, setPositions] = useState<PositionItem[]>([]);
  const [attachmentCounts, setAttachmentCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<DocumentLibraryItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<DocumentLibraryItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [attachmentTarget, setAttachmentTarget] = useState<DocumentLibraryItem | null>(null);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, positionList] = await Promise.all([listDocumentLibrary(), listPositionCatalog()]);
      setItems(list);
      setPositions(positionList);
      // So file dinh kem la thong tin bo sung (hien badge tren cot) - loi o day khong duoc lam
      // hong ca man hinh danh sach chinh, chi de badge ve 0 va log ra console de debug.
      try {
        setAttachmentCounts(await fetchAttachmentCounts(httpClient, ATTACHMENT_ENTITY, list.map((i) => i.id)));
      } catch (err) {
        console.error("Khong tai duoc so luong file dinh kem", err);
      }
    } catch {
      message.error(t("documentLibrary.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  const openAttachments = (target: DocumentLibraryItem) => setAttachmentTarget(target);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ expired: false });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      documentNumber: target.documentNumber,
      documentName: target.documentName,
      issueDate: target.issueDate ? dayjs(target.issueDate) : undefined,
      effectiveDate: target.effectiveDate ? dayjs(target.effectiveDate) : undefined,
      issuerPositionId: target.issuerPositionId ?? undefined,
      businessActivity: target.businessActivity ?? undefined,
      topic: target.topic ?? undefined,
      replacedDocument: target.replacedDocument ?? undefined,
      amendedDocument: target.amendedDocument ?? undefined,
      legalBasis: target.legalBasis ?? undefined,
      expired: target.expired,
      expiryDate: target.expiryDate ? dayjs(target.expiryDate) : undefined,
      content: target.content ?? undefined,
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
      const request: DocumentLibraryRequest = {
        documentNumber: values.documentNumber,
        documentName: values.documentName,
        issueDate: values.issueDate ? values.issueDate.format("YYYY-MM-DD") : null,
        effectiveDate: values.effectiveDate ? values.effectiveDate.format("YYYY-MM-DD") : null,
        issuerPositionId: values.issuerPositionId ?? null,
        businessActivity: values.businessActivity ?? null,
        topic: values.topic ?? null,
        replacedDocument: values.replacedDocument ?? null,
        amendedDocument: values.amendedDocument ?? null,
        legalBasis: values.legalBasis ?? null,
        expired: values.expired,
        expiryDate: values.expiryDate ? values.expiryDate.format("YYYY-MM-DD") : null,
        content: values.content ?? null,
      };
      let created: DocumentLibraryItem | null = null;
      if (editing) {
        await updateDocumentLibrary(editing.id, request);
        message.success(t("documentLibrary.messages.updateSuccess"));
      } else {
        created = await createDocumentLibrary(request);
        message.success(t("documentLibrary.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
      // Van ban vua tao chua co file nao - mo luon Drawer dinh kem de nguoi dung dinh kem
      // ngay, thay vi phai tim lai dong vua tao trong danh sach roi bam nut Dinh kem.
      if (created) {
        setAttachmentTarget(created);
      }
    } catch {
      message.error(t("documentLibrary.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("documentLibrary.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteDocumentLibrary(item.id)));
          message.success(t("documentLibrary.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("documentLibrary.messages.deleteError"));
        }
      },
    });
  };

  const handleAttachmentClick = () => {
    const target = selected[0];
    if (!target) {
      message.warning(t("documentLibrary.messages.selectRowFirst"));
      return;
    }
    openAttachments(target);
  };

  const columns: TableProps<DocumentLibraryItem>["columns"] = [
    { title: t("documentLibrary.columns.documentNumber"), width: 140, ...getSearchColumnProps("documentNumber", searchLabels) },
    { title: t("documentLibrary.columns.documentName"), ...getSearchColumnProps("documentName", searchLabels) },
    {
      title: t("documentLibrary.columns.attachments"),
      key: "attachments",
      width: 120,
      align: "center",
      render: (_, record) => {
        const count = attachmentCounts[record.id] ?? 0;
        return (
          <Button type="text" size="small" onClick={() => openAttachments(record)}>
            <Badge count={count} size="small" showZero color={count > 0 ? "#2563eb" : "#d9d9d9"} offset={[6, -1]}>
              <PaperClipOutlined style={{ fontSize: 16 }} />
            </Badge>
          </Button>
        );
      },
    },
    { title: t("documentLibrary.columns.issueDate"), dataIndex: "issueDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("documentLibrary.columns.effectiveDate"), dataIndex: "effectiveDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("documentLibrary.columns.issuerPosition"), dataIndex: "issuerPositionName", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("documentLibrary.columns.businessActivity"), dataIndex: "businessActivity", render: (v: string | null) => v ?? "-" },
    { title: t("documentLibrary.columns.topic"), dataIndex: "topic", render: (v: string | null) => v ?? "-" },
    {
      title: t("documentLibrary.columns.expired"),
      dataIndex: "expired",
      width: 120,
      sorter: (a, b) => Number(a.expired) - Number(b.expired),
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("documentLibrary.title")}</Typography.Title>
      <CrudTable<DocumentLibraryItem>
        tableId="audit.documentLibrary"
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
        onExportExcel={canExport ? () => exportDocumentLibrary("excel") : undefined}
        onExportWord={canExport ? () => exportDocumentLibrary("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importDocumentLibrary(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("documentLibrary.form.editTitle") : t("documentLibrary.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="documentNumber" label={t("documentLibrary.columns.documentNumber")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="documentName" label={t("documentLibrary.columns.documentName")} rules={[{ required: true }]}>
                <Input maxLength={500} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="issueDate" label={t("documentLibrary.columns.issueDate")}>
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="effectiveDate" label={t("documentLibrary.columns.effectiveDate")}>
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="issuerPositionId" label={t("documentLibrary.columns.issuerPosition")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={positions.map((p) => ({ value: p.id, label: `${p.code} - ${p.name}` }))}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="businessActivity" label={t("documentLibrary.columns.businessActivity")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="topic" label={t("documentLibrary.columns.topic")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="replacedDocument" label={t("documentLibrary.columns.replacedDocument")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="amendedDocument" label={t("documentLibrary.columns.amendedDocument")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="legalBasis" label={t("documentLibrary.columns.legalBasis")}>
            <Input />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="expiryDate" label={t("documentLibrary.columns.expiryDate")}>
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="expired" label={t("documentLibrary.columns.expired")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t("documentLibrary.columns.content")}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <DocumentAttachmentDrawer
        open={!!attachmentTarget}
        document={attachmentTarget}
        onClose={() => setAttachmentTarget(null)}
        onCountChange={(documentId, count) => setAttachmentCounts((prev) => ({ ...prev, [documentId]: count }))}
      />
    </div>
  );
}
