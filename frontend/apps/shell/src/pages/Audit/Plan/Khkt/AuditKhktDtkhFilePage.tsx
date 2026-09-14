import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Select, Space, Typography, Upload } from "antd";
import type { TableProps, UploadFile } from "antd";
import { UploadOutlined, DownloadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import { CrudTable } from "@govia/ui-kit";
import {
  createAuditKhktDtkhFile,
  deleteAuditKhktDtkhFile,
  downloadAuditKhktDtkhFile,
  listAuditKhktDtkhFiles,
  updateAuditKhktDtkhFile,
  type AuditKhktDtkhFileItem,
} from "../../../../api/auditKhktDtkhFile";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface CreateFormValues {
  departmentId: string;
  versionId?: string;
  note?: string;
}

interface EditFormValues {
  departmentId: string;
  versionId?: string;
  note?: string;
}

/** "Báo cáo tham khảo từ các bộ phận cho PKH lập dự thảo" (sheet ZTC_DTKH_FILE) - moi dong la 1
 * file dinh kem (xem AuditKhktDtkhFileService o BE). Khac cac man hinh KHKT khac: cho phep
 * them/sua/xoa day du nen dung CrudTable/StandardToolbar chuan thay vi luong xac nhan rieng. */
export function AuditKhktDtkhFilePage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHKT_DTKH_FILE.VIEW");
  const canCreate = hasPermission("AUDIT.KHKT_DTKH_FILE.CREATE");
  const canEdit = hasPermission("AUDIT.KHKT_DTKH_FILE.EDIT");
  const canDelete = hasPermission("AUDIT.KHKT_DTKH_FILE.DELETE");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [departments, setDepartments] = useState<MasterDataItem[]>([]);
  const [versions, setVersions] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<AuditKhktDtkhFileItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditKhktDtkhFileItem[]>([]);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditKhktDtkhFileItem | null>(null);
  const [uploadFile, setUploadFile] = useState<UploadFile | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [createForm] = Form.useForm<CreateFormValues>();
  const [editForm] = Form.useForm<EditFormValues>();

  useEffect(() => {
    Promise.all([listMasterDataItems("YEAR"), listMasterDataItems("DEPARTMENT"), listMasterDataItems("VERSION")])
      .then(([yearList, departmentList, versionList]) => {
        setYears(yearList);
        setDepartments(departmentList);
        setVersions(versionList);
      })
      .catch(() => message.error(t("auditKhktDtkhFile.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      setItems(await listAuditKhktDtkhFiles(year));
    } catch {
      message.error(t("auditKhktDtkhFile.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelected([]);
  }, [year]);

  const openCreate = () => {
    createForm.resetFields();
    setUploadFile(null);
    setCreateModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    const department = departments.find((d) => d.code === target.departmentCode);
    const version = versions.find((v) => v.code === target.versionCode);
    editForm.setFieldsValue({
      departmentId: department?.id,
      versionId: version?.id,
      note: target.note ?? undefined,
    });
    setEditModalOpen(true);
  };

  const handleSubmitCreate = async () => {
    if (!year) return;
    let values: CreateFormValues;
    try {
      values = await createForm.validateFields();
    } catch {
      return;
    }
    const file = uploadFile?.originFileObj;
    if (!file) {
      message.error(t("auditKhktDtkhFile.messages.fileRequired"));
      return;
    }
    setSubmitting(true);
    try {
      await createAuditKhktDtkhFile(year, values.departmentId, values.versionId ?? null, values.note ?? null, file);
      message.success(t("auditKhktDtkhFile.messages.uploadSuccess"));
      setCreateModalOpen(false);
      await load();
    } catch {
      message.error(t("auditKhktDtkhFile.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitEdit = async () => {
    if (!editing) return;
    let values: EditFormValues;
    try {
      values = await editForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateAuditKhktDtkhFile(editing.id, {
        departmentId: values.departmentId,
        versionId: values.versionId ?? null,
        note: values.note ?? null,
      });
      message.success(t("auditKhktDtkhFile.messages.updateSuccess"));
      setEditModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditKhktDtkhFile.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1
          ? t("common.deleteConfirmTitleCount", { count: selected.length })
          : t("auditKhktDtkhFile.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditKhktDtkhFile(item.id)));
          message.success(t("auditKhktDtkhFile.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditKhktDtkhFile.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditKhktDtkhFileItem>["columns"] = [
    { title: t("auditKhktDtkhFile.columns.department"), dataIndex: "departmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktDtkhFile.columns.uploadedByName"), dataIndex: "uploadedByName", width: 160, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktDtkhFile.columns.fileName"),
      dataIndex: "fileName",
      render: (v: string | null, item) =>
        v && item.attachmentId ? (
          <Button type="link" icon={<DownloadOutlined />} onClick={() => downloadAuditKhktDtkhFile(item.attachmentId!, v)}>
            {v}
          </Button>
        ) : (
          "-"
        ),
    },
    { title: t("auditKhktDtkhFile.columns.version"), dataIndex: "versionCode", width: 110, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktDtkhFile.columns.uploadedAt"),
      dataIndex: "uploadedAt",
      width: 150,
      render: (v: string | null) => (v ? dayjs(v).format("DD.MM.YYYY HH:mm") : "-"),
    },
    { title: t("auditKhktDtkhFile.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhktDtkhFile.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhktDtkhFile.selectYearHint")}</Typography.Text>
      ) : (
        <CrudTable<AuditKhktDtkhFileItem>
          tableId="audit.plan.khktDtkhFile"
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
      )}

      <Modal
        title={t("auditKhktDtkhFile.form.createTitle")}
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={handleSubmitCreate}
        confirmLoading={submitting}
        destroyOnClose
        width={520}
      >
        <Form<CreateFormValues> form={createForm} layout="vertical">
          <Form.Item name="departmentId" label={t("auditKhktDtkhFile.columns.department")} rules={[{ required: true }]}>
            <Select options={departments.map((d) => ({ value: d.id, label: `${d.code} - ${d.name}` }))} />
          </Form.Item>
          <Form.Item name="versionId" label={t("auditKhktDtkhFile.columns.version")}>
            <Select allowClear options={versions.map((v) => ({ value: v.id, label: `${v.code} - ${v.name}` }))} />
          </Form.Item>
          <Form.Item name="note" label={t("auditKhktDtkhFile.columns.note")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
          <Form.Item label={t("auditKhktDtkhFile.columns.fileName")} required>
            <Upload
              beforeUpload={() => false}
              maxCount={1}
              fileList={uploadFile ? [uploadFile] : []}
              onChange={({ fileList }) => setUploadFile(fileList[fileList.length - 1] ?? null)}
            >
              <Button icon={<UploadOutlined />}>{t("attachment.upload")}</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={t("auditKhktDtkhFile.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmitEdit}
        confirmLoading={submitting}
        destroyOnClose
        width={520}
      >
        <Form<EditFormValues> form={editForm} layout="vertical">
          <Form.Item name="departmentId" label={t("auditKhktDtkhFile.columns.department")} rules={[{ required: true }]}>
            <Select options={departments.map((d) => ({ value: d.id, label: `${d.code} - ${d.name}` }))} />
          </Form.Item>
          <Form.Item name="versionId" label={t("auditKhktDtkhFile.columns.version")}>
            <Select allowClear options={versions.map((v) => ({ value: v.id, label: `${v.code} - ${v.name}` }))} />
          </Form.Item>
          <Form.Item name="note" label={t("auditKhktDtkhFile.columns.note")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
