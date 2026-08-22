import { useEffect, useState } from "react";
import { App, Form, Modal, Select } from "antd";
import { useTranslation } from "react-i18next";
import type { AccountSummary } from "../../api/accounts";
import { assignAccountRoles } from "../../api/accounts";
import type { Role } from "../../api/roles";

export interface AssignRolesModalProps {
  open: boolean;
  account: AccountSummary | null;
  roles: Role[];
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  roleIds: string[];
}

export function AssignRolesModal({ open, account, roles, onClose, onSaved }: AssignRolesModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open || !account) return;
    const roleByCode = new Map(roles.map((r) => [r.code, r.id]));
    const roleIds = account.roleCodes.map((code) => roleByCode.get(code)).filter((id): id is string => !!id);
    form.setFieldsValue({ roleIds });
  }, [open, account, roles, form]);

  const handleFinish = async (values: FormValues) => {
    if (!account) return;
    setSaving(true);
    try {
      await assignAccountRoles(account.id, values.roleIds ?? []);
      message.success(t("account.assignRoles.success"));
      onSaved();
    } catch {
      message.error(t("account.assignRoles.error"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={t("account.assignRoles.title", { username: account?.username })}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={t("account.assignRoles.submit")}
      cancelText={t("employee.form.cancel")}
      confirmLoading={saving}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label={t("account.assignRoles.roles")} name="roleIds">
          <Select
            mode="multiple"
            allowClear
            optionFilterProp="label"
            options={roles.map((r) => ({ value: r.id, label: `${r.code} - ${r.name}` }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
