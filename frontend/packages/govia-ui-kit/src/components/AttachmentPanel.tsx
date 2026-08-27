import { useEffect, useState, useCallback } from "react";
import { App, Upload, Button, List, Popconfirm } from "antd";
import { UploadOutlined, DeleteOutlined, DownloadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import type { AxiosInstance } from "axios";
import type { ApiResponse } from "../api/httpClient";

export interface Attachment {
  id: string;
  fileName: string;
  contentType: string | null;
  sizeBytes: number | null;
  createdAt: string;
  createdBy: string | null;
}

export interface AttachmentPanelProps {
  http: AxiosInstance;
  entityName: string;
  entityId: string;
  /** Bao lai cho man hinh cha so file hien tai moi lan danh sach thay doi (upload/xoa) - man hinh
   * danh sach dung de cap nhat badge so file ngay lap tuc, khong doi dong Drawer/Modal dinh kem. */
  onCountChange?: (count: number) => void;
}

/**
 * So luong file dinh kem theo tung entityId, dung de hien badge "X file" tren cot cua man hinh
 * danh sach (vd DocumentLibraryPage) - 1 request cho ca trang thay vi goi /api/attachments rieng
 * cho tung dong (N+1).
 */
export async function fetchAttachmentCounts(
  http: AxiosInstance,
  entityName: string,
  entityIds: string[],
): Promise<Record<string, number>> {
  if (entityIds.length === 0) return {};
  const res = await http.get<ApiResponse<Record<string, number>>>("/api/attachments/counts", {
    params: { entityName, entityIds: entityIds.join(",") },
  });
  return res.data.data;
}

/**
 * Component dinh kem DUY NHAT dung chung cho moi man hinh cua moi module GOVIA.
 * Chi can truyen entityName ("AUDIT_FINDING", "EMPLOYEE"...) va entityId,
 * goi thang API /api/attachments ben govia-core - khong man hinh nao tu viet upload rieng.
 */
export function AttachmentPanel({ http, entityName, entityId, onCountChange }: AttachmentPanelProps) {
  const [items, setItems] = useState<Attachment[]>([]);
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();
  const { t } = useTranslation();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await http.get<ApiResponse<Attachment[]>>("/api/attachments", {
        params: { entityName, entityId },
      });
      setItems(res.data.data);
      onCountChange?.(res.data.data.length);
    } finally {
      setLoading(false);
    }
  }, [http, entityName, entityId, onCountChange]);

  useEffect(() => {
    load();
  }, [load]);

  const handleUpload = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    try {
      await http.post("/api/attachments", formData, {
        params: { entityName, entityId },
        headers: { "Content-Type": "multipart/form-data" },
      });
      message.success(t("attachment.uploadSuccess", { fileName: file.name }));
      await load();
    } catch {
      message.error(t("attachment.uploadError", { fileName: file.name }));
    }
    return false; // ngan Upload tu dong submit form mac dinh
  };

  const handleDelete = async (id: string) => {
    await http.delete(`/api/attachments/${id}`);
    message.success(t("attachment.deleteSuccess"));
    await load();
  };

  const handleDownload = async (id: string, fileName: string) => {
    // Dung axios (khong phai <a href> thuan) de header Authorization duoc dinh kem tu dong.
    const res = await http.get(`/api/attachments/${id}/download`, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(res.data as Blob);
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(blobUrl);
  };

  return (
    <div>
      <Upload multiple beforeUpload={handleUpload} showUploadList={false}>
        <Button icon={<UploadOutlined />}>{t("attachment.upload")}</Button>
      </Upload>
      <List
        loading={loading}
        style={{ marginTop: 12 }}
        dataSource={items}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button
                key="download"
                type="link"
                icon={<DownloadOutlined />}
                onClick={() => handleDownload(item.id, item.fileName)}
              />,
              <Popconfirm key="delete" title={t("attachment.deleteConfirm")} onConfirm={() => handleDelete(item.id)}>
                <Button type="link" danger icon={<DeleteOutlined />} />
              </Popconfirm>,
            ]}
          >
            {item.fileName}
          </List.Item>
        )}
      />
    </div>
  );
}
