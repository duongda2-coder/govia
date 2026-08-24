import { Navigate, Route, Routes } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/Login/LoginPage";
import { AppLayout } from "./layout/AppLayout";
import { Dashboard } from "./pages/Dashboard";
import { EmployeeListPage } from "./pages/People/EmployeeListPage";
import { PositionListPage } from "./pages/People/PositionListPage";
import { OrganizationUnitListPage } from "./pages/People/OrganizationUnitListPage";
import { RoleListPage } from "./pages/Admin/RoleListPage";
import { AccountListPage } from "./pages/Admin/AccountListPage";
import { TaskInbox } from "./pages/Workflow/TaskInbox";
import { ProcessInstances } from "./pages/Workflow/ProcessInstances";
import { ApprovalMatrixPage } from "./pages/Workflow/ApprovalMatrixPage";
import { MasterDataGroupPage } from "./pages/Audit/MasterDataGroupPage";
import { RiskScoringGroupsPage } from "./pages/Audit/RiskScoring/RiskScoringGroupsPage";
import { RiskScoringCriteriaPage } from "./pages/Audit/RiskScoring/RiskScoringCriteriaPage";
import { RiskScoringWeightPage } from "./pages/Audit/RiskScoring/RiskScoringWeightPage";
import { RiskScoringCoefficientMatrixPage } from "./pages/Audit/RiskScoring/RiskScoringCoefficientMatrixPage";
import { RiskScoringUserAssignmentPage } from "./pages/Audit/RiskScoring/RiskScoringUserAssignmentPage";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
  const { t } = useTranslation();
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="people/employees" element={<EmployeeListPage />} />
        <Route path="people/positions" element={<PositionListPage />} />
        <Route path="people/org-units" element={<OrganizationUnitListPage />} />
        <Route path="admin/roles" element={<RoleListPage />} />
        <Route path="admin/accounts" element={<AccountListPage />} />
        <Route path="workflow/tasks" element={<TaskInbox />} />
        <Route path="workflow/instances" element={<ProcessInstances />} />
        <Route path="workflow/approval-matrix" element={<ApprovalMatrixPage />} />
        <Route path="audit/master-data/audit" element={<MasterDataGroupPage group="AUDIT" title={t("auditMasterData.groups.AUDIT")} />} />
        <Route path="audit/master-data/finding" element={<MasterDataGroupPage group="FINDING" title={t("auditMasterData.groups.FINDING")} />} />
        <Route path="audit/master-data/risk" element={<MasterDataGroupPage group="RISK" title={t("auditMasterData.groups.RISK")} />} />
        <Route path="audit/master-data/control" element={<MasterDataGroupPage group="CONTROL" title={t("auditMasterData.groups.CONTROL")} />} />
        <Route path="audit/master-data/process" element={<MasterDataGroupPage group="PROCESS" title={t("auditMasterData.groups.PROCESS")} />} />
        <Route path="audit/master-data/compliance" element={<MasterDataGroupPage group="COMPLIANCE" title={t("auditMasterData.groups.COMPLIANCE")} />} />
        <Route path="audit/master-data/general" element={<MasterDataGroupPage group="GENERAL" title={t("auditMasterData.groups.GENERAL")} />} />
        <Route path="audit/risk-scoring/master-data/groups" element={<RiskScoringGroupsPage />} />
        <Route path="audit/risk-scoring/master-data/criteria" element={<RiskScoringCriteriaPage />} />
        <Route path="audit/risk-scoring/master-data/weight" element={<RiskScoringWeightPage />} />
        <Route path="audit/risk-scoring/master-data/coefficient-matrix" element={<RiskScoringCoefficientMatrixPage />} />
        <Route path="audit/risk-scoring/master-data/user-assignment" element={<RiskScoringUserAssignmentPage />} />
        {/* Sub-module "Cham diem" (thuc hien cham diem) se them route vao day khi co man hinh, song song voi "master-data". */}
      </Route>
    </Routes>
  );
}

export default App;
