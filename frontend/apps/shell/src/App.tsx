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
import { DocumentLibraryPage } from "./pages/Audit/DocumentLibrary/DocumentLibraryPage";
import { RiskScoringGroupsPage } from "./pages/Audit/RiskScoring/RiskScoringGroupsPage";
import { RiskScoringCriteriaPage } from "./pages/Audit/RiskScoring/RiskScoringCriteriaPage";
import { RiskScoringWeightPage } from "./pages/Audit/RiskScoring/RiskScoringWeightPage";
import { RiskScoringCoefficientMatrixPage } from "./pages/Audit/RiskScoring/RiskScoringCoefficientMatrixPage";
import { RiskScoringUserAssignmentPage } from "./pages/Audit/RiskScoring/RiskScoringUserAssignmentPage";
import { RiskScoringAuditObjectsPage } from "./pages/Audit/RiskScoring/RiskScoringAuditObjectsPage";
import { RiskScoringExecPage } from "./pages/Audit/RiskScoringExec/RiskScoringExecPage";
import { GlobalModalKeyboardShortcuts } from "./components/GlobalModalKeyboardShortcuts";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
  const { t } = useTranslation();
  return (
    <>
      <GlobalModalKeyboardShortcuts />
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
          <Route path="audit/master-data/document-library" element={<DocumentLibraryPage />} />
          <Route path="audit/master-data/position" element={<MasterDataGroupPage group="POSITION" title={t("auditMasterData.groups.POSITION")} />} />
          <Route path="audit/master-data/department" element={<MasterDataGroupPage group="DEPARTMENT" title={t("auditMasterData.groups.DEPARTMENT")} />} />
          <Route path="audit/master-data/year" element={<MasterDataGroupPage group="YEAR" title={t("auditMasterData.groups.YEAR")} />} />
          <Route path="audit/master-data/business-segment" element={<MasterDataGroupPage group="BUSINESS_SEGMENT" title={t("auditMasterData.groups.BUSINESS_SEGMENT")} />} />
          <Route path="audit/risk-scoring/master-data/groups" element={<RiskScoringGroupsPage />} />
          <Route path="audit/risk-scoring/master-data/criteria" element={<RiskScoringCriteriaPage />} />
          <Route path="audit/risk-scoring/master-data/weight" element={<RiskScoringWeightPage />} />
          <Route path="audit/risk-scoring/master-data/coefficient-matrix" element={<RiskScoringCoefficientMatrixPage />} />
          <Route path="audit/risk-scoring/master-data/user-assignment" element={<RiskScoringUserAssignmentPage />} />
          <Route path="audit/risk-scoring/master-data/audit-objects" element={<RiskScoringAuditObjectsPage />} />
          <Route path="audit/risk-scoring/scoring" element={<RiskScoringExecPage />} />
          {/* Cac sheet tiep theo cua "2. Cham diem.xlsx" se them tab moi vao RiskScoringExecPage, khong can them route moi. */}
        </Route>
      </Routes>
    </>
  );
}

export default App;
