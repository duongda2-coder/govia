import { useCallback, useEffect, useState } from "react";
import { Alert, App, Button, Card, Form, Input, InputNumber, Modal, Result, Space, Switch, Tabs, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { ApiOutlined, SaveOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, getApiErrorMessage, useClientSearchColumn } from "@govia/ui-kit";
import {
  getEmailDemoSetting,
  listEmailDemoLogs,
  saveEmailDemoSetting,
  sendEmailDemo,
  testEmailDemoConnection,
  type EmailDemoActionStatus,
  type EmailDemoLog,
  type EmailDemoSendRequest,
  type EmailDemoSettingRequest,
} from "../../api/emailDemo";
import { useAuth } from "../../auth/AuthContext";

/** Man hinh "Demo gui email" (Quan tri he thong) - DOC LAP voi quy trinh nghiep vu, chi de demo
 * gui mail: tab 1 cau hinh SMTP (luu DB, khong can restart backend), tab 2 soan/gui mail thu + lich su.
 * Chi SUPER_ADMIN, giong quy uoc Nhat ky thao tac. */
export function EmailDemoPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;
  if (!isSuperAdmin) {
    return <Result status="403" title={t("emailDemo.forbidden")} />;
  }
  return (
    <Card title={t("emailDemo.title")}>
      <Tabs
        items={[
          { key: "send", label: t("emailDemo.tabs.send"), children: <EmailDemoSendTab /> },
          { key: "setting", label: t("emailDemo.tabs.setting"), children: <EmailDemoSettingTab /> },
        ]}
      />
    </Card>
  );
}

interface SettingFormValues extends EmailDemoSettingRequest {}

