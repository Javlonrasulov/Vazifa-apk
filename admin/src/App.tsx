import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import EmployeesPage from './pages/EmployeesPage';
import DepartmentsPage from './pages/DepartmentsPage';
import SystemUsersPage from './pages/SystemUsersPage';
import Layout from './components/Layout';
import EmployeesLayout from './components/EmployeesLayout';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('accessToken');
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route index element={<Navigate to="/employees" replace />} />
          <Route path="employees" element={<EmployeesLayout />}>
            <Route index element={<EmployeesPage />} />
            <Route path="departments" element={<DepartmentsPage />} />
          </Route>
          <Route path="system-users" element={<SystemUsersPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
