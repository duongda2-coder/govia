import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, InputNumber, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, useScreenLock } from "@govia/ui-kit";
import {
  createAuditWorkItem,
  deleteAuditWorkItem,
  exportAuditWorkItems,
  importAuditWorkItems,
  listAuditWorkItems,
  updateAuditWorkItem,
  type AuditWorkItemItem,
  type AuditWorkItemRequest,
  type AuditWorkPhase,
} from "../../../api/auditWorkItem";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { httpClient } from "../../../api/client";
import { useAuth } from "../../../auth/AuthContext";

const SCREEN_KEY = "audit.plan.workItem";

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
  hasSampleSelection: boolean;
}

const PHASES: AuditWorkPhase[] = ["CBKT", "THKT", "DCKT"];

/** Danh muc "Cong viec kiem toan" (sheet ZTC_CV) - trong nhom "Danh muc" cua "Lap ke hoach". */
export function WorkItemPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission, user } = useAuth();
  const lock = useScreenLock(SCREEN_KEY, httpClient, user?.userId);
  const canView = hasPermission("AUDIT.WORK_ITEM.VIEW");
  const canCreate = hasPermission("AUDIT.WORK_ITEM.CREATE");
  const canEdit = hasPermission("AUDIT.WORK_ITEM.EDIT");
  const canDelete = hasPermission("AUDIT.WORK_ITEM.DELETE");
  const canExport = hasPermission("AUDIT.WORK_ITEM.EXPORT");
  const canImport = hasPermission("AUDIT.WORK_ITEM.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditWorkItemItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditWorkItemItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditWorkItemItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditWorkItemItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList] = await Promise.all([listAuditWorkItems(), listMasterDataItems("BUSINESS_SEGMENT")]);
      setItems(list);
      setBusinessSegments(segmentList);
    } catch {
      message.error(t("auditWorkItem.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openCreate = async () => {
    const { ok, status } = await lock.acquire();
    if (!ok) {
      message.warning(t("common.screenLockedBy", { name: status.lockedByName, time: new Date(status.lockedAt ?? "").toLocaleString() }));
      return;
    }
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ active: true, hasSampleSelection: false });
    setModalOpen(true);
  };

  const openEdit = async () => {
    const target = selected[0];
    if (!target) return;
    const { ok, status } = await lock.acquire();
    if (!ok) {
      message.warning(t("common.screenLockedBy", { name: status.lockedByName, time: new Date(status.lockedAt ?? "").toLocaleString() }));
      return;
    }
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
      hasSampleSelection: target.hasSampleSelection,
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
      const request: AuditWorkItemRequest = {
        phase: values.phase ?? null,
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        detailCode: values.detailCode ?? null,
        name: values.name,
        applicableYear: values.applicableYear ?? null,
        workSetCode: values.workSetCode ?? null,
        workType: values.workType ?? null,
        active: values.active,
        hasSampleSelection: values.hasSampleSelection,
      };
      if (editing) {
        await updateAuditWorkItem(editing.id, request);
        message.success(t("auditWorkItem.messages.updateSuccess"));
      } else {
        await createAuditWorkItem(request);
        message.success(t("auditWorkItem.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await lock.release();
      await load();
    } catch {
      message.error(t("auditWorkItem.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditWorkItem.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditWorkItem(item.id)));
          message.success(t("auditWorkItem.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditWorkItem.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditWorkItemItem>["columns"] = [
    {
      title: t("auditWorkItem.columns.phase"),
      dataIndex: "phase",
      width: 140,
      render: (v: AuditWorkPhase | null) => (v ? t(`auditWorkItem.phase.${v}`) : "-"),
    },
    {
      title: t("auditWorkItem.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditWorkItem.columns.code"), width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditWorkItem.columns.detailCode"), width: 120, ...getSearchColumnProps("detailCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditWorkItem.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("auditWorkItem.columns.applicableYear"), dataIndex: "applicableYear", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditWorkItem.columns.workSetCode"), dataIndex: "workSetCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditWorkItem.columns.workType"), dataIndex: "workType", width: 120, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditWorkItem.columns.hasSampleSelection"),
      dataIndex: "hasSampleSelection",
      width: 120,
      sorter: (a, b) => Number(a.hasSampleSelection) - Number(b.hasSampleSelection),
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
    },
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
      <Typography.Title level={4}>{t("auditWorkItem.title")}</Typography.Title>
      <CrudTable<AuditWorkItemItem>
        tableId={SCREEN_KEY}
        screenLock={{ screenKey: SCREEN_KEY, httpClient, currentUserId: user?.userId }}
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
        onExportExcel={canExport ? () => exportAuditWorkItems("excel") : undefined}
        onExportWord={canExport ? () => exportAuditWorkItems("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditWorkItems(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditWorkItem.form.editTitle") : t("auditWorkItem.form.createTitle")}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          lock.release();
        }}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={640}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="phase" label={t("auditWorkItem.columns.phase")}>
                <Select allowClear options={PHASES.map((v) => ({ value: v, label: t(`auditWorkItem.phase.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditWorkItem.columns.businessSegment")}>
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
              <Form.Item name="code" label={t("auditWorkItem.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="detailCode" label={t("auditWorkItem.columns.detailCode")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditWorkItem.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="applicableYear" label={t("auditWorkItem.columns.applicableYear")}>
                <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="workSetCode" label={t("auditWorkItem.columns.workSetCode")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="workType" label={t("auditWorkItem.columns.workType")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="active" label={t("common.active")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="hasSampleSelection" label={t("auditWorkItem.columns.hasSampleSelection")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
