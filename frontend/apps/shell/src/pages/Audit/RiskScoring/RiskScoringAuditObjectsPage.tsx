import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { AuditObjectUnitTable } from "./AuditObjectUnitTable";
import { AuditObjectSubsidiaryTable } from "./AuditObjectSubsidiaryTable";
import { AuditObjectProjectTable } from "./AuditObjectProjectTable";
import { AuditObjectProcessTable } from "./AuditObjectProcessTable";

/** "Doi tuong kiem toan" - gom 4 danh muc ZTC_DTKT1-4 (Don vi, Cong ty con, Du an/DVTN, Quy trinh). */
export function RiskScoringAuditObjectsPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringAuditObjects")}</Typography.Title>
      <Tabs
        items={[
          { key: "unit", label: t("riskScoring.tabs.auditObjectUnit"), children: <AuditObjectUnitTable /> },
          { key: "subsidiary", label: t("riskScoring.tabs.auditObjectSubsidiary"), children: <AuditObjectSubsidiaryTable /> },
          { key: "project", label: t("riskScoring.tabs.auditObjectProject"), children: <AuditObjectProjectTable /> },
          { key: "process", label: t("riskScoring.tabs.auditObjectProcess"), children: <AuditObjectProcessTable /> },
        ]}
      />
    </div>
  );
}
