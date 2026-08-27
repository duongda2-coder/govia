import { Drawer, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { AttachmentPanel } from "@govia/ui-kit";
import type { DocumentLibraryItem } from "../../../api/documentLibrary";
import { httpClient } from "../../../api/client";

export interface DocumentAttachmentDrawerProps {
  open: boolean;
  document: DocumentLibraryItem | null;
  onClose: () => void;
  onCountChange?: (documentId: string, count: number) => void;
}

/**
 * Drawer dinh kem file rieng cho 1 van ban - mo tu nut "Dinh kem" tren toolbar hoac tu badge so
 * luong file tren cot danh sach, khong bat buoc phai mo form Sua truoc (xem DocumentLibraryPage).
 */
export function DocumentAttachmentDrawer({ open, document, onClose, onCountChange }: DocumentAttachmentDrawerProps) {
  const { t } = useTranslation();

  return (
    <Drawer
      title={t("common.attachment")}
      open={open}
      onClose={onClose}
      width={480}
      destroyOnClose
    >
      {document && (
        <>
          <Typography.Text type="secondary">{document.documentNumber}</Typography.Text>
          <Typography.Title level={5} style={{ marginTop: 4 }}>
            {document.documentName}
          </Typography.Title>
          <div style={{ marginTop: 16 }}>
            <AttachmentPanel
              http={httpClient}
              entityName="AUDIT_DOCUMENT_LIBRARY"
              entityId={document.id}
              onCountChange={(count) => onCountChange?.(document.id, count)}
            />
          </div>
        </>
      )}
    </Drawer>
  );
}
