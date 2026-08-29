import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Form, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { FormOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  riskAssessmentOtherApi,
  type RiskAssessmentOtherHeaderItem,
  type RiskAssessmentOtherHeaderRequest,
} from "../../../api/riskScoringExec";
import {
  auditObjectCategoryApi,
  auditObjectProcessApi,
  auditObjectProjectApi,
  auditObjectSubsidiaryApi,
  auditObjectUnitApi,
  type AuditObjectCategoryItem,
  type AuditObjectSource,
} from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";
import { RiskAssessmentOtherLineDrawer } from "./RiskAssessmentOtherLineDrawer";

interface FormValues {
  auditObjectCategoryId: string;
  auditObjectCode: string;
  year: number;
  active: boolean;
}

/** Man hinh "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_CDRR_KHAC) - header. */
export function RiskAssessmentOtherTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskAssessmentOtherHeaderItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<RiskAssessmentOtherHeaderItem[]>([]);
  const [categories, setCategories] = useState<AuditObjectCategoryItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [objectCodeOptions, setObjectCodeOptions] = useState<{ value: string; label: string }[]>([]);
  const [objectCodeLoading, setObjectCodeLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskAssessmentOtherHeaderItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskAssessmentOtherHeaderItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [scoringTarget, setScoringTarget] = useState<RiskAssessmentOtherHeaderItem | null>(null);
  const [form] = Form.useForm<FormValues>();
  const selectedCategoryId = Form.useWatch("auditObjectCategoryId", form);

  const categoryOptions = useMemo(() => categories.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })), [categories]);
  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, categoryList, yearList] = await Promise.all([
        riskAssessmentOtherApi.list(),
        auditObjectCategoryApi.list(),
        listMasterDataItems("YEAR"),
      ]);
      setItems(list);
      setCategories(categoryList);
      setYears(yearList);
    } catch {
      message.error(t("riskScoringExec.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  // "Ma doi tuong KT" tro toi 1 trong 4 danh muc tuy theo objectSource cua Loai doi tuong KT da chon
  // (NSD tu khai bao o man hinh danh muc goc, xem AuditObjectCategoryTable) - KHONG con hard-code
  // theo dung ma "HO"/"CTC"/"KTQT" nhu truoc, vi NSD co the tao category moi voi ma bat ky.
  useEffect(() => {
    const category = categories.find((c) => c.id === selectedCategoryId);
    if (!category) {
      setObjectCodeOptions([]);
      return;
    }
    setObjectCodeLoading(true);
    const apiByObjectSource: Record<AuditObjectSource, { list(): Promise<{ code: string; name: string }[]> }> = {
      UNIT: auditObjectUnitApi,
      SUBSIDIARY: auditObjectSubsidiaryApi,
      PROCESS: auditObjectProcessApi,
      PROJECT: auditObjectProjectApi,
    };
    apiByObjectSource[category.objectSource]
      .list()
      .then((list) => setObjectCodeOptions(list.map((o) => ({ value: o.code, label: `${o.code} - ${o.name}` }))))
      .catch(() => setObjectCodeOptions([]))
      .finally(() => setObjectCodeLoading(false));
  }, [selectedCategoryId, categories]);

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
      auditObjectCategoryId: target.auditObjectCategoryId,
      auditObjectCode: target.auditObjectCode,
      year: target.year,
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
      const request: RiskAssessmentOtherHeaderRequest = {
        auditObjectCategoryId: values.auditObjectCategoryId,
        auditObjectCode: values.auditObjectCode,
        year: values.year,
        active: values.active,
      };
      if (editing) {
        await riskAssessmentOtherApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskAssessmentOtherApi.create(request);
        message.success(t("riskScoringExec.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoringExec.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => riskAssessmentOtherApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<RiskAssessmentOtherHeaderItem>["columns"] = [
    { title: t("riskScoring.columns.auditObjectCategory"), dataIndex: "auditObjectCategoryCode", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.assessmentOther.auditObjectCode"), dataIndex: "auditObjectCode", width: 140, ...getSearchColumnProps("auditObjectCode", searchLabels) },
    { title: t("riskScoringExec.assessmentOther.auditObjectName"), dataIndex: "auditObjectName", render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 100, sorter: (a, b) => a.year - b.year },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 110,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
    {
      title: t("riskScoringExec.assessmentOther.scoreLines"),
      key: "score",
      width: 130,
      align: "center",
      render: (_, record) => (
        <Button type="text" size="small" icon={<FormOutlined style={{ fontSize: 16 }} />} onClick={() => setScoringTarget(record)} />
      ),
    },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <CrudTable<RiskAssessmentOtherHeaderItem>
        tableId="riskScoringExec.assessmentOther"
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
        onExportExcel={canExport ? () => riskAssessmentOtherApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => riskAssessmentOtherApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskAssessmentOtherApi.importExcel(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("riskScoringExec.form.editTitle") : t("riskScoringExec.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues>
          form={form}
          layout="vertical"
          onValuesChange={(changed) => {
            if (changed.auditObjectCategoryId) {
              form.setFieldValue("auditObjectCode", undefined);
            }
          }}
        >
          <Form.Item name="auditObjectCategoryId" label={t("riskScoring.columns.auditObjectCategory")} rules={[{ required: true }]}>
            <Select options={categoryOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="auditObjectCode" label={t("riskScoringExec.assessmentOther.auditObjectCode")} rules={[{ required: true }]}>
            <Select options={objectCodeOptions} loading={objectCodeLoading} showSearch optionFilterProp="label" disabled={!selectedCategoryId} />
          </Form.Item>
          <Form.Item name="year" label={t("riskScoringExec.assessmentOther.year")} rules={[{ required: true }]}>
            <Select options={yearOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <RiskAssessmentOtherLineDrawer open={!!scoringTarget} header={scoringTarget} onClose={() => setScoringTarget(null)} />
    </div>
  );
}
