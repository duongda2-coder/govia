import { useState } from "react";
import { App, Button, List, Modal, Space, Typography, Upload } from "antd";
import {
  PlusOutlined,
  EditOutlined,
  CopyOutlined,
  DeleteOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  PaperClipOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useTranslation } from "react-i18next";

export interface ImportResult {
  successCount: number;
  failureCount: number;
  errors: { row: number; message: string }[];
}

/**
 * Toolbar hanh dong CHUAN dung chung cho TAT CA man hinh cua moi module GOVIA.
 * Man hinh nao can nut Them/Sua/Copy/Xoa/Xuat Excel/Xuat Word/Attachment/Import thi chi
 * can truyen handler tuong ung - khong tu ve lai nut, dam bao UI dong nhat toan platform.
 * Import bat buoc di kem 1 nut Export lam "mau" - du lieu import phai dung dinh dang da xuat ra.
 * Copy = mo lai form Them moi voi du lieu cua dong dang chon dien san (tru cac truong dinh danh
 * duy nhat nhu "ma") - ho tro nguoi dung tao nhanh 1 dong moi gan giong 1 dong da co san.
 */
export interface StandardToolbarProps {
  onAdd?: () => void;
  onEdit?: () => void;
  onCopy?: () => void;
  onDelete?: () => void;
  onExportExcel?: () => void;
  onExportWord?: () => void;
  onAttachment?: () => void;
  onImport?: (file: File) => Promise<ImportResult>;
  editDisabled?: boolean;
  copyDisabled?: boolean;
  deleteDisabled?: boolean;
  loading?: boolean;
}

export function StandardToolbar(props: StandardToolbarProps) {
  const {
    onAdd,
    onEdit,
    onCopy,
    onDelete,
    onExportExcel,
    onExportWord,
    onAttachment,
    onImport,
    editDisabled,
    copyDisabled,
    deleteDisabled,
    loading,
  } = props;
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [importing, setImporting] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);

  const handleImport = (file: File) => {
    if (!onImport) return false;
    setImporting(true);
    onImport(file)
      .then((res) => {
        setResult(res);
        if (res.failureCount === 0) {
          message.success(t("common.importSuccess", { count: res.successCount }));
        }
      })
      .catch(() => message.error(t("common.importError")))
      .finally(() => setImporting(false));
    return false; // ngan Upload tu dong submit file theo co che mac dinh
  };

  return (
    <>
      <Space wrap>
        {onAdd && (
          <Button type="primary" icon={<PlusOutlined />} onClick={onAdd} loading={loading}>
            {t("common.add")}
          </Button>
        )}
        {onEdit && (
          <Button icon={<EditOutlined />} onClick={onEdit} disabled={editDisabled}>
            {t("common.edit")}
          </Button>
        )}
        {onCopy && (
          <Button icon={<CopyOutlined />} onClick={onCopy} disabled={copyDisabled}>
            {t("common.copy")}
          </Button>
        )}
        {onDelete && (
          <Button danger icon={<DeleteOutlined />} onClick={onDelete} disabled={deleteDisabled}>
            {t("common.delete")}
          </Button>
        )}
        {onImport && (
          <Upload accept=".xlsx" showUploadList={false} beforeUpload={handleImport}>
            <Button icon={<UploadOutlined />} loading={importing}>
              {t("common.import")}
            </Button>
          </Upload>
        )}
        {onExportExcel && (
          <Button icon={<FileExcelOutlined />} onClick={onExportExcel}>
            {t("common.exportExcel")}
          </Button>
        )}
        {onExportWord && (
          <Button icon={<FileWordOutlined />} onClick={onExportWord}>
            {t("common.exportWord")}
          </Button>
        )}
        {onAttachment && (
          <Button icon={<PaperClipOutlined />} onClick={onAttachment}>
            {t("common.attachment")}
          </Button>
        )}
      </Space>

      <Modal title={t("common.importResultTitle")} open={!!result} onCancel={() => setResult(null)} onOk={() => setResult(null)} footer={null}>
        {result && (
          <>
            <Typography.Paragraph>
              {t("common.importSummary", { success: result.successCount, failure: result.failureCount })}
            </Typography.Paragraph>
            {result.errors.length > 0 && (
              <List
                size="small"
                bordered
                dataSource={result.errors}
                style={{ maxHeight: 300, overflowY: "auto" }}
                renderItem={(err) => (
                  <List.Item>
                    {t("common.importRow", { row: err.row })}: {err.message}
                  </List.Item>
                )}
              />
            )}
          </>
        )}
      </Modal>
    </>
  );
}
