// src/pages/member/GroupDetail.js
import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { groupAPI, payoutAPI, urgencyAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function GroupDetail() {
  const { id } = useParams();
  const [group,        setGroup]        = useState(null);
  const [members,      setMembers]      = useState([]);
  const [payouts,      setPayouts]      = useState(null);
  const [urgency,      setUrgency]      = useState([]);
  const [currentCycle, setCurrentCycle] = useState(null);
  const [loading,      setLoading]      = useState(true);

  useEffect(() => {
    Promise.all([
      groupAPI.getGroupById(id),
      groupAPI.getGroupMembers(id),
      payoutAPI.getHistory(id),
      urgencyAPI.getByGroup(id),
    ]).then(async ([g, m, p, u]) => {
      const grpData = g.data.data;
      setGroup(grpData);
      setMembers(m.data.data || []);
      setPayouts(p.data.data);
      setUrgency(u.data.data || []);

      // Fetch current active cycle so we can show bidding banner
      try {
        const c = await payoutAPI.getCurrentCycle(id);
        setCurrentCycle(c.data.data);
      } catch {
        setCurrentCycle(null);
      }
    }).catch(() => toast.error('Could not load group details'))
    .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;
  if (!group)  return <Layout><div className="empty"><h3>Group not found</h3></div></Layout>;

  // Is there an active bidding round right now?
  const activeBidding = currentCycle &&
    currentCycle.payoutMethod === 'BIDDING' &&
    currentCycle.status === 'IN_PROGRESS';

  return (
    <Layout>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h2>{group.name}</h2>
          <p className="text-grey">{group.description}</p>
        </div>
        <div className="flex gap-1">
          <Link to={`/member/payments/${id}`}  className="btn btn-primary">Pay Now</Link>
          <Link to={`/member/payouts/${id}`}   className="btn btn-outline">Payout History</Link>
          <Link to={`/member/urgency/${id}`}   className="btn btn-outline">Urgency</Link>
        </div>
      </div>

      {/* Active Bidding Banner */}
      {activeBidding && (
        <div style={{
          background: 'linear-gradient(135deg, var(--primary) 0%, #9333EA 100%)',
          borderRadius: 'var(--radius)',
          padding: '1.25rem 1.5rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          color: '#fff',
          boxShadow: '0 4px 20px rgba(108,99,255,0.35)',
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.3rem' }}>
              <span style={{ fontSize: '1.3rem' }}>🏷️</span>
              <strong style={{ fontSize: '1.05rem' }}>Bidding Round is Open!</strong>
            </div>
            <p style={{ opacity: 0.88, fontSize: '0.88rem' }}>
              Cycle #{currentCycle.cycleNumber} — Total pot: ₹{currentCycle.totalCollected?.toLocaleString()}.
              Place your bid before the admin closes the round.
            </p>
          </div>
          <Link
            to={`/member/bidding/${id}`}
            style={{
              background: '#fff',
              color: 'var(--primary)',
              borderRadius: 'var(--radius-sm)',
              padding: '0.6rem 1.25rem',
              fontWeight: 600,
              fontSize: '0.9rem',
              textDecoration: 'none',
              whiteSpace: 'nowrap',
              marginLeft: '1rem',
              flexShrink: 0,
            }}
          >
            Place Bid →
          </Link>
        </div>
      )}

      {/* Stats */}
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-label">Total Pot</div>
          <div className="stat-value">₹{group.totalAmount?.toLocaleString()}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Your Contribution</div>
          <div className="stat-value">₹{group.contributionPerMember?.toLocaleString()}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Members</div>
          <div className="stat-value">{group.currentMemberCount}/{group.maxMembers}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Current Cycle</div>
          <div className="stat-value">{group.currentCycleMonth}</div>
          <div className="stat-sub">{group.payoutMethod?.replace(/_/g,' ')}</div>
        </div>
      </div>

      <div className="grid-2">
        {/* Members list */}
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Members</h3>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  {group.payoutMethod === 'FIXED_ROTATION' && <th>Order</th>}
                  <th>Payout</th>
                </tr>
              </thead>
              <tbody>
                {members.map(m => (
                  <tr key={m.id}>
                    <td>
                      <strong>{m.name}</strong><br/>
                      <span className="text-grey text-sm">{m.phone}</span>
                    </td>
                    <td>
                      <span className={`badge badge-${m.status==='ACTIVE'?'success':m.status==='PENDING'?'warning':'grey'}`}>
                        {m.status}
                      </span>
                    </td>
                    {group.payoutMethod === 'FIXED_ROTATION' && (
                      <td>#{m.rotationOrder || '—'}</td>
                    )}
                    <td>
                      {m.hasReceivedPayout
                        ? <span className="badge badge-info">Cycle {m.payoutReceivedOnCycle}</span>
                        : <span className="text-grey text-sm">Pending</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Payout history + bidding quick link */}
        <div className="card">
          <div className="flex-between" style={{ marginBottom: '1rem' }}>
            <h3>Recent Payouts</h3>
            {group.payoutMethod === 'BIDDING' && (
              <Link to={`/member/bidding/${id}`} className="btn btn-outline btn-sm">
                🏷️ {activeBidding ? 'Bid Now' : 'View Bidding'}
              </Link>
            )}
          </div>

          {payouts?.cycles?.length === 0 ? (
            <div className="empty"><p>No payouts yet</p></div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr><th>Cycle</th><th>Winner</th><th>Amount</th></tr>
                </thead>
                <tbody>
                  {payouts?.cycles?.slice(0,5).map(c => (
                    <tr key={c.id}>
                      <td>#{c.cycleNumber}</td>
                      <td>{c.winnerName || '—'}</td>
                      <td>{c.winnerAmount ? `₹${c.winnerAmount?.toLocaleString()}` : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {group.payoutMethod === 'BIDDING' && !activeBidding && (
            <p className="text-grey text-sm" style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border)' }}>
              🏷️ This group uses <strong>Bidding</strong> payout. When the admin opens a bidding round, a banner will appear here so you can place your bid.
            </p>
          )}
        </div>
      </div>
    </Layout>
  );
}