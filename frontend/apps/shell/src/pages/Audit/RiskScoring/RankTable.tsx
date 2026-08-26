import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { rankApi, type ScoreRankItem, type ScoreRankRequest } from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  scoreFrom: number;
  scoreTo: number;
  rankLabel: string;
  fromYear: number;
  toYear?: number;
  active: boolean;
}

/**
 * Danh muc "Thang diem xep loai rui ro" (sheet "QL thang diem", tcode ztc_rank). Khi them 1 ky moi
 * (nam bat dau lon hon) cho cung 1 xep loai, backend tu dong dong ky cu dang mo (nam ket thuc =
 * nam bat dau moi - 1) theo dung mo ta trong tai lieu goc - khong can xu ly gi them tren FE.
 */
export function RankTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<ScoreRankItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<ScoreRankItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<ScoreRankItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ScoreRankItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await rankApi.list());
    } catch {
      message.error(t("riskScoring.messages.loadError"));
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
    form.setFieldsValue({ active: true });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      scoreFrom: target.scoreFrom,
      scoreTo: target.scoreTo,
      rankLabel: target.rankLabel,
      fromYear: target.fromYear,
      toYear: target.toYear,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      scoreFrom: target.scoreFrom,
      scoreTo: target.scoreTo,
      rankLabel: target.rankLabel,
      fromYear: target.fromYear,
      toYear: target.toYear,
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
      const request: ScoreRankRequest = {
        scoreFrom: values.scoreFrom,
        scoreTo: values.scoreTo,
        rankLabel: values.rankLabel,
        fromYear: values.fromYear,
        toYear: values.toYear ?? null,
        active: values.active,
      };
      if (editing) {
        await rankApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await rankApi.create(request);
        message.success(t("riskScoring.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoring.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await rankApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<ScoreRankItem>["columns"] = [
    { title: t("riskScoring.columns.rankLabel"), width: 130, ...getSearchColumnProps("rankLabel", searchLabels) },
    { title: t("riskScoring.columns.scoreFrom"), dataIndex: "scoreFrom", width: 120 },
    { title: t("riskScoring.columns.scoreTo"), dataIndex: "scoreTo", width: 120 },
    { title: t("riskScoring.columns.fromYear"), dataIndex: "fromYear", width: 100 },
    { title: t("riskScoring.columns.toYear"), dataIndex: "toYear", width: 100 },
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
      <CrudTable<ScoreRankItem>
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onCopy={canCreate ? openCopy : undefined}
        copyDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => rankApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => rankApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await rankApi.importExcel(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("riskScoring.form.editTitle") : t("riskScoring.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="rankLabel" label={t("riskScoring.columns.rankLabel")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="scoreFrom" label={t("riskScoring.columns.scoreFrom")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="scoreTo" label={t("riskScoring.columns.scoreTo")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="fromYear" label={t("riskScoring.columns.fromYear")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="toYear" label={t("riskScoring.columns.toYear")}>
            <InputNumber style={{ width: "100%" }} placeholder="9999" />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
