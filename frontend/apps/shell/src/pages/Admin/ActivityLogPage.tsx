import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Result, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useSearchColumn, useSelectFilterColumn, useServerTable } from "@govia/ui-kit";
import {
  exportActivityLogs,
  listActivityLogs,
  type ActivityLogItem,
  type AuditActionType,
} from "../../api/activityLog";
import { useAuth } from "../../auth/AuthContext";

const ACTIONS: AuditActionType[] = ["CREATE", "UPDATE", "DELETE", "LOGIN", "LOGOUT", "EXPORT", "APPROVE", "REJECT"];

const ACTION_COLORS: Record<AuditActionType, string> = {
  CREATE: "green",
  UPDATE: "blue",
  DELETE: "red",
  LOGIN: "cyan",
  LOGOUT: "default",
  EXPORT: "purple",
  APPROVE: "geekblue",
  REJECT: "volcano",
};

const SORT_FIELD_MAP: Record<string, string> = {
  performedAt: "createdAt",
  performedBy: "createdBy",
};

/** Man hinh "Nhat ky thao tac" (Admin) - CHI DOC bang audit_log dung chung toan platform, ghi nhan
 * CREATE/UPDATE/DELETE tren moi man hinh (xem AuditLogService o backend). Chi SUPER_ADMIN xem duoc,
 * giong quy uoc cua Vai tro/Tai khoan - khong dung permission code rieng. */
export function ActivityLogPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { user } = useAuth();
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;
  const { getSearchColumnProps } = useSearchColumn<ActivityLogItem>();
  const { getSelectFilterColumnProps } = useSelectFilterColumn<ActivityLogItem>();
  const { query, filters, handleChange, pagination } = useServerTable<ActivityLogItem>(SORT_FIELD_MAP);
  const filterLabels = { confirmText: t("common.confirm"), resetText: t("common.reset") };
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [logs, setLogs] = useState<ActivityLogItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listActivityLogs({
        ...query,
        dateFrom: dateRange?.[0]?.format("YYYY-MM-DD"),
        dateTo: dateRange?.[1]?.format("YYYY-MM-DD"),
      });
      setLogs(result.content);
      setTotal(result.totalElements);
    } catch {
      message.error(t("activityLog.messages.loadError"));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(query), dateRange, message, t]);

  useEffect(() => {
    if (isSuperAdmin) load();
  }, [isSuperAdmin, load]);

  const exportFilterParams = {
    entityName: filters.entityName,
    action: filters.action as AuditActionType | undefined,
    performedBy: filters.performedBy,
    keyword: filters.keyword,
    dateFrom: dateRange?.[0]?.format("YYYY-MM-DD"),
    dateTo: dateRange?.[1]?.format("YYYY-MM-DD"),
  };

  const columns: TableProps<ActivityLogItem>["columns"] = [
    {
      title: t("activityLog.columns.performedAt"),
      dataIndex: "performedAt",
      width: 170,
      sorter: true,
      defaultSortOrder: "descend",
      render: (v: string) => dayjs(v).format("DD.MM.YYYY HH:mm:ss"),
    },
    {
      title: t("activityLog.columns.performedBy"),
      dataIndex: "performedBy",
      width: 150,
      sorter: true,
      ...getSearchColumnProps("performedBy", filters.performedBy, searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("activityLog.columns.action"),
      dataIndex: "action",
      width: 130,
      ...getSelectFilterColumnProps(
        "action",
        ACTIONS.map((a) => ({ value: a, text: t(`activityLog.action.${a}`) })),
        filters.action,
        filterLabels,
      ),
      render: (v: AuditActionType) => <Tag color={ACTION_COLORS[v]}>{t(`activityLog.action.${v}`)}</Tag>,
    },
    {
      title: t("activityLog.columns.entityName"),
      dataIndex: "entityName",
      width: 180,
      ...getSearchColumnProps("entityName", filters.entityName, searchLabels),
    },
    {
      title: t("activityLog.columns.detail"),
      dataIndex: "detail",
      ...getSearchColumnProps("keyword", filters.keyword, searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("activityLog.columns.entityId"),
      dataIndex: "entityId",
      width: 280,
      render: (v: string | null) => v ?? "-",
    },
  ];

  if (!isSuperAdmin) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("activityLog.title")}</Typography.Title>
      <div style={{ marginBottom: 12 }}>
        <DatePicker.RangePicker
          value={dateRange}
          onChange={(range) => setDateRange(range && range[0] && range[1] ? [range[0], range[1]] : null)}
          format="DD.MM.YYYY"
          placeholder={[t("activityLog.dateFrom"), t("activityLog.dateTo")]}
        />
      </div>
      <CrudTable<ActivityLogItem>
        tableId="admin.activityLog"
        columns={columns}
        dataSource={logs}
        rowKey="id"
        loading={loading}
        onExportExcel={() => exportActivityLogs("excel", exportFilterParams)}
        onExportWord={() => exportActivityLogs("word", exportFilterParams)}
        onChange={handleChange}
        pagination={pagination(total)}
      />
    </div>
  );
}
