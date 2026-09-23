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
import type { AssignmentApprovalStatus } from "../../../api/auditWorkManagement";
import { useAuth } from "../../../auth/AuthContext";
import {
  approvalStatusOptions,
  filterOption,
  fromApiDate,
  renderApprovalStatus,
  renderDate,
  renderDeadlineState,
  renderText,
  toApiDate,
  useTdkpLookups,
} from "./tdkpShared";
import type { TFunction } from "i18next";
import type dayjs from "dayjs";

const PROGRESS_STATUS_VALUES: TdkpStatus[] = ["IN_PROGRESS", "NOT_STARTED", "DONE"];
const progressStatusOptions = (t: TFunction) => PROGRESS_STATUS_VALUES.map((value) => ({ value, label: t(`auditTdkp.resolution.progressStatus.${value}`) }));
const renderProgressStatus = (t: TFunction) => (value: TdkpStatus | null) => (value ? t(`auditTdkp.resolution.progressStatus.${value}`) : "-");

const FOLLOW_UP_GROUP_VALUES = ["QTDH", "TD", "NTD"] as const;
const followUpGroupOptions = (t: TFunction) => FOLLOW_UP_GROUP_VALUES.map((value) => ({ value, label: t(`auditTdkp.resolution.followUpGroup.${value}`) }));

interface FormValues {
  resolutionNumber: string;
  issueDate?: dayjs.Dayjs;
  content?: string;
  workDetail?: string;
  fieldArea?: string;
  unitId?: string;
  contactPerson?: string;
  relatedResolution?: string;
  completionDeadline?: dayjs.Dayjs;
  completionDeadlineBasis?: string;
  implementation?: string;
  progressStatus?: TdkpStatus;
  reason?: string;
  issuanceEvaluation?: string;
  completionDate?: dayjs.Dayjs;
  followUpGroup?: string;
  followerName?: string;
  proposal?: string;
  proposalReason?: string;
  note?: string;
  approvalStatus?: AssignmentApprovalStatus;
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
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));
  const hoUnits = lookups.units.filter((u) => u.unitType === "HO");

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
      workDetail: target.workDetail ?? undefined,
      fieldArea: target.fieldArea ?? undefined,
      unitId: target.unitId ?? undefined,
      contactPerson: target.contactPerson ?? undefined,
      relatedResolution: target.relatedResolution ?? undefined,
      completionDeadline: fromApiDate(target.completionDeadline),
      completionDeadlineBasis: target.completionDeadlineBasis ?? undefined,
      implementation: target.implementation ?? undefined,
      progressStatus: target.progressStatus ?? undefined,
      reason: target.reason ?? undefined,
      issuanceEvaluation: target.issuanceEvaluation ?? undefined,
      completionDate: fromApiDate(target.completionDate),
      followUpGroup: target.followUpGroup ?? undefined,
      followerName: target.followerName ?? undefined,
      proposal: target.proposal ?? undefined,
      proposalReason: target.proposalReason ?? undefined,
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
      const request: TdkpResolutionRequest = {
        resolutionNumber: values.resolutionNumber,
        issueDate: toApiDate(values.issueDate),
        content: values.content ?? null,
        workDetail: values.workDetail ?? null,
        fieldArea: values.fieldArea ?? null,
        unitId: values.unitId ?? null,
        contactPerson: values.contactPerson ?? null,
        relatedResolution: values.relatedResolution ?? null,
        completionDeadline: toApiDate(values.completionDeadline),
        completionDeadlineBasis: values.completionDeadlineBasis ?? null,
        implementation: values.implementation ?? null,
        progressStatus: values.progressStatus ?? null,
        reason: values.reason ?? null,
        issuanceEvaluation: values.issuanceEvaluation ?? null,
        completionDate: toApiDate(values.completionDate),
        followUpGroup: values.followUpGroup ?? null,
        followerName: values.followerName ?? null,
        proposal: values.proposal ?? null,
        proposalReason: values.proposalReason ?? null,
        note: values.note ?? null,
        approvalStatus: values.approvalStatus ?? null,
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
    { title: t(`${c}.workDetail`), dataIndex: "workDetail", width: 240, render: renderText },
    { title: t(`${c}.fieldArea`), dataIndex: "fieldArea", width: 150, render: renderText },
    { title: t(`${c}.unit`), dataIndex: "unitName", width: 180, render: renderText },
    { title: t(`${c}.contactPerson`), dataIndex: "contactPerson", width: 180, render: renderText },
    { title: t(`${c}.relatedResolution`), dataIndex: "relatedResolution", width: 160, render: renderText },
    { title: t(`${c}.completionDeadline`), dataIndex: "completionDeadline", width: 140, render: renderDate },
    { title: t(`${c}.completionDeadlineBasis`), dataIndex: "completionDeadlineBasis", width: 180, render: renderText },
    { title: t(`${c}.implementation`), dataIndex: "implementation", width: 280, render: renderText },
    { title: t(`${c}.progressStatus`), dataIndex: "progressStatus", width: 150, render: renderProgressStatus(t) },
    { title: t(`${c}.reason`), dataIndex: "reason", width: 180, render: renderText },
    { title: t(`${c}.issuanceEvaluation`), dataIndex: "issuanceEvaluation", width: 220, render: renderText },
    { title: t(`${c}.completionDate`), dataIndex: "completionDate", width: 140, render: renderDate },
    { title: t(`${c}.resolutionState`), dataIndex: "resolutionState", width: 130, render: renderDeadlineState(t) },
    {
      title: t(`${c}.followUpGroup`),
      dataIndex: "followUpGroupLabel",
      width: 130,
      render: renderText,
    },
    { title: t(`${c}.followerName`), dataIndex: "followerName", width: 160, render: renderText },
    { title: t(`${c}.proposal`), dataIndex: "proposal", width: 180, render: renderText },
    { title: t(`${c}.proposalReason`), dataIndex: "proposalReason", width: 180, render: renderText },
    { title: t(`${c}.note`), dataIndex: "note", width: 220, render: renderText },
    { title: t(`${c}.approvalStatus`), dataIndex: "approvalStatus", width: 150, render: renderApprovalStatus(t) },
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
        width={840}
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
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="workDetail" label={t(`${c}.workDetail`)}>
                <Input maxLength={255} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="fieldArea" label={t(`${c}.fieldArea`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unitId" label={t(`${c}.unit`)}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={hoUnits.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPerson" label={t(`${c}.contactPerson`)}>
                <Input maxLength={255} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="relatedResolution" label={t(`${c}.relatedResolution`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="completionDeadline" label={t(`${c}.completionDeadline`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="completionDeadlineBasis" label={t(`${c}.completionDeadlineBasis`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="implementation" label={t(`${c}.implementation`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="progressStatus" label={t(`${c}.progressStatus`)}>
                <Select allowClear options={progressStatusOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reason" label={t(`${c}.reason`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="issuanceEvaluation" label={t(`${c}.issuanceEvaluation`)}>
            <Input.TextArea rows={2} maxLength={100} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="completionDate" label={t(`${c}.completionDate`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="followUpGroup" label={t(`${c}.followUpGroup`)}>
                <Select allowClear options={followUpGroupOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="followerName" label={t(`${c}.followerName`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="proposal" label={t(`${c}.proposal`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="proposalReason" label={t(`${c}.proposalReason`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="approvalStatus" label={t(`${c}.approvalStatus`)}>
                <Select allowClear options={approvalStatusOptions(t)} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="note" label={t(`${c}.note`)}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
