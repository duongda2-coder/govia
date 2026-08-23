import { useEffect, useState } from "react";
import { App, Drawer, Form, Input, Select, DatePicker, Button, Space, Row, Col, Divider, Typography } from "antd";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import type { Employee, EmployeeRankLevel, EmployeeRequest } from "../../api/employees";
import { createEmployee, createEmployeeAccount, updateEmployee } from "../../api/employees";
import type { OrganizationUnit } from "../../api/orgUnits";
import type { Position } from "../../api/positions";
import { ResetPasswordModal } from "../../components/ResetPasswordModal";

export interface EmployeeFormDrawerProps {
  open: boolean;
  employee: Employee | null;
  orgUnits: OrganizationUnit[];
  positions: Position[];
  employees: Employee[];
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  employeeCode: string;
  fullName: string;
  email?: string;
  personalEmail?: string;
  phone?: string;
  orgUnitId?: string;
  positionId?: string;
  hireDate?: dayjs.Dayjs;
  dateOfBirth?: dayjs.Dayjs;
  gender?: "MALE" | "FEMALE" | "OTHER";
  idNumber?: string;
  managerId?: string;
  rankLevel?: EmployeeRankLevel;
  accountUsername?: string;
  accountPassword?: string;
}

const RANK_LEVELS: EmployeeRankLevel[] = ["N1", "N2", "N3", "N4", "N5", "N6"];

