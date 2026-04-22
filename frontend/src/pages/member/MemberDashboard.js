// src/pages/member/MemberDashboard.js
import React, { useEffect, useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI, payoutAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { Link } from 'react-router-dom';

export default function MemberDashboard() {
  const { user } = useAuth();
  const [groups,       setGroups]       = useState([]);
  const [activeBids,   setActiveBids]   = useState([]); // groups with open bidding
  const [loading,      setLoading]      = useState(true);

  useEffect(() => {
    groupAPI.getJoinedGroups().then(async g => {
      const groupList = g.data.data || [];
      setGroups(groupList);

      // Check which active BIDDING groups have an open cycle
      const biddingGroups = groupList.filter(
        grp => grp.status === 'ACTIVE' && grp.payoutMethod === 'BIDDING'
      );
      const cycleChecks = await Promise.allSettled(
        biddingGroups.map(grp => payoutAPI.getCurrentCycle(grp.id))
      );
      const open = biddingGroups.filter((grp, i) => {
        const res = cycleChecks[i];
        if (res.status !== 'fulfilled') return false;
        const c = res.value.data.data;
        return c && c.status === 'IN_PROGRESS' && c.payoutMethod === 'BIDDING';
      });
      setActiveBids(open);
    }).finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h2>Welcome, {user?.name} 👋</h2>
          <p className="text-grey">Here's your Bhishi overview</p>
        </div>
        <Link to="/member/groups" className="btn btn-primary">My Groups</Link>
      </div>

      {/* Active bidding alerts */}
      {activeBids.length > 0 && (
        <div style={{ marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {activeBids.map(grp => (
            <div key={grp.id} style={{
              background: 'linear-gradient(135deg, var(--primary) 0%, #9333EA 100%)',
              borderRadius: 'var(--radius)',
              padding: '1rem 1.5rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              color: '#fff',
            }}>
              <div>
                <strong>🏷️ Bidding Open — {grp.name}</strong>
                <p style={{ opacity: 0.88, fontSize: '0.85rem', marginTop: '0.2rem' }}>
                  A bidding round is live! Place your bid before the admin closes it.
                </p>
              </div>
              <Link to={`/member/bidding/${grp.id}`} style={{
                background: '#fff', color: 'var(--primary)',
                borderRadius: 'var(--radius-sm)', padding: '0.5rem 1rem',
                fontWeight: 600, fontSize: '0.88rem', textDecoration: 'none',
                marginLeft: '1rem', flexShrink: 0,
              }}>
                Place Bid →
              </Link>
            </div>
          ))}
        </div>
      )}

      {/* Stats */}
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-label">Active Groups</div>
          <div className="stat-value">{groups.filter(g=>g.status==='ACTIVE').length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Pending Groups</div>
          <div className="stat-value">{groups.filter(g=>g.status==='PENDING_APPROVAL').length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Payouts Received</div>
          <div className="stat-value">{groups.filter(g=>g.hasReceivedPayout).length}</div>
          <div className="stat-sub">across all groups</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Open Bid Rounds</div>
          <div className="stat-value" style={{ color: activeBids.length > 0 ? 'var(--primary)' : undefined }}>
            {activeBids.length}
          </div>
        </div>
      </div>

      <h3 style={{ marginBottom: '1rem' }}>Your Groups</h3>
      {loading ? <div className="spinner-wrap"><div className="spinner"/></div> :
       groups.length === 0 ? (
        <div className="card empty">
          <div className="empty-icon">🏦</div>
          <h3>No groups yet</h3>
          <p>Join a group using a group code from your admin</p>
          <Link to="/member/groups" className="btn btn-primary" style={{marginTop:'1rem'}}>Join a Group</Link>
        </div>
       ) : (
        <div className="grid-3">
          {groups.map(g => {
            const hasOpenBid = activeBids.some(ab => ab.id === g.id);
            return (
              <Link key={g.id} to={`/member/groups/${g.id}`} style={{textDecoration:'none'}}>
                <div className="card-sm" style={{cursor:'pointer',transition:'box-shadow 0.2s', position:'relative'}}
                     onMouseEnter={e=>e.currentTarget.style.boxShadow='0 4px 20px rgba(108,99,255,0.15)'}
                     onMouseLeave={e=>e.currentTarget.style.boxShadow='var(--shadow)'}>
                  {hasOpenBid && (
                    <div style={{
                      position:'absolute', top: 8, right: 8,
                      background:'var(--primary)', color:'#fff',
                      borderRadius: 20, padding:'2px 8px',
                      fontSize:'0.72rem', fontWeight:600,
                    }}>🏷️ Bid Open</div>
                  )}
                  <div className="flex-between" style={{marginBottom:'0.5rem'}}>
                    <h4>{g.name}</h4>
                    <span className={`badge badge-${g.status==='ACTIVE'?'success':g.status==='COMPLETED'?'info':'warning'}`}>{g.status}</span>
                  </div>
                  <p className="text-grey text-sm">₹{g.contributionPerMember}/month</p>
                  <p className="text-grey text-sm">{g.currentMemberCount}/{g.maxMembers} members</p>
                  <p className="text-grey text-sm">Cycle {g.currentCycleMonth} • {g.payoutMethod?.replace('_',' ')}</p>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </Layout>
  );
}