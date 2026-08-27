import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Select, Space, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import type { ProcessDefinitionSummary, ProcessInstanceSummary, TaskSummary } from "../../api/workflow";
import {
  cancelProcessInstance,
  completeTask,
  delegateTask,
  listAllTasks,
  listProcessDefinitions,
  listProcessInstances,
  reassignTask,
  resolveTask,
  startProcessInstance,
} from "../../api/workflow";
import { listAccounts, type AccountSummary } from "../../api/accounts";
import { useAuth } from "../../auth/AuthContext";

interface StartProcessFormValues {
  processDefinitionKey: string;
  businessKey?: string;
  approverUserId?: string;
}

type TransferMode = "reassign" | "delegate";

export function ProcessInstances() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("WORKFLOW.INSTANCE.VIEW");
  const canStart = hasPermission("WORKFLOW.INSTANCE.START");
  const canCancel = hasPermission("WORKFLOW.INSTANCE.CANCEL");
  const canViewAllTasks = hasPermission("WORKFLOW.TASK.VIEW_ALL");
  const canCompleteTasks = hasPermission("WORKFLOW.TASK.COMPLETE");

  const [instances, setInstances] = useState<ProcessInstanceSummary[]>([]);
  const [definitions, setDefinitions] = useState<ProcessDefinitionSummary[]>([]);
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<StartProcessFormValues>();

  const [viewingInstance, setViewingInstance] = useState<ProcessInstanceSummary | null>(null);
  const [instanceTasks, setInstanceTasks] = useState<TaskSummary[]>([]);
  const [tasksLoading, setTasksLoading] = useState(false);
  const [actingTaskId, setActingTaskId] = useState<string | null>(null);
  const [transferTask, setTransferTask] = useState<TaskSummary | null>(null);
  const [transferMode, setTransferMode] = useState<TransferMode>("reassign");
  const [transferTarget, setTransferTarget] = useState<string | null>(null);
  const [transferSubmitting, setTransferSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setInstances(await listProcessInstances());
    } catch {
      message.error(t("workflow.instance.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openStartModal = async () => {
    try {
      const [defs, accts] = await Promise.all([listProcessDefinitions(), listAccounts()]);
      setDefinitions(defs);
      setAccounts(accts);
      setModalOpen(true);
    } catch {
      message.error(t("workflow.instance.messages.loadError"));
    }
  };

  const handleStart = async () => {
    let values: StartProcessFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await startProcessInstance({
        processDefinitionKey: values.processDefinitionKey,
        businessKey: values.businessKey || undefined,
        variables: values.approverUserId ? { approverUserId: values.approverUserId } : undefined,
      });
      message.success(t("workflow.instance.messages.startSuccess"));
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch {
      message.error(t("workflow.instance.messages.startError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (instance: ProcessInstanceSummary) => {
    try {
      await cancelProcessInstance(instance.id);
      message.success(t("workflow.instance.messages.cancelSuccess"));
      await load();
    } catch {
      message.error(t("workflow.instance.messages.cancelError"));
    }
  };

  const loadInstanceTasks = useCallback(async (instance: ProcessInstanceSummary) => {
    setTasksLoading(true);
    try {
      const all = await listAllTasks();
      setInstanceTasks(all.filter((task) => task.processInstanceId === instance.id));
      if (accounts.length === 0) {
        setAccounts(await listAccounts());
      }
    } catch {
      message.error(t("workflow.task.messages.loadError"));
    } finally {
      setTasksLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [message, t]);

  const openInstanceTasks = (instance: ProcessInstanceSummary) => {
    setViewingInstance(instance);
    loadInstanceTasks(instance);
  };

  const handleTaskComplete = async (task: TaskSummary, variables?: Record<string, unknown>) => {
    setActingTaskId(task.id);
    try {
      await completeTask(task.id, variables);
      message.success(t("workflow.task.messages.completeSuccess"));
      if (viewingInstance) await loadInstanceTasks(viewingInstance);
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setActingTaskId(null);
    }
  };

  const handleTaskResolve = async (task: TaskSummary) => {
    setActingTaskId(task.id);
    try {
      await resolveTask(task.id);
      message.success(t("workflow.task.messages.resolveSuccess"));
      if (viewingInstance) await loadInstanceTasks(viewingInstance);
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setActingTaskId(null);
    }
  };

  const openTransfer = (task: TaskSummary, mode: TransferMode) => {
    setTransferTask(task);
    setTransferMode(mode);
    setTransferTarget(null);
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
      if (viewingInstance) await loadInstanceTasks(viewingInstance);
    } catch {
      message.error(t("workflow.task.messages.actionError"));
    } finally {
      setTransferSubmitting(false);
    }
  };

  const columns: TableProps<ProcessInstanceSummary>["columns"] = [
    { title: t("workflow.instance.columns.processDefinitionKey"), dataIndex: "processDefinitionKey", width: 200 },
    {
      title: t("workflow.instance.columns.businessKey"),
      dataIndex: "businessKey",
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("workflow.instance.columns.startTime"),
      dataIndex: "startTime",
      width: 200,
      render: (v: string) => new Date(v).toLocaleString(),
    },
    {
      title: t("workflow.instance.columns.status"),
      key: "status",
      width: 140,
      render: (_: unknown, record) =>
        record.endTime ? (
          <Tag>{t("workflow.instance.status.ended")}</Tag>
        ) : (
          <Tag color="blue">{t("workflow.instance.status.running")}</Tag>
        ),
    },
    {
      title: t("workflow.instance.columns.actions"),
      key: "actions",
      width: 220,
      render: (_: unknown, record) => (
        <Space>
          {!record.endTime && canViewAllTasks ? (
            <Button size="small" onClick={() => openInstanceTasks(record)}>
              {t("workflow.instance.actions.viewTasks")}
            </Button>
          ) : null}
          {!record.endTime && canCancel ? (
            <Button size="small" danger onClick={() => handleCancel(record)}>
              {t("workflow.instance.actions.cancel")}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const taskColumns: TableProps<TaskSummary>["columns"] = [
    { title: t("workflow.task.columns.name"), dataIndex: "name" },
    { title: t("workflow.task.columns.assignee"), dataIndex: "assignee", render: (v: string | null) => v ?? "-" },
    {
      title: t("workflow.task.columns.delegation"),
      dataIndex: "delegationState",
      render: (v: TaskSummary["delegationState"]) =>
        v === "PENDING" ? (
          <Tag color="gold">{t("workflow.task.delegation.pending")}</Tag>
        ) : v === "RESOLVED" ? (
          <Tag color="blue">{t("workflow.task.delegation.resolved")}</Tag>
        ) : null,
    },
    {
      title: t("workflow.task.columns.actions"),
      key: "actions",
      render: (_: unknown, task) => {
        if (!canCompleteTasks) return null;
        const busy = actingTaskId === task.id;
        return (
          <Space wrap>
            {task.delegationState === "PENDING" ? (
              <Button size="small" type="primary" loading={busy} onClick={() => handleTaskResolve(task)}>
                {t("workflow.task.actions.resolve")}
              </Button>
            ) : (
              <>
                <Button size="small" type="primary" loading={busy} onClick={() => handleTaskComplete(task, { approved: true })}>
                  {t("workflow.task.actions.approve")}
                </Button>
                <Button size="small" danger loading={busy} onClick={() => handleTaskComplete(task, { approved: false })}>
                  {t("workflow.task.actions.reject")}
                </Button>
              </>
            )}
            <Button size="small" onClick={() => openTransfer(task, "reassign")}>
              {t("workflow.task.actions.forward")}
            </Button>
            <Button size="small" onClick={() => openTransfer(task, "delegate")}>
              {t("workflow.task.actions.delegate")}
            </Button>
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
      <Typography.Title level={4}>{t("workflow.instance.title")}</Typography.Title>
      <CrudTable<ProcessInstanceSummary>
        tableId="workflow.processInstances"
        columns={columns}
        dataSource={instances}
        rowKey="id"
        loading={loading}
        onAdd={canStart ? openStartModal : undefined}
      />

      <Modal
        title={t("workflow.instance.startModal.title")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleStart}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="processDefinitionKey"
            label={t("workflow.instance.startModal.processDefinition")}
            rules={[{ required: true }]}
          >
            <Select options={definitions.map((d) => ({ value: d.key, label: `${d.name ?? d.key} (v${d.version})` }))} />
          </Form.Item>
          <Form.Item name="businessKey" label={t("workflow.instance.startModal.businessKey")}>
            <Input />
          </Form.Item>
          <Form.Item name="approverUserId" label={t("workflow.instance.startModal.approver")}>
            <Select
              allowClear
              options={accounts.map((a) => ({ value: a.id, label: a.username }))}
              placeholder={t("workflow.instance.startModal.approverPlaceholder")}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={t("workflow.instance.tasksModal.title")}
        open={!!viewingInstance}
        onCancel={() => setViewingInstance(null)}
        footer={null}
        width={800}
        destroyOnClose
      >
        <Table<TaskSummary>
          columns={taskColumns}
          dataSource={instanceTasks}
          rowKey="id"
          loading={tasksLoading}
          pagination={false}
          size="small"
        />
      </Modal>

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