export function EmployeeFormDrawer({ open, employee, orgUnits, positions, employees, onClose, onSaved }: EmployeeFormDrawerProps) {
  const [form] = Form.useForm<FormValues>();
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [resetPasswordOpen, setResetPasswordOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (employee) {
      form.setFieldsValue({
        employeeCode: employee.employeeCode,
        fullName: employee.fullName,
        email: employee.email ?? undefined,
        personalEmail: employee.personalEmail ?? undefined,
        phone: employee.phone ?? undefined,
        orgUnitId: employee.orgUnitId ?? undefined,
        positionId: employee.positionId ?? undefined,
        hireDate: employee.hireDate ? dayjs(employee.hireDate) : undefined,
        dateOfBirth: employee.dateOfBirth ? dayjs(employee.dateOfBirth) : undefined,
        gender: employee.gender ?? undefined,
        idNumber: employee.idNumber ?? undefined,
        managerId: employee.managerId ?? undefined,
        rankLevel: employee.rankLevel ?? undefined,
      });
    } else {
      form.resetFields();
    }
  }, [open, employee, form]);

  const handleFinish = async (values: FormValues) => {
    const request: EmployeeRequest = {
      employeeCode: values.employeeCode,
      fullName: values.fullName,
      email: values.email || null,
      personalEmail: values.personalEmail || null,
      phone: values.phone || null,
      orgUnitId: values.orgUnitId || null,
      positionId: values.positionId || null,
      hireDate: values.hireDate ? values.hireDate.format("YYYY-MM-DD") : null,
      dateOfBirth: values.dateOfBirth ? values.dateOfBirth.format("YYYY-MM-DD") : null,
      gender: values.gender || null,
      idNumber: values.idNumber || null,
      managerId: values.managerId || null,
      rankLevel: values.rankLevel || null,
    };

    try {
      const saved = employee ? await updateEmployee(employee.id, request) : await createEmployee(request);
      message.success(t(employee ? "employee.messages.updateSuccess" : "employee.messages.createSuccess"));

      if (values.accountUsername && values.accountPassword) {
        try {
          await createEmployeeAccount(saved.id, { username: values.accountUsername, password: values.accountPassword });
          message.success(t("employee.messages.accountCreateSuccess"));
        } catch {
          message.error(t("employee.messages.accountCreateError"));
        }
      }

      onSaved();
    } catch {
      message.error(t("employee.messages.saveError"));
    }
  };

  const managerOptions = employees
    .filter((e) => e.id !== employee?.id)
    .map((e) => ({ value: e.id, label: `${e.employeeCode} - ${e.fullName}` }));

  const canCreateAccount = !employee || !employee.username;

  return (
    <Drawer
      title={employee ? t("employee.form.editTitle") : t("employee.form.createTitle")}
      open={open}
      onClose={onClose}
      width={480}
      destroyOnClose
      extra={
        <Space>
          <Button onClick={onClose}>{t("employee.form.cancel")}</Button>
          <Button type="primary" onClick={() => form.submit()}>
            {t("employee.form.save")}
          </Button>
        </Space>
      }
    >
      <Form<FormValues> form={form} layout="vertical" onFinish={handleFinish}>
        <Row gutter={12}>
          <Col span={12}>
            <Form.Item
              label={t("employee.form.employeeCode")}
              name="employeeCode"
              rules={[{ required: true, message: t("employee.form.employeeCodeRequired") }]}
            >
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={t("employee.form.gender")} name="gender">
              <Select
                allowClear
                options={[
                  { value: "MALE", label: t("employee.gender.MALE") },
                  { value: "FEMALE", label: t("employee.gender.FEMALE") },
                  { value: "OTHER", label: t("employee.gender.OTHER") },
                ]}
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          label={t("employee.form.fullName")}
          name="fullName"
          rules={[{ required: true, message: t("employee.form.fullNameRequired") }]}
        >
          <Input />
        </Form.Item>

        <Row gutter={12}>
          <Col span={12}>
            <Form.Item label={t("employee.form.email")} name="email" rules={[{ type: "email", message: t("employee.form.invalidEmail") }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={t("employee.form.personalEmail")}
              name="personalEmail"
              rules={[{ type: "email", message: t("employee.form.invalidEmail") }]}
            >
              <Input />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={12}>
          <Col span={12}>
            <Form.Item label={t("employee.form.phone")} name="phone">
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={t("employee.form.idNumber")} name="idNumber">
              <Input />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item label={t("employee.form.orgUnit")} name="orgUnitId">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            options={orgUnits.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
          />
        </Form.Item>

        <Row gutter={12}>
          <Col span={12}>
            <Form.Item label={t("employee.form.position")} name="positionId">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                options={positions.map((p) => ({ value: p.id, label: `${p.code} - ${p.name}` }))}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={t("employee.form.manager")} name="managerId">
              <Select allowClear showSearch optionFilterProp="label" placeholder={t("employee.form.noManager")} options={managerOptions} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={12}>
          <Col span={12}>
            <Form.Item label={t("employee.form.hireDate")} name="hireDate">
              <DatePicker style={{ width: "100%" }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label={t("employee.form.dateOfBirth")} name="dateOfBirth">
              <DatePicker style={{ width: "100%" }} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          label={t("employee.form.rankLevel")}
          name="rankLevel"
          tooltip={t("employee.form.rankLevelHint")}
        >
          <Select allowClear options={RANK_LEVELS.map((level) => ({ value: level, label: level }))} />
        </Form.Item>

        <Divider orientation="left" plain>
          {t("employee.form.accountSection")}
        </Divider>

        {canCreateAccount ? (
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item
                label={t("employee.form.accountUsername")}
                name="accountUsername"
                dependencies={["accountPassword"]}
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value && !getFieldValue("accountPassword")) return Promise.resolve();
                      if (!value) return Promise.reject(new Error(t("employee.form.accountUsernameRequired")));
                      return Promise.resolve();
                    },
                  }),
                ]}
              >
                <Input autoComplete="off" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label={t("employee.form.accountPassword")}
                name="accountPassword"
                dependencies={["accountUsername"]}
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value && !getFieldValue("accountUsername")) return Promise.resolve();
                      if (!value || value.length < 8) return Promise.reject(new Error(t("employee.form.accountPasswordRequired")));
                      return Promise.resolve();
                    },
                  }),
                ]}
              >
                <Input.Password autoComplete="new-password" />
              </Form.Item>
            </Col>
          </Row>
        ) : (
          <Space direction="vertical">
            <Typography.Text type="secondary">
              {t("employee.form.accountExisting", { username: employee?.username })}
            </Typography.Text>
            <Button size="small" onClick={() => setResetPasswordOpen(true)}>
              {t("employee.form.resetPassword")}
            </Button>
          </Space>
        )}
      </Form>

      <ResetPasswordModal
        open={resetPasswordOpen}
        employeeId={employee?.id ?? null}
        username={employee?.username ?? null}
        onClose={() => setResetPasswordOpen(false)}
      />
    </Drawer>
  );
}
