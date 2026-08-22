import { App, Form, Input, Modal } from "antd";
import { useTranslation } from "react-i18next";
import { changePassword } from "../api/auth";

export interface ChangePasswordModalProps {
  open: boolean;
  onClose: () => void;
}

interface FormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export function ChangePasswordModal({ open, onClose }: ChangePasswordModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();

  const handleFinish = async (values: FormValues) => {
    try {
      await changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword });
      message.success(t("account.changePassword.success"));
      form.resetFields();
      onClose();
    } catch {
      message.error(t("account.changePassword.error"));
    }
  };

  return (
    <Modal
      title={t("account.changePassword.title")}
      open={open}
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
      okText={t("account.changePassword.submit")}
      cancelText={t("employee.form.cancel")}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label={t("account.changePassword.currentPassword")}
          name="currentPassword"
          rules={[{ required: true, message: t("account.changePassword.currentPasswordRequired") }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          label={t("account.changePassword.newPassword")}
          name="newPassword"
          rules={[
            { required: true, message: t("account.changePassword.newPasswordRequired") },
            { min: 8, message: t("account.changePassword.newPasswordRequired") },
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          label={t("account.changePassword.confirmPassword")}
          name="confirmPassword"
          dependencies={["newPassword"]}
          rules={[
            { required: true, message: t("account.changePassword.confirmPasswordRequired") },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || value === getFieldValue("newPassword")) return Promise.resolve();
                return Promise.reject(new Error(t("account.changePassword.confirmPasswordMismatch")));
              },
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
