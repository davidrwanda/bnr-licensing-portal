// @author David NTAMAKEMWA

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Shell from './components/layout/Shell';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import NewApplicationPage from './pages/applicant/NewApplicationPage';
import ApplicationDetailPage from './pages/ApplicationDetailPage';
import AuditLogPage from './pages/admin/AuditLogPage';
import UsersPage from './pages/admin/UsersPage';

function PrivateRoute({ children, allowedRoles }: { children: React.ReactNode; allowedRoles?: string[] }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to="/dashboard" replace />;
  return <Shell>{children}</Shell>;
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  if (user) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />

          <Route path="/dashboard" element={<PrivateRoute><DashboardPage /></PrivateRoute>} />

          <Route path="/apply" element={
            <PrivateRoute allowedRoles={['APPLICANT']}>
              <NewApplicationPage />
            </PrivateRoute>
          } />

          <Route path="/applications/:id" element={<PrivateRoute><ApplicationDetailPage /></PrivateRoute>} />

          <Route path="/admin/audit" element={
            <PrivateRoute allowedRoles={['ADMIN']}>
              <AuditLogPage />
            </PrivateRoute>
          } />

          <Route path="/admin/users" element={
            <PrivateRoute allowedRoles={['ADMIN']}>
              <UsersPage />
            </PrivateRoute>
          } />

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
