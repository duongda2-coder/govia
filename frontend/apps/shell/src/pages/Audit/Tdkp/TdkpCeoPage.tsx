import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { SwapOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpCeo,
  deleteTdkpCeo,
  exportTdkpCeo,
  importTdkpCeo,
  listTdkpCeo,
  transferTdkpCeoFromAll,
  updateTdkpCeo,
  type TdkpCeoItem,
  type TdkpCeoRequest,
  type TdkpCeoVariant,
  type TdkpStatus,
  type TdkpTarget,
} from "../../../api/auditTdkp";
import { useAuth } from "../../../auth/AuthContext";
import {
  filterOption,
  fromApiDate,
  renderDate,
  renderDeadlineState,
  renderStatus,
  renderText,
  statusOptions,
  targetOptions,
  toApiDate,
  useTdkpLookups,
} from "./tdkpShared";
import type dayjs from "dayjs";

interface FormValues {
  reportNumber?: string;
  reportDate?: dayjs.Dayjs;
  content: string;
  recommendationTypeId?: string;
  businessSegmentId?: string;
  targetObject?: TdkpTarget;
  executingUnitId?: string;
  directive?: string;
  deadline?: dayjs.Dayjs;
  status?: TdkpStatus;
  evaluation?: string;
  note?: string;
}

