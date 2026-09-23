import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpUnitRecommendation,
  deleteTdkpUnitRecommendation,
  exportTdkpUnitRecommendations,
  importTdkpUnitRecommendations,
  listTdkpUnitRecommendations,
  updateTdkpUnitRecommendation,
  type TdkpStatus,
  type TdkpUnitRecommendationItem,
  type TdkpUnitRecommendationRequest,
} from "../../../api/auditTdkp";
import type { AssignmentApprovalStatus } from "../../../api/auditWorkManagement";
import { useAuth } from "../../../auth/AuthContext";
import {
  approvalStatusOptions,
  filterOption,
  fromApiDate,
  renderApprovalStatus,
  renderDate,
  renderDeadlineState,
  renderStatus,
  renderText,
  statusOptions,
  toApiDate,
  useTdkpLookups,
} from "./tdkpShared";
import type dayjs from "dayjs";

interface FormValues {
  reportNumber?: string;
  reportDate?: dayjs.Dayjs;
  unitId?: string;
  recommendationTarget?: string;
  content: string;
  deadline?: dayjs.Dayjs;
  implementation?: string;
  status?: TdkpStatus;
  evaluation?: string;
  note?: string;
  approvalStatus?: AssignmentApprovalStatus;
}

/** Màn hình ZTC_TDKP_KTNB: quản lý, theo dõi và cập nhật tình hình thực hiện kiến nghị của Đơn vị đối với KTNB. */
export function TdkpUnitRecommendationPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.TDKP_KTNB.VIEW");
  const canCreate = hasPermission("AUDIT.TDKP_KTNB.CREATE");
  const canEdit = hasPermission("AUDIT.TDKP_KTNB.EDIT");
  const canDelete = hasPermission("AUDIT.TDKP_KTNB.DELETE");
  const canExport = hasPermission("AUDIT.TDKP_KTNB.EXPORT");
  const canImport = hasPermission("AUDIT.TDKP_KTNB.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<TdkpUnitRecommendationItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));

  const [items, setItems] = useState<TdkpUnitRecommendationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<TdkpUnitRecommendationItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpUnitRecommendationItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listTdkpUnitRecommendations());
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
      reportNumber: target.reportNumber ?? undefined,
      reportDate: fromApiDate(target.reportDate),
      unitId: target.unitId ?? undefined,
      recommendationTarget: target.recommendationTarget ?? undefined,
      content: target.content,
      deadline: fromApiDate(target.deadline),
      implementation: target.implementation ?? undefined,
      status: target.status ?? undefined,
      evaluation: target.evaluation ?? undefined,
      note: target.note ?? undefined,
      approvalStatus: target.approvalStatus ?? undefined,
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
      const request: TdkpUnitRecommendationRequest = {
        reportNumber: values.reportNumber ?? null,
        reportDate: toApiDate(values.reportDate),
        unitId: values.unitId ?? null,
        recommendationTarget: values.recommendationTarget ?? null,
        content: values.content,
        deadline: toApiDate(values.deadline),
        implementation: values.implementation ?? null,
        status: values.status ?? null,
        evaluation: values.evaluation ?? null,
        note: values.note ?? null,
        approvalStatus: values.approvalStatus ?? null,
      };
      if (editing) {
        await updateTdkpUnitRecommendation(editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpUnitRecommendation(request);
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
          await Promise.all(selected.map((item) => deleteTdkpUnitRecommendation(item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const c = "auditTdkp.unitRecommendation.columns";
  const columns: TableProps<TdkpUnitRecommendationItem>["columns"] = [
    { title: t(`${c}.code`), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t(`${c}.reportNumber`), width: 150, ...getSearchColumnProps("reportNumber", searchLabels) },
    { title: t(`${c}.reportDate`), dataIndex: "reportDate", width: 120, sorter: (a, b) => (a.reportDate ?? "").localeCompare(b.reportDate ?? ""), render: renderDate },
    { title: t(`${c}.unit`), width: 200, ...getSearchColumnProps("unitName", searchLabels) },
    { title: t(`${c}.recommendationTarget`), dataIndex: "recommendationTarget", width: 180, render: renderText },
    { title: t(`${c}.content`), width: 320, ...getSearchColumnProps("content", searchLabels) },
    { title: t(`${c}.deadline`), dataIndex: "deadline", width: 130, sorter: (a, b) => (a.deadline ?? "").localeCompare(b.deadline ?? ""), render: renderDate },
    { title: t(`${c}.implementation`), dataIndex: "implementation", width: 280, render: renderText },
    { title: t(`${c}.status`), dataIndex: "status", width: 140, render: renderStatus(t) },
    { title: t(`${c}.evaluation`), dataIndex: "evaluation", width: 260, render: renderText },
    { title: t(`${c}.lastEditedDate`), dataIndex: "lastEditedDate", width: 140, sorter: (a, b) => (a.lastEditedDate ?? "").localeCompare(b.lastEditedDate ?? ""), render: renderDate },
    { title: t(`${c}.deadlineState`), dataIndex: "deadlineState", width: 120, render: renderDeadlineState(t) },
    { title: t(`${c}.note`), dataIndex: "note", width: 220, render: renderText },
    { title: t(`${c}.approvalStatus`), dataIndex: "approvalStatus", width: 150, render: renderApprovalStatus(t) },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditTdkp.unitRecommendation.title")}</Typography.Title>
      <CrudTable<TdkpUnitRecommendationItem>
        tableId="audit.tdkp.unitRecommendation"
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
        onExportExcel={canExport ? () => exportTdkpUnitRecommendations() : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importTdkpUnitRecommendations(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? `${t("auditTdkp.unitRecommendation.form.editTitle")} - ${editing.code}` : t("auditTdkp.unitRecommendation.form.createTitle")}
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
              <Form.Item name="reportNumber" label={t(`${c}.reportNumber`)}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reportDate" label={t(`${c}.reportDate`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="unitId" label={t(`${c}.unit`)}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="recommendationTarget" label={t(`${c}.recommendationTarget`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t(`${c}.content`)} rules={[{ required: true }]}>
            <Input.TextArea rows={3} maxLength={2000} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="deadline" label={t(`${c}.deadline`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label={t(`${c}.status`)}>
                <Select allowClear options={statusOptions(t)} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="implementation" label={t(`${c}.implementation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="evaluation" label={t(`${c}.evaluation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="note" label={t(`${c}.note`)}>
                <Input.TextArea rows={2} maxLength={500} showCount />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="approvalStatus" label={t(`${c}.approvalStatus`)}>
                <Select allowClear options={approvalStatusOptions(t)} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
