import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Tabs, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpAssignment,
  deleteTdkpAssignment,
  exportTdkpAssignments,
  listTdkpAssignments,
  updateTdkpAssignment,
  type TdkpAssignmentItem,
  type TdkpAssignmentRequest,
  type TdkpAssignmentScope,
  type TdkpTarget,
} from "../../../api/auditTdkp";
import { useAuth } from "../../../auth/AuthContext";
import { filterOption, fromApiDate, renderDate, renderText, targetOptions, toApiDate, useTdkpLookups } from "./tdkpShared";
import type dayjs from "dayjs";

interface FormValues {
  recommendationTypeId?: string;
  businessSegmentId?: string;
  targetObject?: TdkpTarget;
  auditObjectUnitId?: string;
  geographicAreaId?: string;
  employeeId: string;
  startDate?: dayjs.Dayjs;
  endDate?: dayjs.Dayjs;
}

/** Màn hình "Phân công Theo dõi khắc phục" (ZTC_TDKP_PC): 2 phần - 1. kiến nghị đối với HĐTV/TGĐ, 2. kiến nghị đối với Chi nhánh. */
export function TdkpAssignmentPage() {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  if (!hasPermission("AUDIT.TDKP_PC.VIEW")) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }
  return (
    <div>
      <Typography.Title level={4}>{t("auditTdkp.assignment.title")}</Typography.Title>
      <Tabs
        items={[
          { key: "CEO", label: t("auditTdkp.assignment.tabs.ceo"), children: <AssignmentTable scope="CEO" /> },
          { key: "BRANCH", label: t("auditTdkp.assignment.tabs.branch"), children: <AssignmentTable scope="BRANCH" /> },
        ]}
      />
    </div>
  );
}

