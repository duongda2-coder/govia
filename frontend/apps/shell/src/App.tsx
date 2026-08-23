import { Navigate, Route, Routes } from "react-router-dom";
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

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
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
      </Route>
    </Routes>
  );
}

export default App;
