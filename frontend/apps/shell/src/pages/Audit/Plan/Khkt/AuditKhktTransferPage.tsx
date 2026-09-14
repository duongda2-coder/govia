import { useCallback, useEffect, useState } from "react";
import { App, Button, List, Modal, Result, Select, Space, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { SendOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import {
  listAuditKhktTransferCandidates,
  transferAuditKhktObjects,
  type AuditKhnsTransferCandidateItem,
  type AuditKhnsTransferResultItem,
} from "../../../../api/auditKhktTransfer";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

/** "Chuyển thông tin KHTH" (sheet ChuyenthongtinKHTH) - day du lieu KHNS_NAM (nhan su + doi tuong +
 * thang + so/ngay quyet dinh) sang Cuoc kiem toan (ztc_job). Chon tung doi tuong kiem toan roi bam
 * "Thực hiện KHKT" (theo yeu cau NSD, khong chuyen ca nam 1 lan) - xem AuditKhktTransferService. */
export function AuditKhktTransferPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHTH_TRANSFER.VIEW");
  const canExecute = hasPermission("AUDIT.KHTH_TRANSFER.EXECUTE");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<AuditKhnsTransferCandidateItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [transferring, setTransferring] = useState(false);
  const [resultItems, setResultItems] = useState<AuditKhnsTransferResultItem[] | null>(null);

  useEffect(() => {
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => message.error(t("auditKhktTransfer.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      setRows(await listAuditKhktTransferCandidates(year));
    } catch {
      message.error(t("auditKhktTransfer.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedCodes([]);
  }, [year]);

  const handleTransfer = async () => {
    if (!year || selectedCodes.length === 0) return;
    setTransferring(true);
    try {
      const results = await transferAuditKhktObjects(year, selectedCodes);
      setResultItems(results);
      const failureCount = results.filter((r) => !r.success).length;
      if (failureCount === 0) {
        message.success(t("auditKhktTransfer.messages.transferSuccess", { count: results.length }));
      } else {
        message.warning(t("auditKhktTransfer.messages.transferPartial", { success: results.length - failureCount, failure: failureCount }));
      }
      setSelectedCodes([]);
      await load();
    } catch {
      message.error(t("auditKhktTransfer.messages.transferError"));
    } finally {
      setTransferring(false);
    }
  };

  const columns: TableProps<AuditKhnsTransferCandidateItem>["columns"] = [
    { title: t("auditKhktTransfer.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 100, fixed: "left" },
    { title: t("auditKhktTransfer.columns.auditObjectName"), dataIndex: "auditObjectName", width: 180, fixed: "left" },
    {
      title: t("auditKhktTransfer.columns.teamLead"),
      dataIndex: "teamLeadEmployeeName",
      width: 160,
      render: (v: string | null, row) => (v ? `${row.teamLeadEmployeeCode} - ${v}` : "-"),
    },
    { title: t("auditKhktTransfer.columns.memberCount"), dataIndex: "memberCount", width: 90, align: "center" },
    {
      title: t("auditKhktTransfer.columns.expectedMonth"),
      dataIndex: "expectedMonth",
      width: 110,
      render: (v: number | null) => (v ? t("auditKhktThang.columns.month", { month: v }) : "-"),
    },
    { title: t("auditKhktTransfer.columns.decisionNumber"), dataIndex: "decisionNumber", width: 120, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktTransfer.columns.decisionDate"),
      dataIndex: "decisionDate",
      width: 120,
      render: (v: string | null) => (v ? dayjs(v).format("DD.MM.YYYY") : "-"),
    },
    { title: t("auditKhktTransfer.columns.expectedBatch"), dataIndex: "expectedBatch", width: 130, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktTransfer.columns.existingEngagementCode"),
      dataIndex: "existingEngagementCode",
      width: 150,
      render: (v: string | null) => (v ? <Tag color="blue">{v}</Tag> : "-"),
    },
    {
      title: t("auditKhktTransfer.columns.status"),
      dataIndex: "transferable",
      width: 220,
      render: (v: boolean, row) => (v ? <Tag color="green">{t("auditKhktTransfer.ready")}</Tag> : <Tag color="orange">{row.blockReason}</Tag>),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhktTransfer.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
        {canExecute && (
          <Button
            type="primary"
            icon={<SendOutlined />}
            disabled={selectedCodes.length === 0}
            loading={transferring}
            onClick={handleTransfer}
          >
            {t("auditKhktTransfer.transferButton", { count: selectedCodes.length })}
          </Button>
        )}
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhktTransfer.selectYearHint")}</Typography.Text>
      ) : (
        <Table<AuditKhnsTransferCandidateItem>
          size="small"
          columns={columns}
          dataSource={rows}
          rowKey="auditObjectCode"
          loading={loading}
          pagination={{ pageSize: 50 }}
          scroll={{ x: "max-content" }}
          rowSelection={{
            selectedRowKeys: selectedCodes,
            onChange: (keys) => setSelectedCodes(keys as string[]),
            getCheckboxProps: (row) => ({ disabled: !row.transferable }),
          }}
        />
      )}

      <Modal
        title={t("auditKhktTransfer.resultTitle")}
        open={!!resultItems}
        onCancel={() => setResultItems(null)}
        onOk={() => setResultItems(null)}
        footer={null}
        width={560}
      >
        {resultItems && (
          <List
            size="small"
            bordered
            dataSource={resultItems}
            style={{ maxHeight: 400, overflowY: "auto" }}
            renderItem={(item) => (
              <List.Item>
                <Space>
                  <Tag color={item.success ? "green" : "red"}>{item.auditObjectCode}</Tag>
                  {item.success ? t("auditKhktTransfer.resultSuccess", { code: item.engagementCode }) : item.message}
                </Space>
              </List.Item>
            )}
          />
        )}
      </Modal>
    </div>
  );
}
