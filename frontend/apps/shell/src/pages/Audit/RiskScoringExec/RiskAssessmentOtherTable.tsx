import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Form, Modal, Select, Switch, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  riskAssessmentOtherApi,
  riskCriteriaOtherApi,
  riskCriteriaOtherScaleApi,
  type RiskAssessmentOtherHeaderItem,
  type RiskAssessmentOtherHeaderRequest,
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
  active: boolean;
}

interface LineRow {
  lineId: string | null;
  criteriaOtherId: string;
  criteriaOtherCode: string | null;
  criteriaOtherName: string;
  scaleId: string | null;
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
  const [criteriaList, setCriteriaList] = useState<RiskCriteriaOtherItem[]>([]);
  const [scaleList, setScaleList] = useState<RiskCriteriaOtherScaleItem[]>([]);
  const [objectCodeOptions, setObjectCodeOptions] = useState<{ value: string; label: string }[]>([]);
  const [objectCodeLoading, setObjectCodeLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskAssessmentOtherHeaderItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskAssessmentOtherHeaderItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [lineRows, setLineRows] = useState<LineRow[]>([]);
  const [linesLoading, setLinesLoading] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const selectedCategoryId = Form.useWatch("auditObjectCategoryId", form);

  const categoryOptions = useMemo(() => categories.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })), [categories]);
  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, categoryList, yearList, criteria, scales] = await Promise.all([
        riskAssessmentOtherApi.list(),
        auditObjectCategoryApi.list(),
        listMasterDataItems("YEAR"),
        riskCriteriaOtherApi.list(),
        riskCriteriaOtherScaleApi.list(),
      ]);
      setItems(list);
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

  // Bang "Cham diem chi tieu" ben trong modal: neu dang sua va category chua doi, dung dung cac
  // dong da co san (kem diem da cham) - nguoc lai (them moi, hoac doi sang category khac) chi hien
  // truoc danh sach chi tieu phu hop de NSD cham diem ngay, dong that se duoc tao khi bam Luu.
  useEffect(() => {
    if (!modalOpen || !selectedCategoryId) {
      setLineRows([]);
      return;
    }
    if (editing && editing.auditObjectCategoryId === selectedCategoryId) {
      setLinesLoading(true);
      riskAssessmentOtherApi
        .lines(editing.id)
        .then((lines) =>
          setLineRows(
            lines.map((l) => ({
              lineId: l.id,
              criteriaOtherId: l.criteriaOtherId,
              criteriaOtherCode: l.criteriaOtherCode,
              criteriaOtherName: l.criteriaOtherName ?? "",
              scaleId: l.scaleId,
            })),
          ),
        )
        .catch(() => message.error(t("riskScoringExec.messages.loadError")))
        .finally(() => setLinesLoading(false));
      return;
    }
    setLineRows(
      criteriaList
        .filter((c) => c.auditObjectCategoryId === selectedCategoryId)
        .map((c) => ({ lineId: null, criteriaOtherId: c.id, criteriaOtherCode: c.code, criteriaOtherName: c.name, scaleId: null })),
    );
  }, [modalOpen, selectedCategoryId, editing, criteriaList, message, t]);

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

  const handleScoreChange = (criteriaOtherId: string, scaleId: string | null) => {
    setLineRows((prev) => prev.map((row) => (row.criteriaOtherId === criteriaOtherId ? { ...row, scaleId } : row)));
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
      const header = editing ? await riskAssessmentOtherApi.update(editing.id, request) : await riskAssessmentOtherApi.create(request);

      // He thong tu sinh du dong chi tieu ngay khi tao/cap nhat header (xem ensureLines o backend) -
      // doi chieu lai voi diem NSD da chon o bang preview de cap nhat dung dong tuong ung.
      const realLines = await riskAssessmentOtherApi.lines(header.id);
      const scoreByCriteria = new Map(lineRows.map((row) => [row.criteriaOtherId, row.scaleId]));
      await Promise.all(
        realLines
          .filter((line) => scoreByCriteria.get(line.criteriaOtherId) !== undefined && scoreByCriteria.get(line.criteriaOtherId) !== line.scaleId)
          .map((line) => riskAssessmentOtherApi.updateLine(header.id, line.id, { scaleId: scoreByCriteria.get(line.criteriaOtherId) ?? null })),
      );

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
    {
      title: t("riskScoring.columns.auditObjectCategory"),
      width: 160,
      ...getSearchColumnProps("auditObjectCategoryCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.assessmentOther.auditObjectCode"), dataIndex: "auditObjectCode", width: 140, ...getSearchColumnProps("auditObjectCode", searchLabels) },
    {
      title: t("riskScoringExec.assessmentOther.auditObjectName"),
      ...getSearchColumnProps("auditObjectName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 100, sorter: (a, b) => a.year - b.year },
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
        width={760}
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

        <Typography.Title level={5} style={{ marginTop: 8 }}>
          {t("riskScoringExec.assessmentOther.scoreLines")}
        </Typography.Title>
        {selectedCategoryId ? (
          <Table<LineRow>
            rowKey="criteriaOtherId"
            size="small"
            loading={linesLoading}
            dataSource={lineRows}
            pagination={false}
            columns={[
              { title: t("riskScoringExec.columns.criteriaCode"), dataIndex: "criteriaOtherCode", width: 100, render: (v: string | null) => v ?? "-" },
              { title: t("riskScoringExec.columns.criteriaName"), dataIndex: "criteriaOtherName" },
              {
                title: t("riskScoringExec.columns.scaleScore"),
                width: 240,
                render: (_: unknown, row: LineRow) => {
                  const options = scaleList
                    .filter((s) => s.criteriaOtherId === row.criteriaOtherId)
                    .map((s) => ({ value: s.id, label: `${s.scaleScore} - ${s.ratingLevel}` }));
                  return (
                    <Select
                      style={{ width: "100%" }}
                      allowClear
                      placeholder={t("riskScoringExec.assessmentOther.selectScore")}
                      value={row.scaleId ?? undefined}
                      options={options}
                      onChange={(value) => handleScoreChange(row.criteriaOtherId, value ?? null)}
                    />
                  );
                },
              },
            ]}
          />
        ) : (
          <Typography.Text type="secondary">{t("riskScoringExec.assessmentOther.selectCategoryFirst")}</Typography.Text>
        )}
      </Modal>
    </div>
  );
}
