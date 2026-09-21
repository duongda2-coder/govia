import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpResolution,
  deleteTdkpResolution,
  exportTdkpResolutions,
  importTdkpResolutions,
  listTdkpResolutions,
  updateTdkpResolution,
  type TdkpResolutionItem,
  type TdkpResolutionRequest,
  type TdkpStatus,
} from "../../../api/auditTdkp";
import { useAuth } from "../../../auth/AuthContext";
import { fromApiDate, renderDate, renderStatus, renderText, statusOptions, toApiDate } from "./tdkpShared";
import type dayjs from "dayjs";

interface FormValues {
  resolutionNumber: string;
  issueDate?: dayjs.Dayjs;
  content?: string;
  implementation?: string;
  status?: TdkpStatus;
  supervisor?: string;
  issuanceEvaluation?: string;
  note?: string;
}

/** Màn hình ZTC_TDKP_NQ: quản lý, theo dõi và cập nhật tình hình thực hiện nghị quyết của HĐTV. */
export function TdkpResolutionPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.TDKP_NQ.VIEW");
  const canCreate = hasPermission("AUDIT.TDKP_NQ.CREATE");
  const canEdit = hasPermission("AUDIT.TDKP_NQ.EDIT");
  const canDelete = hasPermission("AUDIT.TDKP_NQ.DELETE");
  const canExport = hasPermission("AUDIT.TDKP_NQ.EXPORT");
  const canImport = hasPermission("AUDIT.TDKP_NQ.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<TdkpResolutionItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<TdkpResolutionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<TdkpResolutionItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpResolutionItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listTdkpResolutions());
    } catch {
      message.error(t("auditTdkp.messages.loadError"));
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
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      resolutionNumber: target.resolutionNumber,
      issueDate: fromApiDate(target.issueDate),
      content: target.content ?? undefined,
      implementation: target.implementation ?? undefined,
      status: target.status ?? undefined,
      supervisor: target.supervisor ?? undefined,
      issuanceEvaluation: target.issuanceEvaluation ?? undefined,
      note: target.note ?? undefined,
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
      const request: TdkpResolutionRequest = {
        resolutionNumber: values.resolutionNumber,
        issueDate: toApiDate(values.issueDate),
        content: values.content ?? null,
        implementation: values.implementation ?? null,
        status: values.status ?? null,
        supervisor: values.supervisor ?? null,
        issuanceEvaluation: values.issuanceEvaluation ?? null,
        note: values.note ?? null,
      };
      if (editing) {
        await updateTdkpResolution(editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpResolution(request);
        message.success(t("auditTdkp.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditTdkp.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("common.deleteConfirmTitleCount", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteTdkpResolution(item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const c = "auditTdkp.resolution.columns";
  const columns: TableProps<TdkpResolutionItem>["columns"] = [
    { title: t(`${c}.code`), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t(`${c}.resolutionNumber`), width: 170, ...getSearchColumnProps("resolutionNumber", searchLabels) },
    { title: t(`${c}.issueDate`), dataIndex: "issueDate", width: 140, sorter: (a, b) => (a.issueDate ?? "").localeCompare(b.issueDate ?? ""), render: renderDate },
    { title: t(`${c}.content`), width: 320, ...getSearchColumnProps("content", searchLabels) },
    { title: t(`${c}.implementation`), dataIndex: "implementation", width: 280, render: renderText },
    { title: t(`${c}.status`), dataIndex: "status", width: 150, render: renderStatus(t) },
    { title: t(`${c}.supervisor`), dataIndex: "supervisor", width: 170, render: renderText },
    { title: t(`${c}.issuanceEvaluation`), dataIndex: "issuanceEvaluation", width: 280, render: renderText },
    { title: t(`${c}.note`), dataIndex: "note", width: 220, render: renderText },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditTdkp.resolution.title")}</Typography.Title>
      <CrudTable<TdkpResolutionItem>
        tableId="audit.tdkp.resolution"
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
        onExportExcel={canExport ? () => exportTdkpResolutions() : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importTdkpResolutions(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? `${t("auditTdkp.resolution.form.editTitle")} - ${editing.code}` : t("auditTdkp.resolution.form.createTitle")}
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
              <Form.Item name="resolutionNumber" label={t(`${c}.resolutionNumber`)} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="issueDate" label={t(`${c}.issueDate`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t(`${c}.content`)}>
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="implementation" label={t(`${c}.implementation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label={t(`${c}.status`)}>
                <Select allowClear options={statusOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="supervisor" label={t(`${c}.supervisor`)}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="issuanceEvaluation" label={t(`${c}.issuanceEvaluation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="note" label={t(`${c}.note`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