function AssignmentTable({ scope }: { scope: TdkpAssignmentScope }) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission("AUDIT.TDKP_PC.CREATE");
  const canEdit = hasPermission("AUDIT.TDKP_PC.EDIT");
  const canDelete = hasPermission("AUDIT.TDKP_PC.DELETE");
  const canExport = hasPermission("AUDIT.TDKP_PC.EXPORT");
  const { getSearchColumnProps } = useClientSearchColumn<TdkpAssignmentItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));
  const isCeo = scope === "CEO";

  const [items, setItems] = useState<TdkpAssignmentItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<TdkpAssignmentItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpAssignmentItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const employeeId = Form.useWatch("employeeId", form);
  const selectedEmployee = lookups.employees.find((e) => e.id === employeeId);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listTdkpAssignments(scope));
    } catch {
      message.error(t("auditTdkp.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, scope, t]);

  useEffect(() => {
    load();
  }, [load]);

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
      recommendationTypeId: target.recommendationTypeId ?? undefined,
      businessSegmentId: target.businessSegmentId ?? undefined,
      targetObject: target.targetObject ?? undefined,
      auditObjectUnitId: target.auditObjectUnitId ?? undefined,
      geographicAreaId: target.geographicAreaId ?? undefined,
      employeeId: target.employeeId,
      startDate: fromApiDate(target.startDate),
      endDate: fromApiDate(target.endDate),
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
      const request: TdkpAssignmentRequest = {
        scope,
        recommendationTypeId: values.recommendationTypeId ?? null,
        businessSegmentId: values.businessSegmentId ?? null,
        targetObject: isCeo ? (values.targetObject ?? null) : null,
        auditObjectUnitId: values.auditObjectUnitId ?? null,
        geographicAreaId: values.geographicAreaId ?? null,
        employeeId: values.employeeId,
        startDate: toApiDate(values.startDate),
        endDate: toApiDate(values.endDate),
      };
      if (editing) {
        await updateTdkpAssignment(editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        await createTdkpAssignment(request);
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
          await Promise.all(selected.map((item) => deleteTdkpAssignment(item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const employeeColumns: TableProps<TdkpAssignmentItem>["columns"] = [
    {
      title: t("auditTdkp.assignment.columns.employee"),
      key: "employee",
      width: 220,
      render: (_: unknown, row) => (row.employeeName ? `${row.employeeCode ?? ""} - ${row.employeeName}` : "-"),
    },
    { title: t("auditTdkp.assignment.columns.username"), dataIndex: "username", width: 140, render: renderText },
    { title: t("auditTdkp.assignment.columns.department"), dataIndex: "departmentName", width: 160, render: renderText },
    { title: t("auditTdkp.assignment.columns.startDate"), dataIndex: "startDate", width: 120, render: renderDate },
    { title: t("auditTdkp.assignment.columns.endDate"), dataIndex: "endDate", width: 120, render: renderDate },
  ];
  const columns: TableProps<TdkpAssignmentItem>["columns"] = isCeo
    ? [
        { title: t("auditTdkp.assignment.columns.recommendationType"), dataIndex: "recommendationTypeName", width: 200, render: renderText },
        { title: t("auditTdkp.assignment.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 130, render: renderText },
        { title: t("auditTdkp.assignment.columns.targetObject"), dataIndex: "targetObjectLabel", width: 150, render: renderText },
        { title: t("auditTdkp.assignment.columns.auditObjectCode"), dataIndex: "auditObjectUnitCode", width: 150, render: renderText },
        { title: t("auditTdkp.assignment.columns.geographicArea"), dataIndex: "geographicAreaName", width: 150, render: renderText },
        ...(employeeColumns ?? []),
      ]
    : [
        { title: t("auditTdkp.assignment.columns.auditObject"), width: 220, ...getSearchColumnProps("auditObjectUnitName", searchLabels) },
        { title: t("auditTdkp.assignment.columns.auditObjectCodeBranch"), dataIndex: "auditObjectUnitCode", width: 150, render: renderText },
        { title: t("auditTdkp.assignment.columns.recommendationType"), dataIndex: "recommendationTypeName", width: 200, render: renderText },
        { title: t("auditTdkp.assignment.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 130, render: renderText },
        { title: t("auditTdkp.assignment.columns.geographicArea"), dataIndex: "geographicAreaName", width: 150, render: renderText },
        ...(employeeColumns ?? []),
      ];

  return (
    <>
      <CrudTable<TdkpAssignmentItem>
        tableId={`audit.tdkp.assignment.${scope.toLowerCase()}`}
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
        onExportExcel={canExport ? () => exportTdkpAssignments(scope) : undefined}
      />

      <Modal
        title={editing ? t("auditTdkp.assignment.form.editTitle") : t("auditTdkp.assignment.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            {isCeo ? (
              <Col span={12}>
                <Form.Item name="targetObject" label={t("auditTdkp.assignment.columns.targetObject")} rules={[{ required: true }]}>
                  <Select options={targetOptions(t)} />
                </Form.Item>
              </Col>
            ) : (
              <Col span={12}>
                <Form.Item name="auditObjectUnitId" label={t("auditTdkp.assignment.columns.auditObject")} rules={[{ required: true }]}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    filterOption={filterOption}
                    options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                  />
                </Form.Item>
              </Col>
            )}
            {isCeo && (
              <Col span={12}>
                <Form.Item name="auditObjectUnitId" label={t("auditTdkp.assignment.columns.auditObjectCode")}>
                  <Select
                    allowClear
                    showSearch
                    optionFilterProp="label"
                    filterOption={filterOption}
                    options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                  />
                </Form.Item>
              </Col>
            )}
            <Col span={12}>
              <Form.Item name="recommendationTypeId" label={t("auditTdkp.assignment.columns.recommendationType")}>
                <Select allowClear options={lookups.recommendationTypes.map((o) => ({ value: o.id, label: o.name }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditTdkp.assignment.columns.businessSegment")}>
                <Select allowClear showSearch optionFilterProp="label" options={lookups.businessSegments.map((o) => ({ value: o.id, label: `${o.code} - ${o.name}` }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="geographicAreaId" label={t("auditTdkp.assignment.columns.geographicArea")}>
                <Select allowClear options={lookups.geographicAreas.map((o) => ({ value: o.id, label: o.name }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="employeeId" label={t("auditTdkp.assignment.columns.employee")} rules={[{ required: true }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.employees.map((e) => ({ value: e.id, label: `${e.code} - ${e.name}` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label={t("auditTdkp.assignment.columns.username")}>
                <Input readOnly value={selectedEmployee?.username ?? ""} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label={t("auditTdkp.assignment.columns.department")}>
                <Input readOnly value={selectedEmployee?.departmentName ?? ""} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="startDate" label={t("auditTdkp.assignment.columns.startDate")}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="endDate"
                label={t("auditTdkp.assignment.columns.endDate")}
                dependencies={["startDate"]}
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value: dayjs.Dayjs | undefined) {
                      const start = getFieldValue("startDate") as dayjs.Dayjs | undefined;
                      if (!value || !start || !value.isBefore(start, "day")) return Promise.resolve();
                      return Promise.reject(new Error(t("auditTdkp.assignment.form.endBeforeStart")));
                    },
                  }),
                ]}
              >
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  );
}
