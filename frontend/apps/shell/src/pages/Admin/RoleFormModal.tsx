import { useEffect } from "react";
import { App, Form, Input, Modal } from "antd";
import { useTranslation } from "react-i18next";
import type { Role, RoleRequest } from "../../api/roles";
import { createRole, updateRole } from "../../api/roles";

export interface RoleFormModalProps {
  open: boolean;
  role: Role | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  code: string;
  name: string;
  description?: string;
}

export function RoleFormModal({ open, role, onClose, onSaved }: RoleFormModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();

  useEffect(() => {
    if (!open) return;
    if (role) {
      form.setFieldsValue({ code: role.code, name: role.name, description: role.description ?? undefined });
    } else {
      form.resetFields();
    }
  }, [open, role, form]);

  const handleFinish = async (values: FormValues) => {
    const request: RoleRequest = { code: values.code, name: values.name, description: values.description || null };
    try {
      if (role) {
        await updateRole(role.id, request);
        message.success(t("role.messages.updateSuccess"));
      } else {
        await createRole(request);
        message.success(t("role.messages.createSuccess"));
      }
      onSaved();
    } catch {
      message.error(t("role.messages.saveError"));
    }
  };

  return (
    <Modal
      title={role ? t("role.form.editTitle") : t("role.form.createTitle")}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={t("role.form.save")}
      cancelText={t("role.form.cancel")}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label={t("role.form.code")} name="code" rules={[{ required: true, message: t("role.form.codeRequired") }]}>
          <Input disabled={role?.systemDefined} />
        </Form.Item>
        <Form.Item label={t("role.form.name")} name="name" rules={[{ required: true, message: t("role.form.nameRequired") }]}>
          <Input disabled={role?.systemDefined} />
        </Form.Item>
        <Form.Item label={t("role.form.description")} name="description">
          <Input.TextArea rows={3} disabled={role?.systemDefined} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
