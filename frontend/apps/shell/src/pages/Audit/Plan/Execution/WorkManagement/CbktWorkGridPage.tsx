import { useTranslation } from "react-i18next";
import { WorkManagementGridPage } from "./WorkManagementGridPage";

/** "Màn hình Quản lý công việc CBKT" (sheet "Quản lý công việc" trong Tạo CKT (1).xlsx). */
export function CbktWorkGridPage() {
  const { t } = useTranslation();
  return <WorkManagementGridPage phase="CBKT" tableId="audit.plan.execution.workManagement.cbkt" title={t("auditWorkManagement.cbktTitle")} />;
}
