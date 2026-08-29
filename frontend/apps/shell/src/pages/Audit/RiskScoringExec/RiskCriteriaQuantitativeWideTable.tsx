import { useCallback, useEffect, useMemo, useState } from "react";
import { App, DatePicker, Form, InputNumber, Modal, Select, Space, Typography } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import {
  riskCriteriaQuantitativeValueApi,
  type RiskCriteriaQuantitativeWideRowItem,
  type RiskCriteriaQuantitativeWideRowRequest,
} from "../../../api/riskScoringExec";
import { auditObjectUnitApi, criteriaQuantitativeApi, type CriteriaQuantitativeItem } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  entryDate?: dayjs.Dayjs;
  values: Record<string, number | null | undefined>;
}

/**
 * Ban "bang tong hop" (wide-format) cua Ho so rui ro dinh luong - 1 dong = 1 chi nhanh/nam, tung
 * chi tieu dinh luong la 1 cot rieng (dung dinh dang voi sheet DL_Nhaptructiep / mau DL_HSRR_Upload
 * cua file "2. Cham diem (1).xlsx"), khac voi RiskCriteriaQuantitativeValueTable (long-format, 1
 * dong/1 chi tieu) - man do van giu song song de sua nhanh 1-2 gia tri, Import/Export/Xoa tung dong.
 * Sua 1 dong o day mo modal voi tat ca chi tieu (nhom theo Ma nhom) roi luu 1 lan qua
 * PUT .../hsrr/quantitative/wide (upsert tung chi tieu, gia tri de trong = xoa neu da co).
 */
export function RiskCriteriaQuantitativeWideTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaQuantitativeWideRowItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<RiskCriteriaQuantitativeWideRowItem[]>([]);
  const [criteriaList, setCriteriaList] = useState<CriteriaQuantitativeItem[]>([]);
  const [branchOptions, setBranchOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskCriteriaQuantitativeWideRowItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskCriteriaQuantitativeWideRowItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
    criteriaQuantitativeApi
      .list()
      .then(setCriteriaList)
      .catch(() => setCriteriaList([]));
    auditObjectUnitApi
      .list()
      .then((list) => setBranchOptions(list.map((u) => ({ value: u.code, label: `${u.code} - ${u.name}` }))))
      .catch(() => setBranchOptions([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setRows(await riskCriteriaQuantitativeValueApi.listWide(selectedYear));
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

  const groupedCriteria = useMemo(() => {
    const groups = new Map<string, CriteriaQuantitativeItem[]>();
    for (const c of criteriaList) {
      const key = c.group1Code ?? "";
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key)?.push(c);
    }
    return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [criteriaList]);

  const openCreate = () => {
    if (year == null) {
      message.warning(t("riskScoringExec.hsrr.selectYearFirst"));
      return;
    }
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      branchCode: target.branchCode,
      entryDate: target.entryDate ? dayjs(target.entryDate) : undefined,
      values: { ...target.valuesByCriteriaCode },
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (year == null) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request: RiskCriteriaQuantitativeWideRowRequest = {
        branchCode: values.branchCode,
        year,
        entryDate: values.entryDate ? values.entryDate.format("YYYY-MM-DD") : null,
        valuesByCriteriaCode: Object.fromEntries(criteriaList.map((c) => [c.code, values.values?.[c.code] ?? null])),
      };
      await riskCriteriaQuantitativeValueApi.saveWideRow(request);
      message.success(t(editing ? "riskScoringExec.messages.updateSuccess" : "riskScoringExec.messages.createSuccess"));
      setModalOpen(false);
      setSelected([]);
      await load(year);
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: CrudColumn<RiskCriteriaQuantitativeWideRowItem>[] = useMemo(() => {
    const fixed: CrudColumn<RiskCriteriaQuantitativeWideRowItem>[] = [
      { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
      { title: t("riskScoringExec.hsrr.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
      {
        title: t("riskScoringExec.hsrr.branchName"),
        width: 200,
        ...getSearchColumnProps("branchName", searchLabels),
        render: (v: string | null) => v ?? "-",
      },
      { title: t("riskScoringExec.hsrr.entryDate"), dataIndex: "entryDate", width: 110, render: (v: string | null) => v ?? "-" },
    ];
    const dynamic: CrudColumn<RiskCriteriaQuantitativeWideRowItem>[] = criteriaList.map((c) => ({
      title: c.code,
      width: 90,
      render: (_: unknown, record: RiskCriteriaQuantitativeWideRowItem) => record.valuesByCriteriaCode[c.code] ?? "-",
    }));
    return [...fixed, ...dynamic];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [criteriaList, t]);

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
      <CrudTable<RiskCriteriaQuantitativeWideRowItem>
        tableId="riskScoringExec.hsrrQuantitativeWide"
        columns={columns}
        dataSource={rows}
        rowKey="branchCode"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onSelectionChange={(_keys, selectedRows) => setSelected(selectedRows)}
      />

      <Modal
        title={editing ? t("riskScoringExec.form.editTitle") : t("riskScoringExec.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={760}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Space.Compact block style={{ marginBottom: 8 }}>
            <Form.Item name="branchCode" label={t("riskScoringExec.hsrr.branchCode")} rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <Select options={branchOptions} showSearch optionFilterProp="label" disabled={!!editing} />
            </Form.Item>
            <Form.Item name="entryDate" label={t("riskScoringExec.hsrr.entryDate")} style={{ flex: 1 }}>
              <DatePicker style={{ width: "100%" }} />
            </Form.Item>
          </Space.Compact>
          <div style={{ maxHeight: 420, overflowY: "auto", paddingRight: 8 }}>
            {groupedCriteria.map(([groupCode, items]) => (
              <div key={groupCode || "_"} style={{ marginBottom: 12 }}>
                {groupCode && (
                  <Typography.Text strong>
                    {groupCode}
                  </Typography.Text>
                )}
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: 8, marginTop: 4 }}>
                  {items.map((c) => (
                    <Form.Item key={c.id} name={["values", c.code]} label={c.code} style={{ marginBottom: 8 }} tooltip={c.name}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Form>
      </Modal>
    </div>
  );
}
