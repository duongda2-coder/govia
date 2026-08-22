import { useEffect, useState } from "react";
import { App, Form, Modal, Select, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { AccountSummary } from "../../api/accounts";
import { copyAccountRoles } from "../../api/accounts";

export interface CopyRolesModalProps {
  open: boolean;
  targetAccount: AccountSummary | null;
  accounts: AccountSummary[];
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  sourceAccountId: string;
}

export function CopyRolesModal({ open, targetAccount, accounts, onClose, onSaved }: CopyRolesModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    form.resetFields();
  }, [open, form]);

  const handleFinish = async (values: FormValues) => {
    if (!targetAccount) return;
    setSaving(true);
    try {
      await copyAccountRoles(targetAccount.id, values.sourceAccountId);
      message.success(t("account.copyRoles.success"));
      onSaved();
    } catch {
      message.error(t("account.copyRoles.error"));
    } finally {
      setSaving(false);
    }
  };

  const sourceOptions = accounts
    .filter((a) => a.id !== targetAccount?.id)
    .map((a) => ({
      value: a.id,
      label: `${a.username}${a.employeeName ? " - " + a.employeeName : ""} (${a.roleCodes.join(", ") || t("account.noRoles")})`,
    }));

  return (
    <Modal
      title={t("account.copyRoles.title", { username: targetAccount?.username })}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={t("account.copyRoles.submit")}
      cancelText={t("employee.form.cancel")}
      confirmLoading={saving}
      destroyOnClose
    >
      <Typography.Paragraph type="secondary">{t("account.copyRoles.description")}</Typography.Paragraph>
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label={t("account.copyRoles.sourceAccount")}
          name="sourceAccountId"
          rules={[{ required: true, message: t("account.copyRoles.sourceAccountRequired") }]}
        >
          <Select showSearch optionFilterProp="label" options={sourceOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
