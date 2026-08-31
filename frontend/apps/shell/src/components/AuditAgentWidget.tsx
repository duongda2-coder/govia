import { useState } from "react";
import { Drawer, FloatButton } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { AuditAgentChat } from "../pages/Audit/Agent/AuditAgentChat";

/** Nut noi "Audit AI Assistant" - hien tren MOI man hinh cua toan he thong (mount 1 lan trong
 * AppLayout), khong rieng module Cham diem rui ro, de cac module sau nay dung chung duoc luon. Chi
 * hien khi co quyen AUDIT.AGENT.VIEW - dung cach kiem tra quyen giong moi man hinh khac. */
export function AuditAgentWidget() {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const [open, setOpen] = useState(false);

  if (!hasPermission("AUDIT.AGENT.VIEW")) {
    return null;
  }

  return (
    <>
      <FloatButton
        icon={<RobotOutlined />}
        type="primary"
        tooltip={t("menu.riskScoringExecAgent")}
        onClick={() => setOpen(true)}
      />
      <Drawer
        title={t("menu.riskScoringExecAgent")}
        open={open}
        onClose={() => setOpen(false)}
        width={480}
        destroyOnClose={false}
        styles={{ body: { display: "flex", flexDirection: "column", padding: 16 } }}
      >
        <AuditAgentChat />
      </Drawer>
    </>
  );
}
