import { useCallback, useEffect, useMemo, useState } from "react";
import { App, DatePicker, Form, InputNumber, Modal, Select, Space, Typography } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import {
  riskCriteriaQuantitativeValueApi,
  type RiskCriteriaQuantitativeValueItem,
  type RiskCriteriaQuantitativeValueRequest,
} from "../../../api/riskScoringExec";
import { auditObjectUnitApi, criteriaQuantitativeApi, type CriteriaQuantitativeItem } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  criteriaId: string;
  branchCode: string;
  year: number;
  entryDate?: dayjs.Dayjs;
  value?: number;
}

/**
 * "Ho so rui ro dinh luong" (sheet ZTC_HSRR - nut "Upload HSRR dinh luong", mau DL_HSRR_Upload) -
 * upload file wide-format (1 dong = 1 chi nhanh/nam, tung cot la 1 ma chi tieu dinh luong). He
 * thong chi ghi nhung o thuoc chi tieu ma user dang upload duoc phan quyen (xem
 * RiskCriteriaQuantitativeValueService), nen so gia tri thuc te ghi duoc co the it hon so o co du
 * lieu trong file. Ngoai Import hang loat, van cho Them/Sua/Xoa/Xuat tung dong nhu moi danh muc
 * khac cua platform - huu ich khi can sua nhanh 1-2 gia tri ma khong phai upload lai ca file.
 */
export function RiskCriteriaQuantitativeValueTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaQuantitativeValueItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<RiskCriteriaQuantitativeValueItem[]>([]);
  const [criteriaOptions, setCriteriaOptions] = useState<CriteriaQuantitativeItem[]>([]);
  const [branchOptions, setBranchOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskCriteriaQuantitativeValueItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskCriteriaQuantitativeValueItem | null>(null);
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
    criteriaQuantitativeApi
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
        setItems(await riskCriteriaQuantitativeValueApi.list(selectedYear));
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
      entryDate: target.entryDate ? dayjs(target.entryDate) : undefined,
      value: target.value ?? undefined,
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
      const request: RiskCriteriaQuantitativeValueRequest = {
        criteriaId: values.criteriaId,
        branchCode: values.branchCode,
        year: values.year,
        entryDate: values.entryDate ? values.entryDate.format("YYYY-MM-DD") : null,
        value: values.value ?? null,
      };
      if (editing) {
        await riskCriteriaQuantitativeValueApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskCriteriaQuantitativeValueApi.create(request);
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

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoringExec.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => riskCriteriaQuantitativeValueApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          if (year != null) await load(year);
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: CrudColumn<RiskCriteriaQuantitativeValueItem>[] = [
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
      title: t("riskScoringExec.hsrr.value"),
      dataIndex: "value",
      width: 130,
      sorter: (a, b) => (a.value ?? 0) - (b.value ?? 0),
      render: (v: number | null) => v ?? "-",
    },
    { title: t("riskScoringExec.hsrr.entryDate"), dataIndex: "entryDate", width: 130, render: (v: string | null) => v ?? "-" },
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
      <CrudTable<RiskCriteriaQuantitativeValueItem>
        tableId="riskScoringExec.hsrrQuantitative"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate && year != null ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport && year != null ? () => riskCriteriaQuantitativeValueApi.exportFile(year, "excel") : undefined}
        onExportWord={canExport && year != null ? () => riskCriteriaQuantitativeValueApi.exportFile(year, "word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaQuantitativeValueApi.importExcel(file);
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
          <Form.Item name="entryDate" label={t("riskScoringExec.hsrr.entryDate")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="value" label={t("riskScoringExec.hsrr.value")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
