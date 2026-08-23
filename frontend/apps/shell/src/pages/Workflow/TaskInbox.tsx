import { useCallback, useEffect, useState } from "react";
import { App, Button, Modal, Result, Select, Space, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import type { TaskSummary } from "../../api/workflow";
import { claimTask, completeTask, delegateTask, listMyTasks, reassignTask, resolveTask } from "../../api/workflow";
import { listAccounts, type AccountSummary } from "../../api/accounts";
import { useAuth } from "../../auth/AuthContext";

type TransferMode = "reassign" | "delegate";

export function TaskInbox() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("WORKFLOW.TASK.VIEW");
  const canComplete = hasPermission("WORKFLOW.TASK.COMPLETE");

  const [tasks, setTasks] = useState<TaskSummary[]>([]);
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [actingId, setActingId] = useState<string | null>(null);
  const [transferTask, setTransferTask] = useState<TaskSummary | null>(null);
  const [transferMode, setTransferMode] = useState<TransferMode>("reassign");
  const [transferTarget, setTransferTarget] = useState<string | null>(null);
  const [transferSubmitting, setTransferSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setTasks(await listMyTasks());
    } catch {
      message.error(t("workflow.task.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openTransfer = async (task: TaskSummary, mode: TransferMode) => {
    setTransferTask(task);
    setTransferMode(mode);
    setTransferTarget(null);
    if (accounts.length === 0) {
      try {
        setAccounts(await listAccounts());
      } catch {
        message.error(t("workflow.task.messages.loadError"));
      }
    }
  };

  const handleTransferSubmit = async () => {
    if (!transferTask || !transferTarget) return;
    setTransferSubmitting(true);
    try {
      if (transferMode === "reassign") {
        await reassignTask(transferTask.id, transferTarget);
        message.success(t("workflow.task.messages.reassignSuccess"));
      } else {
        await delegateTask(transferTask.id, transferTarget);
        message.success(t("workflow.task.messages.delegateSuccess"));
      }
      setTransferTask(null);
      await load();
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setTransferSubmitting(false);
    }
  };

  const handleClaim = async (task: TaskSummary) => {
    setActingId(task.id);
    try {
      await claimTask(task.id);
      message.success(t("workflow.task.messages.claimSuccess"));
      await load();
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setActingId(null);
    }
  };

  const handleComplete = async (task: TaskSummary, variables?: Record<string, unknown>) => {
    setActingId(task.id);
    try {
      await completeTask(task.id, variables);
      message.success(t("workflow.task.messages.completeSuccess"));
      await load();
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setActingId(null);
    }
  };

  const handleResolve = async (task: TaskSummary) => {
    setActingId(task.id);
    try {
      await resolveTask(task.id);
      message.success(t("workflow.task.messages.resolveSuccess"));
      await load();
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setActingId(null);
    }
  };

  const columns: TableProps<TaskSummary>["columns"] = [
    { title: t("workflow.task.columns.name"), dataIndex: "name" },
    {
      title: t("workflow.task.columns.businessKey"),
      dataIndex: "businessKey",
      width: 160,
      render: (v: string | null) => v || "-",
    },
    {
      title: t("workflow.task.columns.delegation"),
      dataIndex: "delegationState",
      width: 140,
      render: (v: TaskSummary["delegationState"]) =>
        v === "PENDING" ? (
          <Tag color="gold">{t("workflow.task.delegation.pending")}</Tag>
        ) : v === "RESOLVED" ? (
          <Tag color="blue">{t("workflow.task.delegation.resolved")}</Tag>
        ) : null,
    },
    {
      title: t("workflow.task.columns.createTime"),
      dataIndex: "createTime",
      width: 200,
      render: (v: string) => new Date(v).toLocaleString(),
    },
    {
      title: t("workflow.task.columns.actions"),
      key: "actions",
      width: 340,
      render: (_: unknown, record) => {
        if (!canComplete) return null;
        const busy = actingId === record.id;

        if (!record.assignee) {
          return (
            <Button size="small" loading={busy} onClick={() => handleClaim(record)}>
              {t("workflow.task.actions.claim")}
            </Button>
          );
        }

        const transferButtons = (
          <>
            <Button size="small" onClick={() => openTransfer(record, "reassign")}>
              {t("workflow.task.actions.forward")}
            </Button>
            <Button size="small" onClick={() => openTransfer(record, "delegate")}>
              {t("workflow.task.actions.delegate")}
            </Button>
          </>
        );

        if (record.delegationState === "PENDING") {
          return (
            <Space wrap>
              <Button size="small" type="primary" loading={busy} onClick={() => handleResolve(record)}>
                {t("workflow.task.actions.resolve")}
              </Button>
              {transferButtons}
            </Space>
          );
        }

        if (record.name === "Submit") {
          return (
            <Space wrap>
              <Button size="small" type="primary" loading={busy} onClick={() => handleComplete(record)}>
                {t("workflow.task.actions.complete")}
              </Button>
              {transferButtons}
            </Space>
          );
        }

        return (
          <Space wrap>
            <Button size="small" type="primary" loading={busy} onClick={() => handleComplete(record, { approved: true })}>
              {t("workflow.task.actions.approve")}
            </Button>
            <Button size="small" danger loading={busy} onClick={() => handleComplete(record, { approved: false })}>
              {t("workflow.task.actions.reject")}
            </Button>
            {transferButtons}
          </Space>
        );
      },
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("workflow.task.title")}</Typography.Title>
      <CrudTable<TaskSummary> columns={columns} dataSource={tasks} rowKey="id" loading={loading} />

      <Modal
        title={transferMode === "reassign" ? t("workflow.task.transferModal.forwardTitle") : t("workflow.task.transferModal.delegateTitle")}
        open={!!transferTask}
        onCancel={() => setTransferTask(null)}
        onOk={handleTransferSubmit}
        confirmLoading={transferSubmitting}
        okButtonProps={{ disabled: !transferTarget }}
        destroyOnClose
      >
        <Select
          style={{ width: "100%" }}
          placeholder={t("workflow.task.transferModal.selectPlaceholder")}
          value={transferTarget}
          onChange={setTransferTarget}
          options={accounts.map((a) => ({ value: a.id, label: a.username }))}
        />
      </Modal>
    </div>
  );
}