/** Kiến nghị của KTNB đối với HĐTV/TGĐ - variant "all" = ZTC_TDKP_CEO_ALL (Phòng nghiệp vụ cập nhật), "kh" = ZTC_TDKP_CEO_KH (Phòng Kế hoạch, chuyển từ ALL). */
export function TdkpCeoPage({ variant }: { variant: TdkpCeoVariant }) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const perm = variant === "all" ? "AUDIT.TDKP_CEO_ALL" : "AUDIT.TDKP_CEO_KH";
  const canView = hasPermission(`${perm}.VIEW`);
  const canCreate = hasPermission(`${perm}.CREATE`);
  const canEdit = hasPermission(`${perm}.EDIT`);
  const canDelete = hasPermission(`${perm}.DELETE`);
  const canExport = hasPermission(`${perm}.EXPORT`);
  const canImport = hasPermission(`${perm}.IMPORT`);
  const canTransfer = variant === "kh" && hasPermission(`${perm}.TRANSFER`);
  const { getSearchColumnProps } = useClientSearchColumn<TdkpCeoItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));

  const [items, setItems] = useState<TdkpCeoItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [transferring, setTransferring] = useState(false);
  const [selected, setSelected] = useState<TdkpCeoItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpCeoItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listTdkpCeo(variant));
    } catch {
      message.error(t("auditTdkp.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t, variant]);

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
      content: target.content,
      recommendationTypeId: target.recommendationTypeId ?? undefined,
      businessSegmentId: target.businessSegmentId ?? undefined,
      targetObject: target.targetObject ?? undefined,
      executingUnitId: target.executingUnitId ?? undefined,
      directive: target.directive ?? undefined,
      deadline: fromApiDate(target.deadline),
      status: target.status ?? undefined,
      evaluation: target.evaluation ?? undefined,
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
      const request: TdkpCeoRequest = {
        reportNumber: values.reportNumber ?? null,
        reportDate: toApiDate(values.reportDate),
        content: values.content,
        recommendationTypeId: values.recommendationTypeId ?? null,
        businessSegmentId: values.businessSegmentId ?? null,
        targetObject: values.targetObject ?? null,
        executingUnitId: values.executingUnitId ?? null,
        directive: values.directive ?? null,
        deadline: toApiDate(values.deadline),
        status: values.status ?? null,
        evaluation: values.evaluation ?? null,
        note: values.note ?? null,
      };
      if (editing) {
        await updateTdkpCeo(variant, editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpCeo(variant, request);
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
          await Promise.all(selected.map((item) => deleteTdkpCeo(variant, item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const handleTransfer = async () => {
    setTransferring(true);
    try {
      const result = await transferTdkpCeoFromAll();
      message.success(t("auditTdkp.ceo.transferResult", { transferred: result.transferred, skipped: result.skipped }));
      await load();
    } catch {
      message.error(t("auditTdkp.ceo.transferError"));
    } finally {
      setTransferring(false);
    }
  };

  const c = "auditTdkp.ceo.columns";
  const columns: TableProps<TdkpCeoItem>["columns"] = [
    { title: t(`${c}.reportNumber`), width: 150, ...getSearchColumnProps("reportNumber", searchLabels) },
    { title: t(`${c}.reportDate`), dataIndex: "reportDate", width: 120, sorter: (a, b) => (a.reportDate ?? "").localeCompare(b.reportDate ?? ""), render: renderDate },
    { title: t(`${c}.content`), width: 320, ...getSearchColumnProps("content", searchLabels) },
    { title: t(`${c}.recommendationType`), dataIndex: "recommendationTypeName", width: 180, render: renderText },
    { title: t(`${c}.businessSegment`), dataIndex: "businessSegmentCode", width: 130, render: renderText },
    { title: t(`${c}.targetObject`), dataIndex: "targetObjectLabel", width: 130, render: renderText },
    { title: t(`${c}.executingUnit`), dataIndex: "executingUnitName", width: 180, render: renderText },
    { title: t(`${c}.directive`), dataIndex: "directive", width: 240, render: renderText },
    { title: t(`${c}.deadline`), dataIndex: "deadline", width: 130, sorter: (a, b) => (a.deadline ?? "").localeCompare(b.deadline ?? ""), render: renderDate },
    { title: t(`${c}.status`), dataIndex: "status", width: 140, render: renderStatus(t) },
    { title: t(`${c}.evaluation`), dataIndex: "evaluation", width: 260, render: renderText },
    { title: t(`${c}.deadlineState`), dataIndex: "deadlineState", width: 120, render: renderDeadlineState(t) },
    ...(variant === "all"
      ? [{ title: t(`${c}.lastEditedDate`), dataIndex: "lastEditedDate", width: 150, render: renderDate } as const]
      : []),
    { title: t(`${c}.lastEditedBy`), dataIndex: "lastEditedBy", width: 140, render: renderText },
    { title: t(`${c}.note`), dataIndex: "note", width: 220, render: renderText },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t(variant === "all" ? "auditTdkp.ceo.titleAll" : "auditTdkp.ceo.titleKh")}</Typography.Title>
      {canTransfer && (
        <Space style={{ marginBottom: 16 }} wrap>
          <Button icon={<SwapOutlined />} loading={transferring} onClick={handleTransfer}>
            {t("auditTdkp.ceo.transferButton")}
          </Button>
        </Space>
      )}
      <CrudTable<TdkpCeoItem>
        tableId={`audit.tdkp.ceo.${variant}`}
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
        onExportExcel={canExport ? () => exportTdkpCeo(variant) : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importTdkpCeo(variant, file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditTdkp.ceo.form.editTitle") : t("auditTdkp.ceo.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={820}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="reportNumber" label={t(`${c}.reportNumber`)}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reportDate" label={t(`${c}.reportDate`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t(`${c}.content`)} rules={[{ required: true }]}>
            <Input.TextArea rows={3} maxLength={2000} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationTypeId" label={t(`${c}.recommendationType`)}>
                <Select allowClear options={lookups.recommendationTypes.map((o) => ({ value: o.id, label: o.name }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t(`${c}.businessSegment`)}>
                <Select allowClear showSearch optionFilterProp="label" options={lookups.businessSegments.map((o) => ({ value: o.id, label: `${o.code} - ${o.name}` }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="targetObject" label={t(`${c}.targetObject`)}>
                <Select allowClear options={targetOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="executingUnitId" label={t(`${c}.executingUnit`)}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="directive" label={t(`${c}.directive`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
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
          <Form.Item name="evaluation" label={t(`${c}.evaluation`)}>
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
