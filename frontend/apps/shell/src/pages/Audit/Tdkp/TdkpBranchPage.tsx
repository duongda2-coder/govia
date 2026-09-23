import { useCallback, useEffect, useState } from "react";
import { App, Button, Card, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { SwapOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpBranchDefect,
  createTdkpBranchRecommendation,
  deleteTdkpBranchDefect,
  deleteTdkpBranchRecommendation,
  exportTdkpBranchDefects,
  exportTdkpBranchRecommendations,
  importTdkpBranchRecommendations,
  listTdkpBranchDefects,
  listTdkpBranchRecommendations,
  listTdkpBranchStaffOptions,
  transferTdkpBranchFromExecution,
  updateTdkpBranchDefect,
  updateTdkpBranchRecommendation,
  type TdkpBranchDefectItem,
  type TdkpBranchDefectRequest,
  type TdkpBranchRecommendationItem,
  type TdkpBranchRecommendationRequest,
  type TdkpStatus,
} from "../../../api/auditTdkp";
import { useAuth } from "../../../auth/AuthContext";
import { filterOption, fromApiDate, renderDate, renderDeadlineState, renderStatus, renderText, statusOptions, toApiDate, useTdkpLookups } from "./tdkpShared";
import type dayjs from "dayjs";

interface RecommendationFormValues {
  auditObjectUnitId: string;
  auditYear?: number;
  content: string;
  recommendationTypeId?: string;
  businessSegmentId?: string;
  deadline?: dayjs.Dayjs;
  editContent?: string;
  status?: TdkpStatus;
  evaluation?: string;
  note?: string;
}

interface DefectFormValues {
  defectContent?: string;
  customerEntry?: string;
  creditContract?: string;
  defectCode?: string;
  defectType?: string;
  customerStatus?: TdkpStatus;
  relatedStaff?: string;
}

/** Màn hình ZTC_TDKP_CN: theo dõi kiến nghị của KTNB đối với Chi nhánh - bảng kiến nghị tổng hợp + bảng chi tiết tình hình chỉnh sửa tồn tại sai sót. */
export function TdkpBranchPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.TDKP_CN.VIEW");
  const canCreate = hasPermission("AUDIT.TDKP_CN.CREATE");
  const canEdit = hasPermission("AUDIT.TDKP_CN.EDIT");
  const canDelete = hasPermission("AUDIT.TDKP_CN.DELETE");
  const canExport = hasPermission("AUDIT.TDKP_CN.EXPORT");
  const canImport = hasPermission("AUDIT.TDKP_CN.IMPORT");
  const canTransfer = hasPermission("AUDIT.TDKP_CN.TRANSFER");
  const { getSearchColumnProps } = useClientSearchColumn<TdkpBranchRecommendationItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));

  const [items, setItems] = useState<TdkpBranchRecommendationItem[]>([]);
  const [defects, setDefects] = useState<TdkpBranchDefectItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [defectLoading, setDefectLoading] = useState(false);
  const [transferring, setTransferring] = useState(false);
  const [selected, setSelected] = useState<TdkpBranchRecommendationItem[]>([]);
  const [selectedDefects, setSelectedDefects] = useState<TdkpBranchDefectItem[]>([]);
  const [staffOptions, setStaffOptions] = useState<string[]>([]);
  const [yearFilter, setYearFilter] = useState<number | undefined>(undefined);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpBranchRecommendationItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<RecommendationFormValues>();

  const [defectModalOpen, setDefectModalOpen] = useState(false);
  const [editingDefect, setEditingDefect] = useState<TdkpBranchDefectItem | null>(null);
  const [defectSubmitting, setDefectSubmitting] = useState(false);
  const [defectForm] = Form.useForm<DefectFormValues>();

  const current = selected.length === 1 ? selected[0] : null;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listTdkpBranchRecommendations());
    } catch {
      message.error(t("auditTdkp.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  const loadDefects = useCallback(
    async (recommendationId: string | undefined) => {
      if (!recommendationId) {
        setDefects([]);
        setStaffOptions([]);
        return;
      }
      setDefectLoading(true);
      try {
        const [rows, staff] = await Promise.all([listTdkpBranchDefects(recommendationId), listTdkpBranchStaffOptions(recommendationId)]);
        setDefects(rows);
        setStaffOptions(staff);
      } catch {
        message.error(t("auditTdkp.messages.loadError"));
      } finally {
        setDefectLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedDefects([]);
    loadDefects(current?.id);
  }, [current?.id, loadDefects]);

  // ---------- bảng tổng hợp ----------

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = () => {
    if (!current) return;
    setEditing(current);
    form.setFieldsValue({
      auditObjectUnitId: current.auditObjectUnitId,
      auditYear: current.auditYear ?? undefined,
      content: current.content,
      recommendationTypeId: current.recommendationTypeId ?? undefined,
      businessSegmentId: current.businessSegmentId ?? undefined,
      deadline: fromApiDate(current.deadline),
      editContent: current.editContent ?? undefined,
      status: current.status ?? undefined,
      evaluation: current.evaluation ?? undefined,
      note: current.note ?? undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    let values: RecommendationFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request: TdkpBranchRecommendationRequest = {
        auditObjectUnitId: values.auditObjectUnitId,
        auditYear: values.auditYear ?? null,
        content: values.content,
        recommendationTypeId: values.recommendationTypeId ?? null,
        businessSegmentId: values.businessSegmentId ?? null,
        deadline: toApiDate(values.deadline),
        editContent: values.editContent ?? null,
        status: values.status ?? null,
        evaluation: values.evaluation ?? null,
        note: values.note ?? null,
      };
      if (editing) {
        await updateTdkpBranchRecommendation(editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpBranchRecommendation(request);
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
      title: t("auditTdkp.branch.deleteConfirmTitle", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteTdkpBranchRecommendation(item.id)));
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
      const result = await transferTdkpBranchFromExecution();
      message.success(
        t("auditTdkp.branch.transferResult", { recommendations: result.recommendationsCreated, defects: result.defectsCreated, skipped: result.skipped }),
      );
      await load();
      await loadDefects(current?.id);
    } catch {
      message.error(t("auditTdkp.branch.transferError"));
    } finally {
      setTransferring(false);
    }
  };

  // ---------- bảng chi tiết sai sót ----------

  const openDefectCreate = () => {
    setEditingDefect(null);
    defectForm.resetFields();
    setDefectModalOpen(true);
  };

  const openDefectEdit = () => {
    const target = selectedDefects[0];
    if (!target) return;
    setEditingDefect(target);
    defectForm.setFieldsValue({
      defectContent: target.defectContent ?? undefined,
      customerEntry: target.customerEntry ?? undefined,
      creditContract: target.creditContract ?? undefined,
      defectCode: target.defectCode ?? undefined,
      defectType: target.defectType ?? undefined,
      customerStatus: target.customerStatus ?? undefined,
      relatedStaff: target.relatedStaff ?? undefined,
    });
    setDefectModalOpen(true);
  };

  const handleDefectSubmit = async () => {
    if (!current) return;
    let values: DefectFormValues;
    try {
      values = await defectForm.validateFields();
    } catch {
      return;
    }
    setDefectSubmitting(true);
    try {
      const request: TdkpBranchDefectRequest = {
        branchRecommendationId: current.id,
        defectContent: values.defectContent ?? null,
        customerEntry: values.customerEntry ?? null,
        creditContract: values.creditContract ?? null,
        defectCode: values.defectCode ?? null,
        defectType: values.defectType ?? null,
        customerStatus: values.customerStatus ?? null,
        relatedStaff: values.relatedStaff ?? null,
      };
      if (editingDefect) {
        await updateTdkpBranchDefect(editingDefect.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpBranchDefect(request);
        message.success(t("auditTdkp.messages.createSuccess"));
      }
      setDefectModalOpen(false);
      setSelectedDefects([]);
      await Promise.all([loadDefects(current.id), load()]);
    } catch {
      message.error(t("auditTdkp.messages.saveError"));
    } finally {
      setDefectSubmitting(false);
    }
  };

  const handleDefectDelete = () => {
    if (selectedDefects.length === 0) return;
    modal.confirm({
      title: t("common.deleteConfirmTitleCount", { count: selectedDefects.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selectedDefects.map((item) => deleteTdkpBranchDefect(item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelectedDefects([]);
          await Promise.all([loadDefects(current?.id), load()]);
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const c = "auditTdkp.branch.columns";
  const columns: TableProps<TdkpBranchRecommendationItem>["columns"] = [
    { title: t(`${c}.managementCode`), width: 130, ...getSearchColumnProps("managementCode", searchLabels) },
    { title: t(`${c}.branchName`), width: 200, ...getSearchColumnProps("branchName", searchLabels) },
    { title: t(`${c}.branchCode`), dataIndex: "branchCode", width: 120, render: renderText },
    { title: t(`${c}.auditYear`), dataIndex: "auditYear", width: 100, align: "center", sorter: (a, b) => (a.auditYear ?? 0) - (b.auditYear ?? 0), render: renderText },
    { title: t(`${c}.content`), width: 320, ...getSearchColumnProps("content", searchLabels) },
    { title: t(`${c}.recommendationType`), dataIndex: "recommendationTypeName", width: 180, render: renderText },
    { title: t(`${c}.businessSegment`), dataIndex: "businessSegmentCode", width: 130, render: renderText },
    { title: t(`${c}.deadline`), dataIndex: "deadline", width: 150, sorter: (a, b) => (a.deadline ?? "").localeCompare(b.deadline ?? ""), render: renderDate },
    { title: t(`${c}.editContent`), dataIndex: "editContent", width: 240, render: renderText },
    { title: t(`${c}.status`), dataIndex: "status", width: 140, render: renderStatus(t) },
    { title: t(`${c}.evaluation`), dataIndex: "evaluation", width: 260, render: renderText },
    { title: t(`${c}.deadlineState`), dataIndex: "deadlineState", width: 120, render: renderDeadlineState(t) },
    {
      title: t(`${c}.defectCount`),
      key: "defectCount",
      width: 140,
      align: "center",
      render: (_: unknown, row) => `${row.defectDoneCount}/${row.defectCount}`,
    },
    { title: t(`${c}.note`), dataIndex: "note", width: 220, render: renderText },
  ];

  const d = "auditTdkp.branch.defectColumns";
  const defectColumns: TableProps<TdkpBranchDefectItem>["columns"] = [
    { title: t(`${d}.managementCode`), dataIndex: "managementCode", width: 130 },
    { title: t(`${d}.defectContent`), dataIndex: "defectContent", width: 320, render: renderText },
    { title: t(`${d}.customerEntry`), dataIndex: "customerEntry", width: 240, render: renderText },
    { title: t(`${d}.creditContract`), dataIndex: "creditContract", width: 180, render: renderText },
    { title: t(`${d}.defectCode`), dataIndex: "defectCode", width: 140, render: renderText },
    { title: t(`${d}.defectType`), dataIndex: "defectType", width: 200, render: renderText },
    { title: t(`${d}.recommendationDefectStatus`), dataIndex: "recommendationDefectStatus", width: 180, render: renderStatus(t) },
    { title: t(`${d}.customerStatus`), dataIndex: "customerStatus", width: 180, render: renderStatus(t) },
    { title: t(`${d}.relatedStaff`), dataIndex: "relatedStaff", width: 200, render: renderText },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  const staffSelectOptions = Array.from(new Set([...staffOptions, ...(editingDefect?.relatedStaff ? [editingDefect.relatedStaff] : [])])).map((name) => ({
    value: name,
    label: name,
  }));

  const yearOptions = Array.from(new Set(items.map((item) => item.auditYear).filter((year): year is number => year != null))).sort((a, b) => b - a);
  const filteredItems = yearFilter == null ? items : items.filter((item) => item.auditYear === yearFilter);

  return (
    <div>
      <Typography.Title level={4}>{t("auditTdkp.branch.title")}</Typography.Title>
      <Space style={{ marginBottom: 16 }} wrap>
        {canTransfer && (
          <Button icon={<SwapOutlined />} loading={transferring} onClick={handleTransfer}>
            {t("auditTdkp.branch.transferButton")}
          </Button>
        )}
        <Typography.Text>{t("auditTdkp.branch.yearFilter")}</Typography.Text>
        <Select
          style={{ width: 140 }}
          allowClear
          placeholder={t("auditTdkp.branch.allYears")}
          options={yearOptions.map((year) => ({ value: year, label: year }))}
          value={yearFilter}
          onChange={setYearFilter}
        />
      </Space>
      <Typography.Title level={5}>{t("auditTdkp.branch.summaryTitle")}</Typography.Title>
      <CrudTable<TdkpBranchRecommendationItem>
        tableId="audit.tdkp.branch.summary"
        columns={columns}
        dataSource={filteredItems}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportTdkpBranchRecommendations() : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importTdkpBranchRecommendations(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Typography.Title level={5} style={{ marginTop: 24 }}>
        {t("auditTdkp.branch.defectTitle")}
        {current ? ` - ${current.managementCode}` : ""}
      </Typography.Title>
      {!current && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Typography.Text type="secondary">{t("auditTdkp.branch.selectRecommendationHint")}</Typography.Text>
        </Card>
      )}
      <CrudTable<TdkpBranchDefectItem>
        tableId="audit.tdkp.branch.defect"
        columns={defectColumns}
        dataSource={defects}
        rowKey="id"
        loading={defectLoading}
        onAdd={canCreate ? openDefectCreate : undefined}
        addDisabled={!current}
        onEdit={canEdit ? openDefectEdit : undefined}
        editDisabled={selectedDefects.length !== 1}
        onDelete={canDelete ? handleDefectDelete : undefined}
        deleteDisabled={selectedDefects.length === 0}
        onSelectionChange={(_keys, rows) => setSelectedDefects(rows)}
        onExportExcel={canExport ? () => exportTdkpBranchDefects() : undefined}
      />

      <Modal
        title={editing ? `${t("auditTdkp.branch.form.editTitle")} - ${editing.managementCode}` : t("auditTdkp.branch.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={820}
      >
        <Form<RecommendationFormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={16}>
              <Form.Item name="auditObjectUnitId" label={t(`${c}.branchName`)} rules={[{ required: true }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="auditYear" label={t(`${c}.auditYear`)}>
                <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
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
          <Form.Item name="editContent" label={t(`${c}.editContent`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="evaluation" label={t(`${c}.evaluation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="note" label={t(`${c}.note`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingDefect ? t("auditTdkp.branch.defectForm.editTitle") : t("auditTdkp.branch.defectForm.createTitle")}
        open={defectModalOpen}
        onCancel={() => setDefectModalOpen(false)}
        onOk={handleDefectSubmit}
        confirmLoading={defectSubmitting}
        destroyOnClose
        width={720}
      >
        <Form<DefectFormValues> form={defectForm} layout="vertical">
          <Form.Item name="defectContent" label={t(`${d}.defectContent`)}>
            <Input.TextArea rows={3} maxLength={2000} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerEntry" label={t(`${d}.customerEntry`)}>
                <Input maxLength={500} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="creditContract" label={t(`${d}.creditContract`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="defectCode" label={t(`${d}.defectCode`)}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="defectType" label={t(`${d}.defectType`)}>
                <Input maxLength={255} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerStatus" label={t(`${d}.customerStatus`)}>
                <Select allowClear options={statusOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="relatedStaff" label={t(`${d}.relatedStaff`)}>
                <Select allowClear showSearch optionFilterProp="label" options={staffSelectOptions} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
