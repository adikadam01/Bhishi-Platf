// src/pages/groupadmin/AdminDashboard.js
import React, { useEffect, useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import { Link } from 'react-router-dom';

export default function AdminDashboard() {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    groupAPI.getMyGroups()
      .then(r => setGroups(r.data.data || []))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <div><h2>Group Admin Dashboard</h2><p className="text-grey">Manage your Bhishi groups</p></div>
        <Link to="/admin/groups/create" className="btn btn-primary">+ Create Group</Link>
      </div>

      <div className="grid-3" style={{ marginBottom: '2rem' }}>
        <div className="stat-card"><div className="stat-label">Total Groups</div><div className="stat-value">{groups.length}</div></div>
        <div className="stat-card"><div className="stat-label">Active</div><div className="stat-value">{groups.filter(g=>g.status==='ACTIVE').length}</div></div>
        <div className="stat-card"><div className="stat-label">Pending Approval</div><div className="stat-value">{groups.filter(g=>g.status==='PENDING_APPROVAL').length}</div></div>
      </div>

      {loading ? <div className="spinner-wrap"><div className="spinner"/></div> :
       groups.length === 0 ? (
        <div className="card empty">
          <div className="empty-icon">🏦</div>
          <h3>No groups yet</h3>
          <p>Create your first Bhishi group</p>
          <Link to="/admin/groups/create" className="btn btn-primary" style={{marginTop:'1rem'}}>Create Group</Link>
        </div>
       ) : (
        <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
          {groups.map(g => (
            <div key={g.id} className="card">
              <div className="flex-between">
                <div>
                  <h3>{g.name}</h3>
                  <p className="text-grey text-sm">
                    ₹{g.contributionPerMember}/month • {g.currentMemberCount}/{g.maxMembers} members •
                    Cycle {g.currentCycleMonth} • {g.payoutMethod?.replace(/_/g,' ')}
                  </p>
                  {g.groupCode && <p className="text-sm" style={{marginTop:'0.25rem'}}>Code: <strong>{g.groupCode}</strong></p>}
                </div>
                <div className="flex gap-1">
                  <span className={`badge badge-${g.status==='ACTIVE'?'success':g.status==='COMPLETED'?'info':'warning'}`}>{g.status}</span>
                  {g.status === 'ACTIVE' && (<>
                    <Link to={`/admin/groups/${g.id}/members`}  className="btn btn-outline btn-sm">Members</Link>
                    <Link to={`/admin/groups/${g.id}/payments`} className="btn btn-outline btn-sm">Payments</Link>
                    <Link to={`/admin/groups/${g.id}/payout`}   className="btn btn-primary btn-sm">Payout</Link>
                  </>)}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </Layout>
  );
}
