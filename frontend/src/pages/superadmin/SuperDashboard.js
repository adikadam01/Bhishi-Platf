// src/pages/superadmin/SuperDashboard.js
import React, { useEffect, useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import { Link } from 'react-router-dom';

export default function SuperDashboard() {
  const [pending,  setPending]  = useState([]);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    groupAPI.getPendingGroups()
      .then(r => setPending(r.data.data || []))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <div style={{ marginBottom: '1.5rem' }}>
        <h2>Super Admin Dashboard</h2>
        <p className="text-grey">Platform overview and controls</p>
      </div>

      <div className="grid-3" style={{ marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-label">Pending Group Approvals</div>
          <div className="stat-value" style={{ color: pending.length > 0 ? 'var(--warning)' : 'var(--success)' }}>
            {pending.length}
          </div>
          <div className="stat-sub">
            <Link to="/super/groups" className="text-primary">Review now →</Link>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Scheduler Jobs</div>
          <div className="stat-value">6</div>
          <div className="stat-sub"><Link to="/super/scheduler" className="text-primary">Manage →</Link></div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Platform Status</div>
          <div className="stat-value" style={{ color: 'var(--success)', fontSize: '1.2rem', paddingTop: '0.4rem' }}>● Operational</div>
        </div>
      </div>

      {pending.length > 0 && (
        <div className="card" style={{ borderLeft: '4px solid var(--warning)' }}>
          <div className="flex-between" style={{ marginBottom: '1rem' }}>
            <h3>⏳ Groups Awaiting Approval</h3>
            <Link to="/super/groups" className="btn btn-primary btn-sm">Review All</Link>
          </div>
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Group</th><th>Method</th><th>Members</th><th>Pot</th></tr></thead>
              <tbody>
                {pending.slice(0,5).map(g => (
                  <tr key={g.id}>
                    <td><strong>{g.name}</strong></td>
                    <td><span className="badge badge-purple">{g.payoutMethod?.replace(/_/g,' ')}</span></td>
                    <td>{g.currentMemberCount}/{g.maxMembers}</td>
                    <td>₹{(g.contributionPerMember * g.maxMembers)?.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {loading && <div className="spinner-wrap"><div className="spinner"/></div>}
    </Layout>
  );
}
