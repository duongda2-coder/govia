import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Form, Input, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  riskAssessmentOtherApi,
  riskCriteriaOtherApi,
  riskCriteriaOtherScaleApi,
  type RiskAssessmentOtherHeaderItem,
  type RiskAssessmentOtherHeaderRequest,
  type RiskAssessmentOtherRowItem,
  type RiskCriteriaOtherItem,
  type RiskCriteriaOtherScaleItem,
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

interface FormValues {
  auditObjectCategoryId: string;
  auditObjectCode: string;
  year: number;
  criteriaOtherId: string;
  scaleId: string | null;
  active: boolean;
}

/**
 * Man hinh "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_CDRR_KHAC) - 1 dong
 * danh sach ung voi 1 chi tieu (line) da/can cham diem cua 1 ky (Loai doi tuong KT + Ma doi tuong KT
 * + Nam), dung dinh dang voi file Excel export/import (6 cot). Nhieu dong co the cung 1 ky (header)
 * neu khac chi tieu - header duoc tu dong tao moi/tai su dung dung ky da co khi luu.
 */
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
  const { getSearchColumnProps } = useClientSearchColumn<RiskAssessmentOtherRowItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [rows, setRows] = useState<RiskAssessmentOtherRowItem[]>([]);
  const [headers, setHeaders] = useState<RiskAssessmentOtherHeaderItem[]>([]);
  const [categories, setCategories] = useState<AuditObjectCategoryItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [criteriaList, setCriteriaList] = useState<RiskCriteriaOtherItem[]>([]);
  const [scaleList, setScaleList] = useState<RiskCriteriaOtherScaleItem[]>([]);
  const [objectCodeOptions, setObjectCodeOptions] = useState<{ value: string; label: string }[]>([]);
  const [objectCodeLoading, setObjectCodeLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskAssessmentOtherRowItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskAssessmentOtherRowItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const selectedCategoryId = Form.useWatch("auditObjectCategoryId", form);
  const selectedCriteriaId = Form.useWatch("criteriaOtherId", form);

  const categoryOptions = useMemo(() => categories.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })), [categories]);
  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);
  const criteriaOptions = useMemo(
    () => criteriaList.filter((c) => c.auditObjectCategoryId === selectedCategoryId).map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })),
    [criteriaList, selectedCategoryId],
  );
  const scaleOptions = useMemo(
    () => scaleList.filter((s) => s.criteriaOtherId === selectedCriteriaId).map((s) => ({ value: s.id, label: `${s.scaleScore} - ${s.ratingLevel}` })),
    [scaleList, selectedCriteriaId],
  );
  const selectedCriteriaName = useMemo(() => criteriaList.find((c) => c.id === selectedCriteriaId)?.name ?? "", [criteriaList, selectedCriteriaId]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [rowList, headerList, categoryList, yearList, criteria, scales] = await Promise.all([
        riskAssessmentOtherApi.rows(),
        riskAssessmentOtherApi.list(),
        auditObjectCategoryApi.list(),
        listMasterDataItems("YEAR"),
        riskCriteriaOtherApi.list(),
        riskCriteriaOtherScaleApi.list(),
      ]);
      setRows(rowList);
      setHeaders(headerList);
      setCategories(categoryList);
      setYears(yearList);
      setCriteriaList(criteria);
      setScaleList(scales);
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
      criteriaOtherId: target.criteriaOtherId,
      scaleId: target.scaleId,
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
      const headerRequest: RiskAssessmentOtherHeaderRequest = {
        auditObjectCategoryId: values.auditObjectCategoryId,
        auditObjectCode: values.auditObjectCode,
        year: values.year,
        active: values.active,
      };
      // 1 ky (header) co the co nhieu dong (1 dong/1 chi tieu) - neu ky nay da ton tai (do dong khac
      // cung ky da tao truoc do) thi tai su dung, khong tao moi (se bi loi trung).
      const existingHeader = headers.find(
        (h) => h.auditObjectCategoryId === values.auditObjectCategoryId && h.auditObjectCode === values.auditObjectCode && h.year === values.year,
      );
      const header = existingHeader
        ? await riskAssessmentOtherApi.update(existingHeader.id, headerRequest)
        : await riskAssessmentOtherApi.create(headerRequest);

      const lines = await riskAssessmentOtherApi.lines(header.id);
      const targetLine = lines.find((l) => l.criteriaOtherId === values.criteriaOtherId);
      if (targetLine) {
        await riskAssessmentOtherApi.updateLine(header.id, targetLine.id, { scaleId: values.scaleId ?? null });
      }

      message.success(t(editing ? "riskScoringExec.messages.updateSuccess" : "riskScoringExec.messages.createSuccess"));
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  // Dong (line) luon ton tai san cho moi chi tieu phu hop (ensureLines o backend tu sinh), nen "Xoa"
  // o day chi bo diem da cham ve chua cham chu khong xoa han dong khoi danh sach - dung rieng cau chu
  // "bo diem" (khong dung cac key "riskScoringExec.deleteConfirmTitle"/"deleteSuccess" dung chung cho
  // cac man hinh xoa that su khac trong cung namespace) de tranh gay hieu lam da xoa dong.
  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("riskScoringExec.assessmentOther.clearScoreConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((row) => riskAssessmentOtherApi.updateLine(row.headerId, row.lineId, { scaleId: null })));
          message.success(t("riskScoringExec.assessmentOther.clearScoreSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<RiskAssessmentOtherRowItem>["columns"] = [
    {
      title: t("riskScoring.columns.auditObjectCategory"),
      width: 150,
      ...getSearchColumnProps("auditObjectCategoryCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.assessmentOther.auditObjectCode"),
      dataIndex: "auditObjectCode",
      width: 130,
      ...getSearchColumnProps("auditObjectCode", searchLabels),
    },
    {
      title: t("riskScoringExec.assessmentOther.auditObjectName"),
      ...getSearchColumnProps("auditObjectName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
    {
      title: t("riskScoringExec.columns.criteriaCode"),
      dataIndex: "criteriaOtherCode",
      width: 110,
      ...getSearchColumnProps("criteriaOtherCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.criteriaName"),
      dataIndex: "criteriaOtherName",
      ...getSearchColumnProps("criteriaOtherName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.scaleScore"),
      width: 140,
      render: (_, record) => (record.scaleScore != null ? `${record.scaleScore} - ${record.ratingLevel}` : "-"),
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
    return null;
  }

  return (
    <div>
      <CrudTable<RiskAssessmentOtherRowItem>
        tableId="riskScoringExec.assessmentOther"
        columns={columns}
        dataSource={rows}
        rowKey="lineId"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, selectedRows) => setSelected(selectedRows)}
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
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="auditObjectCategoryId" label={t("riskScoring.columns.auditObjectCategory")} rules={[{ required: true }]}>
            <Select
              options={categoryOptions}
              showSearch
              optionFilterProp="label"
              onChange={() => form.setFieldsValue({ auditObjectCode: undefined, criteriaOtherId: undefined, scaleId: undefined })}
            />
          </Form.Item>
          <Form.Item name="auditObjectCode" label={t("riskScoringExec.assessmentOther.auditObjectCode")} rules={[{ required: true }]}>
            <Select options={objectCodeOptions} loading={objectCodeLoading} showSearch optionFilterProp="label" disabled={!selectedCategoryId} />
          </Form.Item>
          <Form.Item name="year" label={t("riskScoringExec.assessmentOther.year")} rules={[{ required: true }]}>
            <Select options={yearOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="criteriaOtherId" label={t("riskScoringExec.columns.criteriaCode")} rules={[{ required: true }]}>
            <Select
              options={criteriaOptions}
              showSearch
              optionFilterProp="label"
              disabled={!selectedCategoryId}
              placeholder={selectedCategoryId ? undefined : t("riskScoringExec.assessmentOther.selectCategoryFirst")}
              onChange={() => form.setFieldValue("scaleId", undefined)}
            />
          </Form.Item>
          <Form.Item label={t("riskScoringExec.columns.criteriaName")}>
            <Input value={selectedCriteriaName} disabled />
          </Form.Item>
          <Form.Item name="scaleId" label={t("riskScoringExec.columns.scaleScore")}>
            <Select
              options={scaleOptions}
              allowClear
              showSearch
              optionFilterProp="label"
              disabled={!selectedCriteriaId}
              placeholder={t("riskScoringExec.assessmentOther.selectScore")}
            />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
