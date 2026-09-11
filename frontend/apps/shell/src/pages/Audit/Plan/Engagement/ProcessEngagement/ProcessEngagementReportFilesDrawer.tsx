import { useCallback, useEffect, useState } from "react";
import { App, Button, Drawer, Table, Upload } from "antd";
import type { TableProps } from "antd";
import { DeleteOutlined, DownloadOutlined, UploadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import {
  deleteProcessEngagementReportFile,
  listProcessEngagementReportFiles,
  uploadProcessEngagementReportFile,
  type AuditProcessEngagementReportFile,
} from "../../../../../api/auditProcessEngagement";
import { downloadAuditWorkReportFile } from "../../../../../api/auditWorkManagement";
import { useAuth } from "../../../../../auth/AuthContext";

export interface ProcessEngagementReportFilesDrawerProps {
  open: boolean;
  processEngagementId: string | null;
  onClose: () => void;
}

/** "2. File báo cáo khác" (man hinh "QL CKT quy trinh") - clone cua OtherReportFilesDrawer, chi
 * doi entityId/endpoint sang cap CKT quy trinh. Download dung chung endpoint attachment. */
export function ProcessEngagementReportFilesDrawer({ open, processEngagementId, onClose }: ProcessEngagementReportFilesDrawerProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { user } = useAuth();

  const [items, setItems] = useState<AuditProcessEngagementReportFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  const load = useCallback(async () => {
    if (!processEngagementId) return;
    setLoading(true);
    try {
      setItems(await listProcessEngagementReportFiles(processEngagementId));
    } catch {
      message.error(t("auditWorkManagement.reportFiles.loadError"));
    } finally {
      setLoading(false);
    }
  }, [processEngagementId, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const handleUpload = async (file: File) => {
    if (!processEngagementId) return false;
    setUploading(true);
    try {
      await uploadProcessEngagementReportFile(processEngagementId, file);
      message.success(t("auditWorkManagement.reportFiles.uploadSuccess"));
      await load();
    } catch {
      message.error(t("auditWorkManagement.reportFiles.uploadError"));
    } finally {
      setUploading(false);
    }
    return false;
  };

  const handleDelete = async (item: AuditProcessEngagementReportFile) => {
    if (!processEngagementId) return;
    try {
      await deleteProcessEngagementReportFile(processEngagementId, item.id);
      message.success(t("auditWorkManagement.reportFiles.deleteSuccess"));
      await load();
    } catch {
      message.error(t("auditWorkManagement.reportFiles.deleteError"));
    }
  };

  const columns: TableProps<AuditProcessEngagementReportFile>["columns"] = [
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
      render: (_: unknown, item: AuditProcessEngagementReportFile) => (
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
    <Drawer title={t("auditProcessEngagement.reportFiles.title")} open={open} onClose={onClose} width={760} destroyOnClose>
      <Upload beforeUpload={handleUpload} showUploadList={false}>
        <Button icon={<UploadOutlined />} loading={uploading}>
          {t("attachment.upload")}
        </Button>
      </Upload>
      <Table<AuditProcessEngagementReportFile>
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
