import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, DatePicker, Form, Input, Modal, Select, Space, Typography } from "antd";
import { SyncOutlined, ReloadOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import {
  riskAssessmentOtherExpertRankApi,
  type RiskAssessmentOtherExpertRankItem,
  type RiskAssessmentOtherExpertRankRequest,
} from "../../../api/riskScoringExec";
import { rankApi } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  reRankLabel?: string;
  reason?: string;
  assessedDate?: dayjs.Dayjs;
  expertName?: string;
  finalRankLabel?: string;
}

/**
 * "Xep hang rui ro theo y kien chuyen gia cua DTKT khac" (sheet ZTC_XHRR_KHAC_CG) - 2 thao tac
 * theo dung tai lieu goc: "Cap nhat du lieu tu nguon" (keo lai Diem rui ro/Xep loai tu Bang xep
 * hang, khong dung vao du lieu chuyen gia da nhap) va "Xem ket qua" (tai lai danh sach da luu).
 * Sua tung dong (Xep hang lai theo YKCG, Ly do, Ngay danh gia, Chuyen gia, Ket qua xep loai) qua
 * modal - khong co Them/Xoa vi vong doi dong do "Cap nhat du lieu tu nguon" quan ly.
 */
export function RiskAssessmentOtherExpertRankTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canSync = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskAssessmentOtherExpertRankItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rankLabels, setRankLabels] = useState<string[]>([]);
  const [items, setItems] = useState<RiskAssessmentOtherExpertRankItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [selected, setSelected] = useState<RiskAssessmentOtherExpertRankItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskAssessmentOtherExpertRankItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);
  const rankOptions = useMemo(() => rankLabels.map((label) => ({ value: label, label })), [rankLabels]);

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
    rankApi
      .list()
      .then((list) => setRankLabels([...new Set(list.map((r) => r.rankLabel))]))
      .catch(() => setRankLabels([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setItems(await riskAssessmentOtherExpertRankApi.list(selectedYear));
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

  const handleSync = async () => {
    if (year == null) return;
    setSyncing(true);
    try {
      const list = await riskAssessmentOtherExpertRankApi.sync(year);
      setItems(list);
      message.success(t("riskScoringExec.expertRank.syncSuccess"));
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSyncing(false);
    }
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      reRankLabel: target.reRankLabel ?? undefined,
      reason: target.reason ?? undefined,
      assessedDate: target.assessedDate ? dayjs(target.assessedDate) : undefined,
      expertName: target.expertName ?? undefined,
      finalRankLabel: target.finalRankLabel ?? undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!editing) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request: RiskAssessmentOtherExpertRankRequest = {
        reRankLabel: values.reRankLabel ?? null,
        reason: values.reason ?? null,
        assessedDate: values.assessedDate ? values.assessedDate.format("YYYY-MM-DD") : null,
        expertName: values.expertName ?? null,
        finalRankLabel: values.finalRankLabel ?? null,
      };
      const updated = await riskAssessmentOtherExpertRankApi.update(editing.id, request);
      setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
      message.success(t("riskScoringExec.messages.updateSuccess"));
      setModalOpen(false);
      setSelected([]);
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: CrudColumn<RiskAssessmentOtherExpertRankItem>[] = [
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
    {
      title: t("riskScoring.columns.auditObjectCategory"),
      width: 120,
      ...getSearchColumnProps("auditObjectCategoryCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.assessmentOther.auditObjectCode"), width: 130, ...getSearchColumnProps("auditObjectCode", searchLabels) },
    {
      title: t("riskScoringExec.assessmentOther.auditObjectName"),
      ...getSearchColumnProps("auditObjectName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.ranking.riskScore"),
      dataIndex: "riskScore",
      width: 110,
      sorter: (a, b) => (a.riskScore ?? 0) - (b.riskScore ?? 0),
      render: (v: number | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.ranking.rankLabel"),
      width: 110,
      ...getSearchColumnProps("baseRankLabel", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.expertRank.reRankLabel"),
      width: 140,
      ...getSearchColumnProps("reRankLabel", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.expertRank.reason"), dataIndex: "reason", render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.expertRank.assessedDate"), dataIndex: "assessedDate", width: 130, render: (v: string | null) => v ?? "-" },
    {
      title: t("riskScoringExec.expertRank.expertName"),
      width: 160,
      ...getSearchColumnProps("expertName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.expertRank.finalRankLabel"),
      width: 140,
      ...getSearchColumnProps("finalRankLabel", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.expertRank.updatedBy"), dataIndex: "updatedBy", width: 130, defaultHidden: true, render: (v: string | null) => v ?? "-" },
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
        <Button icon={<SyncOutlined />} loading={syncing} disabled={!canSync || year == null} onClick={handleSync}>
          {t("riskScoringExec.expertRank.syncFromSource")}
        </Button>
        <Button icon={<ReloadOutlined />} disabled={year == null} onClick={() => year != null && load(year)}>
          {t("riskScoringExec.expertRank.viewResults")}
        </Button>
      </Space>
      <CrudTable<RiskAssessmentOtherExpertRankItem>
        tableId="riskScoringExec.assessmentOtherExpertRank"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <Modal
        title={t("riskScoringExec.expertRank.editTitle")}
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
            if (changed.reRankLabel !== undefined && !form.isFieldTouched("finalRankLabel")) {
              form.setFieldValue("finalRankLabel", changed.reRankLabel);
            }
          }}
        >
          <Form.Item name="reRankLabel" label={t("riskScoringExec.expertRank.reRankLabel")}>
            <Select allowClear options={rankOptions} />
          </Form.Item>
          <Form.Item name="reason" label={t("riskScoringExec.expertRank.reason")}>
            <Input.TextArea rows={2} maxLength={125} />
          </Form.Item>
          <Form.Item name="assessedDate" label={t("riskScoringExec.expertRank.assessedDate")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="expertName" label={t("riskScoringExec.expertRank.expertName")}>
            <Input maxLength={125} />
          </Form.Item>
          <Form.Item name="finalRankLabel" label={t("riskScoringExec.expertRank.finalRankLabel")}>
            <Select allowClear options={rankOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
