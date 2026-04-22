// src/App.js
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';

import LandingPage     from './pages/LandingPage';
import LoginPage       from './pages/auth/LoginPage';
import RegisterPage    from './pages/auth/RegisterPage';

import MemberDashboard from './pages/member/MemberDashboard';
import MyGroups        from './pages/member/MyGroups';
import GroupDetail     from './pages/member/GroupDetail';
import PaymentPage     from './pages/member/PaymentPage';
import PayoutHistory   from './pages/member/PayoutHistory';
import UrgencyPage     from './pages/member/UrgencyPage';
import BiddingPage     from './pages/member/BiddingPage';   // ← NEW

import AdminDashboard  from './pages/groupadmin/AdminDashboard';
import CreateGroup     from './pages/groupadmin/CreateGroup';
import ManageMembers   from './pages/groupadmin/ManageMembers';
import ManagePayments  from './pages/groupadmin/ManagePayments';
import ManagePayout    from './pages/groupadmin/ManagePayout';

import SuperDashboard  from './pages/superadmin/SuperDashboard';
import ApproveGroups   from './pages/superadmin/ApproveGroups';
import SchedulerPanel  from './pages/superadmin/SchedulerPanel';

// ── Route Guards ──────────────────────────────────────────

function PrivateRoute({ children, roles }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="spinner-wrap"><div className="spinner" /></div>;
  if (!user)   return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function PublicOnlyRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="spinner-wrap"><div className="spinner" /></div>;
  if (user) {
    if (user.role === 'SUPER_ADMIN') return <Navigate to="/super/dashboard"  replace />;
    if (user.role === 'GROUP_ADMIN') return <Navigate to="/admin/dashboard"  replace />;
    return <Navigate to="/member/dashboard" replace />;
  }
  return children;
}

// ── App Router ────────────────────────────────────────────

function AppRoutes() {
  return (
    <Routes>
      {/* Landing page — always accessible */}
      <Route path="/"         element={<LandingPage />} />

      {/* Auth — only when logged out */}
      <Route path="/login"    element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
      <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />

      {/* Member routes */}
      <Route path="/member/dashboard"    element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><MemberDashboard /></PrivateRoute>} />
      <Route path="/member/groups"       element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><MyGroups /></PrivateRoute>} />
      <Route path="/member/groups/:id"   element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><GroupDetail /></PrivateRoute>} />
      <Route path="/member/payments/:id" element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><PaymentPage /></PrivateRoute>} />
      <Route path="/member/payouts/:id"  element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><PayoutHistory /></PrivateRoute>} />
      <Route path="/member/urgency/:id"  element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><UrgencyPage /></PrivateRoute>} />
      <Route path="/member/bidding/:id"  element={<PrivateRoute roles={['MEMBER','GROUP_ADMIN','SUPER_ADMIN']}><BiddingPage /></PrivateRoute>} />  {/* ← NEW */}

      {/* Group Admin routes */}
      <Route path="/admin/dashboard"           element={<PrivateRoute roles={['GROUP_ADMIN','SUPER_ADMIN']}><AdminDashboard /></PrivateRoute>} />
      <Route path="/admin/groups/create"       element={<PrivateRoute roles={['GROUP_ADMIN','SUPER_ADMIN']}><CreateGroup /></PrivateRoute>} />
      <Route path="/admin/groups/:id/members"  element={<PrivateRoute roles={['GROUP_ADMIN','SUPER_ADMIN']}><ManageMembers /></PrivateRoute>} />
      <Route path="/admin/groups/:id/payments" element={<PrivateRoute roles={['GROUP_ADMIN','SUPER_ADMIN']}><ManagePayments /></PrivateRoute>} />
      <Route path="/admin/groups/:id/payout"   element={<PrivateRoute roles={['GROUP_ADMIN','SUPER_ADMIN']}><ManagePayout /></PrivateRoute>} />

      {/* Super Admin routes */}
      <Route path="/super/dashboard" element={<PrivateRoute roles={['SUPER_ADMIN']}><SuperDashboard /></PrivateRoute>} />
      <Route path="/super/groups"    element={<PrivateRoute roles={['SUPER_ADMIN']}><ApproveGroups /></PrivateRoute>} />
      <Route path="/super/scheduler" element={<PrivateRoute roles={['SUPER_ADMIN']}><SchedulerPanel /></PrivateRoute>} />

      {/* Catch-all → landing */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" toastOptions={{ duration: 3500 }} />
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}