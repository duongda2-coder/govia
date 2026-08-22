import { useEffect } from "react";
import { App, Form, Input, Modal, Select } from "antd";
import { useTranslation } from "react-i18next";
import type { OrganizationUnit, OrganizationUnitRequest } from "../../api/orgUnits";
import { createOrgUnit, updateOrgUnit } from "../../api/orgUnits";
import type { Employee } from "../../api/employees";

const LEVEL_CODES = ["001", "002", "003", "004"] as const;

export interface OrganizationUnitFormModalProps {
  open: boolean;
  orgUnit: OrganizationUnit | null;
  orgUnits: OrganizationUnit[];
  employees: Employee[];
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  code: string;
  name: string;
  levelCode?: string;
  parentId?: string;
  managerEmployeeId?: string;
}

export function OrganizationUnitFormModal({ open, orgUnit, orgUnits, employees, onClose, onSaved }: OrganizationUnitFormModalProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();

  useEffect(() => {
    if (!open) return;
    if (orgUnit) {
      form.setFieldsValue({
        code: orgUnit.code,
        name: orgUnit.name,
        levelCode: orgUnit.levelCode ?? undefined,
        parentId: orgUnit.parentId ?? undefined,
        managerEmployeeId: orgUnit.managerEmployeeId ?? undefined,
      });
    } else {
      form.resetFields();
    }
  }, [open, orgUnit, form]);

  const handleFinish = async (values: FormValues) => {
    const request: OrganizationUnitRequest = {
      code: values.code,
      name: values.name,
      levelCode: values.levelCode || null,
      parentId: values.parentId || null,
      managerEmployeeId: values.managerEmployeeId || null,
    };
    try {
      if (orgUnit) {
        await updateOrgUnit(orgUnit.id, request);
        message.success(t("orgUnit.messages.updateSuccess"));
      } else {
        await createOrgUnit(request);
        message.success(t("orgUnit.messages.createSuccess"));
      }
      onSaved();
    } catch {
      message.error(t("orgUnit.messages.saveError"));
    }
  };

  const parentOptions = orgUnits
    .filter((u) => u.id !== orgUnit?.id)
    .map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }));

  const managerOptions = employees.map((e) => ({ value: e.id, label: `${e.employeeCode} - ${e.fullName}` }));

  return (
    <Modal
      title={orgUnit ? t("orgUnit.form.editTitle") : t("orgUnit.form.createTitle")}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText={t("orgUnit.form.save")}
      cancelText={t("orgUnit.form.cancel")}
      destroyOnClose
      width={520}
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label={t("orgUnit.form.code")} name="code" rules={[{ required: true, message: t("orgUnit.form.codeRequired") }]}>
          <Input />
        </Form.Item>
        <Form.Item label={t("orgUnit.form.name")} name="name" rules={[{ required: true, message: t("orgUnit.form.nameRequired") }]}>
          <Input />
        </Form.Item>
        <Form.Item label={t("orgUnit.form.level")} name="levelCode">
          <Select allowClear options={LEVEL_CODES.map((code) => ({ value: code, label: t(`orgUnit.level.${code}`) }))} />
        </Form.Item>
        <Form.Item label={t("orgUnit.form.parent")} name="parentId">
          <Select allowClear showSearch optionFilterProp="label" placeholder={t("orgUnit.form.noParent")} options={parentOptions} />
        </Form.Item>
        <Form.Item label={t("orgUnit.form.manager")} name="managerEmployeeId">
          <Select allowClear showSearch optionFilterProp="label" placeholder={t("orgUnit.form.noManager")} options={managerOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
