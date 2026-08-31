import { Drawer, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { AttachmentPanel } from "@govia/ui-kit";
import type { AuditFindingItem } from "../../../api/auditFinding";
import { httpClient } from "../../../api/client";

export interface AuditFindingAttachmentDrawerProps {
  open: boolean;
  finding: AuditFindingItem | null;
  onClose: () => void;
  onCountChange?: (findingId: string, count: number) => void;
}

/** Drawer dinh kem evidence cho 1 Audit Finding - nguon du lieu that cho tool "get_evidence" (xem
 * docs/audit-tools-contract.md). Cung pattern voi DocumentAttachmentDrawer. */
export function AuditFindingAttachmentDrawer({ open, finding, onClose, onCountChange }: AuditFindingAttachmentDrawerProps) {
  const { t } = useTranslation();

  return (
    <Drawer title={t("common.attachment")} open={open} onClose={onClose} width={480} destroyOnClose>
      {finding && (
        <>
          <Typography.Text type="secondary">{finding.branchName ?? finding.branchCode}</Typography.Text>
          <Typography.Title level={5} style={{ marginTop: 4 }}>
            {finding.title}
          </Typography.Title>
          <div style={{ marginTop: 16 }}>
            <AttachmentPanel
              http={httpClient}
              entityName="AUDIT_FINDING"
              entityId={finding.id}
              onCountChange={(count) => onCountChange?.(finding.id, count)}
            />
          </div>
        </>
      )}
    </Drawer>
  );
}
