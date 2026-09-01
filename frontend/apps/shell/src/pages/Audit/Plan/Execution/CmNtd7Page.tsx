import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd7,
  deleteAuditCmNtd7,
  exportAuditCmNtd7,
  importAuditCmNtd7,
  listAuditCmNtd7,
  updateAuditCmNtd7,
  type AuditCmNtd7Item,
  type AuditCmNtd7Request,
} from "../../../../api/auditCmNtd7";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  constructionCode: string;
  constructionName?: string;
  content?: string;
  documentType?: string;
  completenessAssessment?: string;
  assessment?: string;
  auditResult?: string;
  active: boolean;
}

/** Man hinh "Danh sach chon mau ho so cong trinh XDCB" (sheet ZTC_CM_NTD7) - trong nhom "Thuc hien
 * kiem toan" cua "Lap ke hoach". Cot ma chi nhanh la text tu do (IPCAS), khong FK. */
export function CmNtd7Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD7.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD7.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD7.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD7.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD7.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD7.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd7Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd7Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd7Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd7Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd7());
    } catch {
      message.error(t("auditCmNtd7.messages.loadError"));
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
      branchCode: target.branchCode,
      constructionCode: target.constructionCode,
      constructionName: target.constructionName ?? undefined,
      content: target.content ?? undefined,
      documentType: target.documentType ?? undefined,
      completenessAssessment: target.completenessAssessment ?? undefined,
      assessment: target.assessment ?? undefined,
      auditResult: target.auditResult ?? undefined,
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
      const request: AuditCmNtd7Request = {
        branchCode: values.branchCode,
        constructionCode: values.constructionCode,
        constructionName: values.constructionName ?? null,
        content: values.content ?? null,
        documentType: values.documentType ?? null,
        completenessAssessment: values.completenessAssessment ?? null,
        assessment: values.assessment ?? null,
        auditResult: values.auditResult ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd7(editing.id, request);
        message.success(t("auditCmNtd7.messages.updateSuccess"));
      } else {
        await createAuditCmNtd7(request);
        message.success(t("auditCmNtd7.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd7.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd7.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd7(item.id)));
          message.success(t("auditCmNtd7.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd7.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditCmNtd7Item>["columns"] = [
    { title: t("auditCmNtd7.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd7.columns.constructionCode"), width: 130, ...getSearchColumnProps("constructionCode", searchLabels) },
    { title: t("auditCmNtd7.columns.constructionName"), dataIndex: "constructionName", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd7.columns.content"), dataIndex: "content", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd7.columns.documentType"), dataIndex: "documentType", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd7.columns.completenessAssessment"), dataIndex: "completenessAssessment", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd7.columns.assessment"), dataIndex: "assessment", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd7.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd7.title")}</Typography.Title>
      <CrudTable<AuditCmNtd7Item>
        tableId="audit.plan.execution.cmNtd7"
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
        onExportExcel={canExport ? () => exportAuditCmNtd7("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd7("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd7(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd7.form.editTitle") : t("auditCmNtd7.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={680}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="branchCode" label={t("auditCmNtd7.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="constructionCode" label={t("auditCmNtd7.columns.constructionCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="constructionName" label={t("auditCmNtd7.columns.constructionName")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="documentType" label={t("auditCmNtd7.columns.documentType")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t("auditCmNtd7.columns.content")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="completenessAssessment" label={t("auditCmNtd7.columns.completenessAssessment")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="assessment" label={t("auditCmNtd7.columns.assessment")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
          <Form.Item name="auditResult" label={t("auditCmNtd7.columns.auditResult")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
