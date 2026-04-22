// src/components/shared/Layout.js
import React from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import { FiHome, FiUsers, FiCreditCard, FiAward, FiAlertCircle,
         FiPlusCircle, FiSettings, FiLogOut } from 'react-icons/fi';

// ── Navbar (shown inside the app after login) ─────────────
export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    toast.success('Logged out');
    navigate('/');
  };

  const dashboardLink =
    user?.role === 'SUPER_ADMIN' ? '/super/dashboard' :
    user?.role === 'GROUP_ADMIN' ? '/admin/dashboard' :
                                   '/member/dashboard';

  return (
    <nav className="navbar">
      {/* Brand — goes back to landing page */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        <Link to="/" className="navbar-brand">🪙 Bhishi Platform</Link>

        {/* Inline nav links */}
        <div className="navbar-links" style={{ display: 'flex', gap: '0.1rem' }}>
          <Link to="/#how-it-works" className="nav-link">How it Works</Link>
          <Link to="/#features"     className="nav-link">Features</Link>
          <Link to="/#roles"        className="nav-link">Roles</Link>
        </div>
      </div>

      {/* Right side */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <span style={{ fontSize: '0.85rem', color: 'var(--grey)' }}>
          {user?.name} &nbsp;
          <span className={`badge badge-${
            user?.role === 'SUPER_ADMIN' ? 'danger' :
            user?.role === 'GROUP_ADMIN' ? 'warning' : 'info'}`}>
            {user?.role?.replace(/_/g, ' ')}
          </span>
        </span>

        {/* Dashboard button */}
        <Link to={dashboardLink} className="btn btn-primary btn-sm">
          Dashboard
        </Link>

        <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
          <FiLogOut /> Logout
        </button>
      </div>
    </nav>
  );
}

// ── Member Sidebar ────────────────────────────────────────
function MemberSidebar() {
  return (
    <aside className="sidebar">
      <p className="sidebar-title">Member</p>
      <NavLink to="/member/dashboard" className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiHome /> Dashboard
      </NavLink>
      <NavLink to="/member/groups"    className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiUsers /> My Groups
      </NavLink>
    </aside>
  );
}

// ── Group Admin Sidebar ───────────────────────────────────
function AdminSidebar() {
  return (
    <aside className="sidebar">
      <p className="sidebar-title">Group Admin</p>
      <NavLink to="/admin/dashboard"     className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiHome /> Dashboard
      </NavLink>
      <NavLink to="/admin/groups/create" className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiPlusCircle /> Create Group
      </NavLink>
      <p className="sidebar-title">Also</p>
      <NavLink to="/member/groups" className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiUsers /> Member View
      </NavLink>
    </aside>
  );
}

// ── Super Admin Sidebar ───────────────────────────────────
function SuperSidebar() {
  return (
    <aside className="sidebar">
      <p className="sidebar-title">Super Admin</p>
      <NavLink to="/super/dashboard"  className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiHome /> Dashboard
      </NavLink>
      <NavLink to="/super/groups"     className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiUsers /> Approve Groups
      </NavLink>
      <NavLink to="/super/scheduler"  className={({isActive}) => `sidebar-link${isActive?' active':''}`}>
        <FiSettings /> Scheduler
      </NavLink>
    </aside>
  );
}

// ── Main Layout wrapper ───────────────────────────────────
export default function Layout({ children }) {
  const { user } = useAuth();
  const sidebar =
    user?.role === 'SUPER_ADMIN' ? <SuperSidebar /> :
    user?.role === 'GROUP_ADMIN' ? <AdminSidebar /> :
                                   <MemberSidebar />;
  return (
    <>
      <Navbar />
      <div className="layout">
        {sidebar}
        <main className="main-content">{children}</main>
      </div>
    </>
  );
}