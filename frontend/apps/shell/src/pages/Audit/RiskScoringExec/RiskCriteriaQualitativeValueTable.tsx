import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import {
  riskCriteriaQualitativeValueApi,
  type RiskCriteriaQualitativeValueItem,
  type RiskCriteriaQualitativeValueRequest,
} from "../../../api/riskScoringExec";
import { auditObjectUnitApi, criteriaQualitativeApi, type CriteriaQualitativeItem } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  criteriaId: string;
  branchCode: string;
  year: number;
  violation?: string;
  note?: string;
}

/**
 * "Ho so rui ro dinh tinh" (sheet ZTC_HSRR - nut "Upload HSRR dinh tinh", mau DT_HSRR_Upload) -
 * upload file long-format (1 dong = 1 chi tieu/chi nhanh/nam), xem RiskCriteriaQualitativeValueService.
 * Ngoai Import hang loat, van cho Them/Sua/Xoa/Xuat tung dong nhu moi danh muc khac cua platform -
 * huu ich khi can sua nhanh 1-2 gia tri ma khong phai upload lai ca file.
 */
export function RiskCriteriaQualitativeValueTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaQualitativeValueItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<RiskCriteriaQualitativeValueItem[]>([]);
  const [criteriaOptions, setCriteriaOptions] = useState<CriteriaQualitativeItem[]>([]);
  const [branchOptions, setBranchOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskCriteriaQualitativeValueItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskCriteriaQualitativeValueItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);
  const criteriaSelectOptions = useMemo(
    () => criteriaOptions.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })),
    [criteriaOptions],
  );

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
    criteriaQualitativeApi
      .list()
      .then(setCriteriaOptions)
      .catch(() => setCriteriaOptions([]));
    auditObjectUnitApi
      .list()
      .then((list) => setBranchOptions(list.map((u) => ({ value: u.code, label: `${u.code} - ${u.name}` }))))
      .catch(() => setBranchOptions([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setItems(await riskCriteriaQualitativeValueApi.list(selectedYear));
      } catch {
        message.error(t("riskScoringExec.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView && year != null) load(year);
  }, [canView, year, load]);

  const openCreate = () => {
    if (year == null) {
      message.warning(t("riskScoringExec.hsrr.selectYearFirst"));
      return;
    }
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ year });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      criteriaId: target.criteriaId,
      branchCode: target.branchCode,
      year: target.year,
      violation: target.violation ?? undefined,
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
      const request: RiskCriteriaQualitativeValueRequest = {
        criteriaId: values.criteriaId,
        branchCode: values.branchCode,
        year: values.year,
        violation: values.violation || null,
        note: values.note || null,
      };
      if (editing) {
        await riskCriteriaQualitativeValueApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskCriteriaQualitativeValueApi.create(request);
        message.success(t("riskScoringExec.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      if (year != null) await load(year);
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleExport = (kind: "excel" | "word") => {
    if (year == null) {
      message.warning(t("riskScoringExec.hsrr.selectYearFirst"));
      return Promise.resolve();
    }
    return riskCriteriaQualitativeValueApi.exportFile(year, kind);
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
          await Promise.all(selected.map((item) => riskCriteriaQualitativeValueApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          if (year != null) await load(year);
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: CrudColumn<RiskCriteriaQualitativeValueItem>[] = [
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
    { title: t("riskScoringExec.hsrr.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    {
      title: t("riskScoringExec.hsrr.branchName"),
      ...getSearchColumnProps("branchName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.criteriaCode"),
      width: 110,
      ...getSearchColumnProps("criteriaCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.criteriaName"),
      ...getSearchColumnProps("criteriaName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.hsrr.violation"),
      width: 110,
      ...getSearchColumnProps("violation", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.hsrr.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t("riskScoringExec.assessmentOther.year")}</Typography.Text>
        <Select
          style={{ width: 160 }}
          options={yearOptions}
          value={year}
          onChange={setYear}
          showSearch
          placeholder={t("riskScoringExec.ranking.selectYear")}
        />
      </Space>
      <CrudTable<RiskCriteriaQualitativeValueItem>
        tableId="riskScoringExec.hsrrQualitative"
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
        onExportExcel={canExport ? () => handleExport("excel") : undefined}
        onExportWord={canExport ? () => handleExport("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaQualitativeValueApi.importExcel(file);
                if (year != null) await load(year);
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
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="criteriaId" label={t("riskScoringExec.columns.criteriaCode")} rules={[{ required: true }]}>
            <Select options={criteriaSelectOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="branchCode" label={t("riskScoringExec.hsrr.branchCode")} rules={[{ required: true }]}>
            <Select options={branchOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="year" label={t("riskScoringExec.assessmentOther.year")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
          </Form.Item>
          <Form.Item name="violation" label={t("riskScoringExec.hsrr.violation")}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="note" label={t("riskScoringExec.hsrr.note")}>
            <Input.TextArea rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
