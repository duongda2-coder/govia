import { useState } from "react";
import { App, Button, List, Modal, Space, Typography, Upload } from "antd";
import { isAxiosError } from "axios";
import {
  PlusOutlined,
  EditOutlined,
  CopyOutlined,
  DeleteOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  PaperClipOutlined,
  UploadOutlined,
  FolderOpenOutlined,
  CheckCircleOutlined,
  DownloadOutlined,
} from "@ant-design/icons";
import { useTranslation } from "react-i18next";

export interface ImportResult {
  successCount: number;
  failureCount: number;
  errors: { row: number; message: string }[];
  notices?: string[];
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
  /** "File báo cáo khác" - vd man hinh Quan ly cong viec (CBKT/THKT): mo drawer file dinh kem
   * dung chung cua ca cuoc kiem toan (khac voi onAttachment, thuong gan voi 1 dong dang chon). */
  onOtherReports?: () => void;
  /** "Phê duyệt" hang loat - vd truong doan chon nhieu dong da hoan thanh roi bam duyet. */
  onApprove?: () => void;
  /** "Tải mẫu" - xuat file Excel rong (chi co header) de nguoi dung dien tay roi Import lai - vd
   * "Download Template TTSS". Khac onExportExcel (xuat DU LIEU hien co) o cho day la mau RONG. */
  onDownloadTemplate?: () => void;
  addDisabled?: boolean;
  editDisabled?: boolean;
  copyDisabled?: boolean;
  deleteDisabled?: boolean;
  attachmentDisabled?: boolean;
  approveDisabled?: boolean;
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
    onOtherReports,
    onApprove,
    onDownloadTemplate,
    addDisabled,
    editDisabled,
    copyDisabled,
    deleteDisabled,
    attachmentDisabled,
    approveDisabled,
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
      .catch((err) => {
        const backendMessage = isAxiosError<{ message?: string }>(err) ? err.response?.data?.message : undefined;
        message.error(backendMessage ? `${t("common.importError")}: ${backendMessage}` : t("common.importError"));
      })
      .finally(() => setImporting(false));
    return false; // ngan Upload tu dong submit file theo co che mac dinh
  };

  return (
    <>
      <Space wrap>
        {onDownloadTemplate && (
          <Button icon={<DownloadOutlined />} onClick={onDownloadTemplate}>
            {t("common.downloadTemplate")}
          </Button>
        )}
        {onAdd && (
          <Button type="primary" icon={<PlusOutlined />} onClick={onAdd} loading={loading} disabled={addDisabled}>
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
          <Button icon={<PaperClipOutlined />} onClick={onAttachment} disabled={attachmentDisabled}>
            {t("common.attachment")}
          </Button>
        )}
        {onOtherReports && (
          <Button icon={<FolderOpenOutlined />} onClick={onOtherReports}>
            {t("common.otherReports")}
          </Button>
        )}
        {onApprove && (
          <Button icon={<CheckCircleOutlined />} onClick={onApprove} disabled={approveDisabled}>
            {t("common.approve")}
          </Button>
        )}
      </Space>

      <Modal title={t("common.importResultTitle")} open={!!result} onCancel={() => setResult(null)} onOk={() => setResult(null)} footer={null}>
        {result && (
          <>
            <Typography.Paragraph>
              {t("common.importSummary", { success: result.successCount, failure: result.failureCount })}
            </Typography.Paragraph>
            {!!result.notices?.length && (
              <List
                size="small"
                bordered
                dataSource={result.notices}
                style={{ maxHeight: 300, overflowY: "auto", marginBottom: result.errors.length > 0 ? 16 : 0 }}
                renderItem={(notice) => <List.Item>{notice}</List.Item>}
              />
            )}
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
