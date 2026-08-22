import { useEffect } from "react";
import { App, Form, Input, Modal } from "antd";
import { useTranslation } from "react-i18next";
import type { Position, PositionRequest } from "../../api/positions";
import { createPosition, updatePosition } from "../../api/positions";

export interface PositionFormModalProps {
  open: boolean;
  position: Position | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  code: string;
  name: string;
}

export function PositionFormModal({ open, position, onClose, onSaved }: PositionFormModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();

  useEffect(() => {
    if (!open) return;
    if (position) {
      form.setFieldsValue({ code: position.code, name: position.name });
    } else {
      form.resetFields();
    }
  }, [open, position, form]);

  const handleFinish = async (values: FormValues) => {
    const request: PositionRequest = { code: values.code, name: values.name };
    try {
      if (position) {
        await updatePosition(position.id, request);
        message.success(t("position.messages.updateSuccess"));
      } else {
        await createPosition(request);
        message.success(t("position.messages.createSuccess"));
      }
      onSaved();
    } catch {
      message.error(t("position.messages.saveError"));
    }
  };

  return (
    <Modal
      title={position ? t("position.form.editTitle") : t("position.form.createTitle")}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={t("position.form.save")}
      cancelText={t("position.form.cancel")}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label={t("position.form.code")} name="code" rules={[{ required: true, message: t("position.form.codeRequired") }]}>
          <Input />
        </Form.Item>
        <Form.Item label={t("position.form.name")} name="name" rules={[{ required: true, message: t("position.form.nameRequired") }]}>
          <Input />
        </Form.Item>
      </Form>
    </Modal>
  );
}
