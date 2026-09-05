import { useCallback, useEffect, useState } from "react";
import { App, Button, Drawer, Table, Upload } from "antd";
import type { TableProps } from "antd";
import { DeleteOutlined, DownloadOutlined, UploadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import {
  deleteAuditWorkReportFile,
  downloadAuditWorkReportFile,
  listAuditWorkReportFiles,
  uploadAuditWorkReportFile,
  type AuditWorkReportFile,
} from "../../../../../api/auditWorkManagement";
import { useAuth } from "../../../../../auth/AuthContext";

export interface OtherReportFilesDrawerProps {
  open: boolean;
  engagementId: string | null;
  onClose: () => void;
}

/** "1. File báo cáo khác" (man hinh "Quản lý công việc"): dung chung cho ca CBKT va THKT cua 1
 * cuoc kiem toan. Chi nguoi da upload moi thay nut xoa cho file cua chinh minh (dung dac ta). */
export function OtherReportFilesDrawer({ open, engagementId, onClose }: OtherReportFilesDrawerProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { user } = useAuth();

  const [items, setItems] = useState<AuditWorkReportFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  const load = useCallback(async () => {
    if (!engagementId) return;
    setLoading(true);
    try {
      setItems(await listAuditWorkReportFiles(engagementId));
    } catch {
      message.error(t("auditWorkManagement.reportFiles.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const handleUpload = async (file: File) => {
    if (!engagementId) return false;
    setUploading(true);
    try {
      await uploadAuditWorkReportFile(engagementId, file);
      message.success(t("auditWorkManagement.reportFiles.uploadSuccess"));
      await load();
    } catch {
      message.error(t("auditWorkManagement.reportFiles.uploadError"));
    } finally {
      setUploading(false);
    }
    return false;
  };

  const handleDelete = async (item: AuditWorkReportFile) => {
    if (!engagementId) return;
    try {
      await deleteAuditWorkReportFile(engagementId, item.id);
      message.success(t("auditWorkManagement.reportFiles.deleteSuccess"));
      await load();
    } catch {
      message.error(t("auditWorkManagement.reportFiles.deleteError"));
    }
  };

  const columns: TableProps<AuditWorkReportFile>["columns"] = [
    { title: t("auditWorkManagement.reportFiles.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 110, render: (v) => v ?? "-" },
    {
      title: t("auditWorkManagement.reportFiles.columns.uploadedAt"),
      dataIndex: "uploadedAt",
      width: 140,
      render: (v: string) => dayjs(v).format("DD.MM.YYYY HH:mm"),
    },
    { title: t("auditWorkManagement.reportFiles.columns.uploadedByName"), dataIndex: "uploadedByName", width: 160, render: (v) => v ?? "-" },
    { title: t("auditWorkManagement.reportFiles.columns.reportType"), dataIndex: "reportType", width: 120 },
    { title: t("auditWorkManagement.reportFiles.columns.fileName"), dataIndex: "fileName" },
    {
      title: "",
      key: "actions",
      width: 90,
      render: (_: unknown, item: AuditWorkReportFile) => (
        <>
          <Button type="link" icon={<DownloadOutlined />} onClick={() => downloadAuditWorkReportFile(item.id, item.fileName)} />
          {item.uploadedByUsername === user?.username && (
            <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(item)} />
          )}
        </>
      ),
    },
  ];

  return (
    <Drawer title={t("auditWorkManagement.reportFiles.title")} open={open} onClose={onClose} width={760} destroyOnClose>
      <Upload beforeUpload={handleUpload} showUploadList={false}>
        <Button icon={<UploadOutlined />} loading={uploading}>
          {t("attachment.upload")}
        </Button>
      </Upload>
      <Table<AuditWorkReportFile>
        style={{ marginTop: 16 }}
        rowKey="id"
        loading={loading}
        dataSource={items}
        columns={columns}
        pagination={false}
      />
    </Drawer>
  );
}
