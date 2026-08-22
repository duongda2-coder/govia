import { App, Form, Input, Modal } from "antd";
import { useTranslation } from "react-i18next";
import { resetEmployeeAccountPassword } from "../api/employees";

export interface ResetPasswordModalProps {
  open: boolean;
  employeeId: string | null;
  username: string | null;
  onClose: () => void;
}

interface FormValues {
  newPassword: string;
  confirmPassword: string;
}

export function ResetPasswordModal({ open, employeeId, username, onClose }: ResetPasswordModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();

  const handleFinish = async (values: FormValues) => {
    if (!employeeId) return;
    try {
      await resetEmployeeAccountPassword(employeeId, values.newPassword);
      message.success(t("account.resetPassword.success"));
      form.resetFields();
      onClose();
    } catch {
      message.error(t("account.resetPassword.error"));
    }
  };

  return (
    <Modal
      title={t("account.resetPassword.title", { username })}
      open={open}
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
      okText={t("account.resetPassword.submit")}
      cancelText={t("employee.form.cancel")}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label={t("account.resetPassword.newPassword")}
          name="newPassword"
          rules={[
            { required: true, message: t("account.resetPassword.newPasswordRequired") },
            { min: 8, message: t("account.resetPassword.newPasswordRequired") },
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          label={t("account.resetPassword.confirmPassword")}
          name="confirmPassword"
          dependencies={["newPassword"]}
          rules={[
            { required: true, message: t("account.resetPassword.confirmPasswordRequired") },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || value === getFieldValue("newPassword")) return Promise.resolve();
                return Promise.reject(new Error(t("account.resetPassword.confirmPasswordMismatch")));
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
