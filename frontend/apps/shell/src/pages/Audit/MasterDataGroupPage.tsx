import { useEffect, useState } from "react";
import { App, Result, Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { listMasterDataCategories, type MasterDataCategoryInfo } from "../../api/auditMasterData";
import { useAuth } from "../../auth/AuthContext";
import { MasterDataCatalogTable } from "./MasterDataCatalogTable";

/**
 * 1 man hinh cho MOI nhom danh muc (Kiem toan/Rui ro/Kiem soat/...), moi tab ben trong la 1 loai
 * danh muc rieng - danh sach cac loai danh muc lay tu backend (GET /categories), KHONG hardcode o
 * FE, nen them/bot 1 danh muc o enum backend se tu dong hien/an tab tuong ung, khong can sua FE.
 */
export function MasterDataGroupPage({ group, title }: { group: string; title: string }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.MASTER_DATA.VIEW");

  const [categories, setCategories] = useState<MasterDataCategoryInfo[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!canView) return;
    setLoading(true);
    listMasterDataCategories()
      .then((all) => setCategories(all.filter((c) => c.group === group)))
      .catch(() => message.error(t("auditMasterData.messages.loadError")))
      .finally(() => setLoading(false));
  }, [canView, group, message, t]);

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{title}</Typography.Title>
      {!loading && (
        <Tabs
          items={categories.map((c) => ({
            key: c.code,
            label: c.label,
            children: <MasterDataCatalogTable category={c.code} label={c.label} />,
          }))}
        />
      )}
    </div>
  );
}
