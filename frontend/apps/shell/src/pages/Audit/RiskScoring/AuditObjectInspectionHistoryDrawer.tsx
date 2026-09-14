import { useCallback, useEffect, useState } from "react";
import { App, Drawer, Form, Input, InputNumber, Modal, Select } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  createAuditObjectInspectionHistory,
  deleteAuditObjectInspectionHistory,
  listAuditObjectInspectionHistory,
  updateAuditObjectInspectionHistory,
  type AuditInspectionType,
  type AuditObjectInspectionHistoryItem,
} from "../../../api/auditObjectInspectionHistory";
import type { AuditObjectUnitItem } from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  inspectionType: AuditInspectionType;
  year: number;
  note?: string;
}

const INSPECTION_TYPES: AuditInspectionType[] = ["TTCP", "KTNN", "TTGSNH", "KTNB", "GSBKS", "KTGSNB"];

export interface AuditObjectInspectionHistoryDrawerProps {
  open: boolean;
  unit: AuditObjectUnitItem | null;
  onClose: () => void;
}

/** "Lich su KT" cua 1 Doi tuong kiem toan (sheet ZTC_DTKT1, nut "Lich su KT") - Drawer mo tu man
 * hinh Doi tuong kiem toan (AuditObjectUnitTable), dung chung quyen AUDIT.RISK_SCORING.* voi man
 * hinh cha. Module Ke hoach kiem toan (KHKT) dung du lieu nay de tinh cac cot "T-5..T-1". */
export function AuditObjectInspectionHistoryDrawer({ open, unit, onClose }: AuditObjectInspectionHistoryDrawerProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");

  const [items, setItems] = useState<AuditObjectInspectionHistoryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditObjectInspectionHistoryItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditObjectInspectionHistoryItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    if (!unit) return;
    setLoading(true);
    try {
      setItems(await listAuditObjectInspectionHistory(unit.id));
    } catch {
      message.error(t("riskScoring.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [unit, message, t]);

  useEffect(() => {
    if (open && unit) load();
    if (!open) {
      setItems([]);
      setSelected([]);
    }
  }, [open, unit, load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({ inspectionType: target.inspectionType, year: target.year, note: target.note ?? undefined });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!unit) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request = { auditObjectUnitId: unit.id, inspectionType: values.inspectionType, year: values.year, note: values.note ?? null };
      if (editing) {
        await updateAuditObjectInspectionHistory(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await createAuditObjectInspectionHistory(request);
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
    if (selected.length === 0) return;
    modal.confirm({
      title: t("riskScoring.inspectionHistory.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditObjectInspectionHistory(item.id)));
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditObjectInspectionHistoryItem>["columns"] = [
    {
      title: t("riskScoring.inspectionHistory.inspectionType"),
      dataIndex: "inspectionType",
      width: 140,
      render: (v: AuditInspectionType) => t(`riskScoring.inspectionHistory.types.${v}`),
    },
    { title: t("riskScoring.inspectionHistory.year"), dataIndex: "year", width: 100 },
    { title: t("riskScoring.inspectionHistory.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  return (
    <Drawer
      title={unit ? t("riskScoring.inspectionHistory.title", { name: unit.name }) : ""}
      open={open}
      onClose={onClose}
      width={700}
      destroyOnClose
    >
      <CrudTable<AuditObjectInspectionHistoryItem>
        tableId="riskScoring.auditObjectInspectionHistory"
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
      />

      <Modal
        title={editing ? t("riskScoring.inspectionHistory.editTitle") : t("riskScoring.inspectionHistory.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={480}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="inspectionType" label={t("riskScoring.inspectionHistory.inspectionType")} rules={[{ required: true }]}>
            <Select options={INSPECTION_TYPES.map((v) => ({ value: v, label: t(`riskScoring.inspectionHistory.types.${v}`) }))} />
          </Form.Item>
          <Form.Item name="year" label={t("riskScoring.inspectionHistory.year")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
          </Form.Item>
          <Form.Item name="note" label={t("riskScoring.inspectionHistory.note")}>
            <Input.TextArea rows={2} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </Drawer>
  );
}