function EmailDemoSettingTab() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [form] = Form.useForm<SettingFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [saved, setSaved] = useState(false);
  const [passwordSet, setPasswordSet] = useState(false);
  const smtpAuth = Form.useWatch("smtpAuth", form);

  useEffect(() => {
    setLoading(true);
    getEmailDemoSetting()
      .then((s) => {
        setSaved(s.saved);
        setPasswordSet(s.passwordSet);
        form.setFieldsValue({ ...s, host: s.host ?? "", password: "" });
      })
      .catch((err) => message.error(getApiErrorMessage(err, t("emailDemo.messages.loadError"))))
      .finally(() => setLoading(false));
  }, [form, message, t]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const s = await saveEmailDemoSetting(values);
      setSaved(true);
      setPasswordSet(s.passwordSet);
      form.setFieldValue("password", "");
      message.success(t("emailDemo.messages.saveSuccess"));
    } catch (err) {
      message.error(getApiErrorMessage(err, t("emailDemo.messages.saveError")));
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    setTesting(true);
    try {
      await testEmailDemoConnection();
      message.success(t("emailDemo.messages.testSuccess"));
    } catch (err) {
      message.error(getApiErrorMessage(err, t("emailDemo.messages.testError")), 8);
    } finally {
      setTesting(false);
    }
  };

  return (
    <div style={{ maxWidth: 640 }}>
      <Alert type="info" showIcon style={{ marginBottom: 16 }} message={t("emailDemo.setting.hint")} />
      {!saved && !loading && <Alert type="warning" showIcon style={{ marginBottom: 16 }} message={t("emailDemo.setting.notSaved")} />}
      <Form<SettingFormValues> form={form} layout="vertical" disabled={loading}>
        <Form.Item name="host" label={t("emailDemo.setting.host")} rules={[{ required: true, message: t("emailDemo.validation.required") }]}>
          <Input placeholder="smtp.gmail.com" />
        </Form.Item>
        <Form.Item name="port" label={t("emailDemo.setting.port")} rules={[{ required: true, message: t("emailDemo.validation.required") }]}>
          <InputNumber min={1} max={65535} style={{ width: 160 }} />
        </Form.Item>
        <Form.Item name="smtpAuth" label={t("emailDemo.setting.smtpAuth")} valuePropName="checked">
          <Switch />
        </Form.Item>
        {smtpAuth && (
          <>
            <Form.Item name="username" label={t("emailDemo.setting.username")}>
              <Input autoComplete="off" />
            </Form.Item>
            <Form.Item
              name="password"
              label={t("emailDemo.setting.password")}
              extra={passwordSet ? t("emailDemo.setting.passwordKeep") : undefined}
            >
              <Input.Password autoComplete="new-password" placeholder={passwordSet ? "••••••••" : undefined} />
            </Form.Item>
          </>
        )}
        <Space size="large">
          <Form.Item name="starttls" label={t("emailDemo.setting.starttls")} valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="ssl" label={t("emailDemo.setting.ssl")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Space>
        <Form.Item
          name="fromAddress"
          label={t("emailDemo.setting.fromAddress")}
          rules={[
            { required: true, message: t("emailDemo.validation.required") },
            { type: "email", message: t("emailDemo.validation.email") },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="fromName" label={t("emailDemo.setting.fromName")}>
          <Input />
        </Form.Item>
      </Form>
      <Space>
        <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
          {t("common.save")}
        </Button>
        <Button icon={<ApiOutlined />} loading={testing} disabled={!saved} onClick={handleTest}>
          {t("emailDemo.setting.test")}
        </Button>
      </Space>
    </div>
  );
}

const ACTION_STATUS_COLORS: Record<EmailDemoActionStatus, string> = { PENDING: "gold", APPROVED: "green", REJECTED: "red" };
const DEFAULT_DETAIL_URL = "https://example.com/govia/chi-tiet-ho-so-demo";
const AUTO_REFRESH_MS = 10000;

const SAMPLE_HTML_BODY = `<h2 style="color:#2563eb">GOVIA - Email demo</h2>
<p>Xin chào,</p>
<p>Đây là email <b>demo</b> được gửi từ hệ thống GOVIA để kiểm tra cấu hình SMTP.</p>
<p>Trân trọng,<br/>GOVIA</p>`;

function EmailDemoSendTab() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<EmailDemoLog>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const [form] = Form.useForm<EmailDemoSendRequest>();
  const [logs, setLogs] = useState<EmailDemoLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<EmailDemoLog | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [initialValues, setInitialValues] = useState<EmailDemoSendRequest | undefined>();
  const includeActions = Form.useWatch("includeActions", form);

  const load = useCallback(
    async (silent = false) => {
      if (!silent) setLoading(true);
      try {
        setLogs(await listEmailDemoLogs());
      } catch (err) {
        if (!silent) message.error(getApiErrorMessage(err, t("emailDemo.messages.loadError")));
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    void load();
    // Tu cap nhat de thay ngay trang thai khi nguoi nhan bam Phe duyet/Tu choi trong mail
    const timer = window.setInterval(() => void load(true), AUTO_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [load]);

  const openCompose = (values: EmailDemoSendRequest) => {
    setInitialValues(values);
    setModalOpen(true);
  };

  const handleSend = async () => {
    const values = await form.validateFields();
    setSending(true);
    try {
      const result = await sendEmailDemo(values);
      if (result.success) {
        message.success(t("emailDemo.messages.sendSuccess", { to: result.toAddress }));
        setModalOpen(false);
      } else {
        message.error(t("emailDemo.messages.sendFailed", { error: result.errorMessage ?? "" }), 8);
      }
      await load();
    } catch (err) {
      message.error(getApiErrorMessage(err, t("emailDemo.messages.sendError")), 8);
    } finally {
      setSending(false);
    }
  };

  const columns: TableProps<EmailDemoLog>["columns"] = [
    {
      title: t("emailDemo.columns.sentAt"),
      dataIndex: "sentAt",
      width: 170,
      sorter: (a, b) => a.sentAt.localeCompare(b.sentAt),
      render: (v: string) => dayjs(v).format("DD/MM/YYYY HH:mm:ss"),
    },
    { title: t("emailDemo.columns.to"), width: 240, ...getSearchColumnProps("toAddress", searchLabels) },
    { title: t("emailDemo.columns.subject"), ...getSearchColumnProps("subject", searchLabels) },
    {
      title: t("emailDemo.columns.status"),
      dataIndex: "success",
      width: 130,
      filters: [
        { text: t("emailDemo.status.success"), value: true },
        { text: t("emailDemo.status.failed"), value: false },
      ],
      onFilter: (value, record) => record.success === value,
      render: (v: boolean) => <Tag color={v ? "green" : "red"}>{v ? t("emailDemo.status.success") : t("emailDemo.status.failed")}</Tag>,
    },
    {
      title: t("emailDemo.columns.error"),
      dataIndex: "errorMessage",
      width: 320,
      ellipsis: true,
      render: (v: string | null) => (v ? <Typography.Text type="danger" title={v}>{v}</Typography.Text> : "-"),
    },
    {
      title: t("emailDemo.columns.actionStatus"),
      dataIndex: "actionStatus",
      width: 150,
      filters: (["PENDING", "APPROVED", "REJECTED"] as const).map((s) => ({ text: t(`emailDemo.actionStatus.${s}`), value: s })),
      onFilter: (value, record) => record.actionStatus === value,
      render: (v: EmailDemoActionStatus | null) => (v ? <Tag color={ACTION_STATUS_COLORS[v]}>{t(`emailDemo.actionStatus.${v}`)}</Tag> : "-"),
    },
    {
      title: t("emailDemo.columns.actionAt"),
      dataIndex: "actionAt",
      width: 170,
      render: (v: string | null) => (v ? dayjs(v).format("DD/MM/YYYY HH:mm:ss") : "-"),
    },
    { title: t("emailDemo.columns.smtpHost"), dataIndex: "smtpHost", width: 200, render: (v: string | null) => v ?? "-" },
    { title: t("emailDemo.columns.sentBy"), dataIndex: "sentBy", width: 140, render: (v: string | null) => v ?? "-" },
  ];

  return (
    <>
      <CrudTable<EmailDemoLog>
        tableId="admin-email-demo-logs"
        columns={columns}
        dataSource={logs}
        rowKey="id"
        loading={loading}
        onSelectionChange={(_, rows) => setSelected(rows[0] ?? null)}
        onAdd={() =>
          openCompose({
            to: "",
            cc: "",
            subject: t("emailDemo.compose.sampleSubject"),
            body: SAMPLE_HTML_BODY,
            html: true,
            includeActions: true,
            detailUrl: DEFAULT_DETAIL_URL,
          })
        }
        onCopy={() =>
          selected &&
          openCompose({ to: selected.toAddress, cc: selected.ccAddress ?? "", subject: selected.subject, body: selected.body ?? "", html: selected.html,
            includeActions: selected.actionStatus != null, detailUrl: selected.detailUrl ?? DEFAULT_DETAIL_URL })
        }
        copyDisabled={!selected}
      />
      <Modal
        open={modalOpen}
        title={t("emailDemo.compose.title")}
        width={720}
        destroyOnClose
        okText={t("emailDemo.compose.send")}
        confirmLoading={sending}
        onOk={handleSend}
        onCancel={() => setModalOpen(false)}
      >
        <Form<EmailDemoSendRequest> form={form} layout="vertical" initialValues={initialValues} preserve={false}>
          <Form.Item
            name="to"
            label={t("emailDemo.compose.to")}
            extra={t("emailDemo.compose.multiHint")}
            rules={[{ required: true, message: t("emailDemo.validation.required") }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="cc" label={t("emailDemo.compose.cc")}>
            <Input />
          </Form.Item>
          <Form.Item name="subject" label={t("emailDemo.compose.subject")} rules={[{ required: true, message: t("emailDemo.validation.required") }]}>
            <Input maxLength={500} />
          </Form.Item>
          <Form.Item name="html" label={t("emailDemo.compose.html")} valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="body" label={t("emailDemo.compose.body")}>
            <Input.TextArea rows={8} maxLength={10000} showCount />
          </Form.Item>
          <Form.Item name="includeActions" label={t("emailDemo.compose.includeActions")} extra={t("emailDemo.compose.includeActionsHint")} valuePropName="checked">
            <Switch />
          </Form.Item>
          {includeActions && (
            <Form.Item name="detailUrl" label={t("emailDemo.compose.detailUrl")} rules={[{ type: "url", message: t("emailDemo.validation.url") }]}>
              <Input placeholder={DEFAULT_DETAIL_URL} />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </>
  );
}
