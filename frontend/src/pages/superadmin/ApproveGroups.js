// src/pages/superadmin/ApproveGroups.js
import React, { useEffect, useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function ApproveGroups() {
  const [groups,        setGroups]        = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [working,       setWorking]       = useState('');
  const [rejectModal,   setRejectModal]   = useState(null); // groupId being rejected
  const [rejectReason,  setRejectReason]  = useState('');
  const [expandedGroup, setExpandedGroup] = useState(null);

  const load = () => {
    setLoading(true);
    groupAPI.getPendingGroups()
      .then(r => setGroups(r.data.data || []))
      .catch(() => toast.error('Failed to load pending groups'))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleApprove = async (groupId) => {
    setWorking(groupId + '_APPROVE');
    try {
      await groupAPI.approveGroup({ groupId, action: 'APPROVE' });
      toast.success('✅ Group approved! A unique group code has been generated and sent to the admin.');
      load();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Approval failed');
    } finally { setWorking(''); }
  };

  const openRejectModal = (groupId) => {
    setRejectModal(groupId);
    setRejectReason('');
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) return toast.error('Please provide a reason for rejection');
    setWorking(rejectModal + '_REJECT');
    try {
      await groupAPI.approveGroup({ groupId: rejectModal, action: 'REJECT', reason: rejectReason });
      toast.success('Group rejected. The Group Admin has been notified.');
      setRejectModal(null);
      load();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Rejection failed');
    } finally { setWorking(''); }
  };

  const payoutLabel = (method) => {
    if (!method) return '—';
    return { FIXED_ROTATION: '🔄 Fixed Rotation', BIDDING: '🏷️ Bidding', CONTROLLED_RANDOM: '🎲 Controlled Random' }[method] || method;
  };

  const totalPot = (g) => ((g.contributionPerMember || 0) * (g.maxMembers || 0)).toLocaleString('en-IN');

  return (
    <Layout>
      <div style={{ marginBottom: '1.5rem' }}>
        <h2>Group Approvals</h2>
        <p className="text-grey">
          Review groups submitted by Group Admins. Approved groups get a unique join code and open for member registration.
        </p>
      </div>

      {/* How it works banner */}
      <div style={{
        display: 'flex', gap: '0', marginBottom: '2rem',
        background: 'var(--card-bg)', border: '1px solid var(--border)',
        borderRadius: '12px', overflow: 'hidden',
      }}>
        {[
          { icon: '🏛️', step: '1', label: 'Group Admin creates group', color: '#eff6ff' },
          { icon: '➡️', step: '',  label: '',                           color: '#f9fafb' },
          { icon: '🔍', step: '2', label: 'You review & approve',       color: '#f0fdf4' },
          { icon: '➡️', step: '',  label: '',                           color: '#f9fafb' },
          { icon: '🔑', step: '3', label: 'Code generated, group opens', color: '#fefce8' },
          { icon: '➡️', step: '',  label: '',                           color: '#f9fafb' },
          { icon: '👥', step: '4', label: 'Members join using code',    color: '#fdf4ff' },
        ].map((item, i) => (
          <div key={i} style={{
            flex: item.step ? 2 : 0.3, padding: '0.75rem', textAlign: 'center',
            background: item.step ? undefined : undefined, display: 'flex',
            flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          }}>
            <div style={{ fontSize: item.step ? '1.5rem' : '1rem' }}>{item.icon}</div>
            {item.step && (
              <>
                <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--grey)', marginTop: '0.2rem' }}>STEP {item.step}</div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text)', marginTop: '0.2rem' }}>{item.label}</div>
              </>
            )}
          </div>
        ))}
      </div>

      {loading ? (
        <div className="spinner-wrap"><div className="spinner" /></div>
      ) : groups.length === 0 ? (
        <div className="empty">
          <div className="empty-icon">✅</div>
          <h3>No pending groups</h3>
          <p>All group applications have been reviewed. New submissions will appear here.</p>
        </div>
      ) : (
        <>
          <div style={{ marginBottom: '1rem', color: 'var(--grey)', fontSize: '0.9rem' }}>
            <strong style={{ color: 'var(--warning)' }}>{groups.length}</strong> group{groups.length !== 1 ? 's' : ''} awaiting review
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {groups.map(g => (
              <div key={g.id} className="card" style={{ border: '1px solid var(--border)', borderLeft: '4px solid var(--warning)' }}>

                {/* Header row */}
                <div className="flex-between" style={{ marginBottom: '1rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                      <h3 style={{ margin: 0 }}>{g.name}</h3>
                      <span className="badge badge-warning">⏳ Pending Approval</span>
                    </div>
                    <p className="text-grey text-sm" style={{ marginTop: '0.3rem' }}>
                      Submitted by Group Admin &nbsp;•&nbsp; {g.createdAt ? new Date(g.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                    </p>
                  </div>
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => setExpandedGroup(expandedGroup === g.id ? null : g.id)}
                  >
                    {expandedGroup === g.id ? '▲ Less' : '▼ Details'}
                  </button>
                </div>

                {/* Summary tiles */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.75rem', marginBottom: '1rem' }}>
                  {[
                    { label: 'Payout Method',     value: payoutLabel(g.payoutMethod) },
                    { label: 'Total Pot',          value: `₹${totalPot(g)}` },
                    { label: 'Per Member / Month', value: `₹${g.contributionPerMember?.toLocaleString('en-IN')}` },
                    { label: 'Max Members',        value: g.maxMembers },
                    { label: 'Due Day',            value: g.dueDayOfMonth ? `${g.dueDayOfMonth}th of month` : '—' },
                    { label: 'Late Penalty',       value: g.penaltyAmount ? `₹${g.penaltyAmount}` : '—' },
                  ].map((tile, i) => (
                    <div key={i} style={{
                      padding: '0.65rem 0.9rem', background: 'var(--bg-secondary, #f9fafb)',
                      borderRadius: '8px', border: '1px solid var(--border)',
                    }}>
                      <div style={{ fontSize: '0.72rem', color: 'var(--grey)', marginBottom: '0.2rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>{tile.label}</div>
                      <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{tile.value}</div>
                    </div>
                  ))}
                </div>

                {/* Expanded details */}
                {expandedGroup === g.id && (
                  <div style={{
                    margin: '0 0 1rem', padding: '1rem',
                    background: 'var(--bg-secondary, #f5f5f5)',
                    borderRadius: '8px', border: '1px solid var(--border)',
                  }}>
                    <h4 style={{ margin: '0 0 0.75rem', fontSize: '0.9rem' }}>Full Group Details</h4>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem 1.5rem', fontSize: '0.85rem' }}>
                      <div><span style={{ color: 'var(--grey)' }}>Group ID:</span> <code style={{ fontSize: '0.8rem' }}>{g.id}</code></div>
                      <div><span style={{ color: 'var(--grey)' }}>Status:</span> {g.status}</div>
                      <div><span style={{ color: 'var(--grey)' }}>Current Members:</span> {g.currentMemberCount} / {g.maxMembers}</div>
                      <div><span style={{ color: 'var(--grey)' }}>Cycle Month:</span> {g.currentCycleMonth}</div>
                      {g.description && (
                        <div style={{ gridColumn: '1/-1' }}>
                          <span style={{ color: 'var(--grey)' }}>Description:</span> {g.description}
                        </div>
                      )}
                    </div>

                    <div style={{ marginTop: '1rem', padding: '0.75rem', background: '#fffbeb', borderRadius: '6px', border: '1px solid #fde68a', fontSize: '0.82rem', color: '#92400e' }}>
                      <strong>⚠️ Once approved:</strong> A unique 6-character join code will be generated and emailed to the Group Admin.
                      Members can then use this code to request to join the group. The Group Admin must approve each join request individually.
                    </div>
                  </div>
                )}

                {/* Action buttons */}
                <div className="flex gap-1" style={{ flexWrap: 'wrap' }}>
                  <button
                    className="btn btn-success"
                    disabled={!!working}
                    onClick={() => handleApprove(g.id)}
                    style={{ minWidth: '180px' }}
                  >
                    {working === g.id + '_APPROVE' ? '⏳ Approving...' : '✅ Approve — Open for Joining'}
                  </button>
                  <button
                    className="btn btn-danger"
                    disabled={!!working}
                    onClick={() => openRejectModal(g.id)}
                    style={{ minWidth: '140px' }}
                  >
                    ✗ Reject
                  </button>
                </div>

              </div>
            ))}
          </div>
        </>
      )}

      {/* Reject Modal */}
      {rejectModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
        }}>
          <div className="card" style={{ width: '100%', maxWidth: 440, margin: '1rem' }}>
            <h3 style={{ marginBottom: '0.5rem' }}>Reject Group</h3>
            <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>
              The Group Admin will be notified with your reason.
            </p>
            <div className="form-group">
              <label className="form-label">Reason for Rejection <span style={{ color: 'red' }}>*</span></label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. Contribution amount exceeds platform limits, or Group purpose is unclear..."
                value={rejectReason}
                onChange={e => setRejectReason(e.target.value)}
                style={{ resize: 'vertical' }}
              />
            </div>
            <div className="flex gap-1">
              <button
                className="btn btn-danger"
                disabled={working === rejectModal + '_REJECT'}
                onClick={handleReject}
                style={{ flex: 1 }}
              >
                {working === rejectModal + '_REJECT' ? 'Rejecting...' : 'Confirm Reject'}
              </button>
              <button
                className="btn btn-ghost"
                onClick={() => setRejectModal(null)}
                disabled={!!working}
                style={{ flex: 1 }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}