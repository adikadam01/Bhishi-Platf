// src/pages/groupadmin/ManagePayments.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { paymentAPI, groupAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function ManagePayments() {
  const { id } = useParams();
  const [summary, setSummary] = useState(null);
  const [group,   setGroup]   = useState(null);
  const [loading, setLoading] = useState(true);
  const now = new Date();

  const load = () => {
    Promise.all([
      groupAPI.getGroupById(id),
      paymentAPI.getCycleSummary(id, now.getMonth()+1, now.getFullYear())
    ]).then(([g, s]) => { setGroup(g.data.data); setSummary(s.data.data); })
    .finally(() => setLoading(false));
  };
  useEffect(load, [id]);

  const waivePenalty = async (paymentId) => {
    try {
      await paymentAPI.waivePenalty({ paymentId, reason: 'Waived by admin' });
      toast.success('Penalty waived');
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Failed to waive penalty'); }
  };

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <h2 style={{ marginBottom: '0.5rem' }}>Payments — {group?.name}</h2>
      <p className="text-grey" style={{ marginBottom: '1.5rem' }}>
        Cycle {now.getMonth()+1}/{now.getFullYear()} • Due on {group?.dueDayOfMonth}th
      </p>

      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <div className="stat-card"><div className="stat-label">Total Expected</div><div className="stat-value">₹{summary?.totalExpected?.toLocaleString()}</div></div>
        <div className="stat-card"><div className="stat-label">Collected</div><div className="stat-value" style={{color:'var(--success)'}}>₹{summary?.totalCollected?.toLocaleString()}</div></div>
        <div className="stat-card"><div className="stat-label">Paid</div><div className="stat-value">{summary?.paidCount}/{summary?.totalMembers}</div></div>
        <div className="stat-card"><div className="stat-label">Late / Pending</div><div className="stat-value" style={{color:'var(--danger)'}}>{summary?.lateCount} / {summary?.pendingCount}</div></div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Member Payment Status</h3>
        <div className="table-wrapper">
          <table>
            <thead><tr><th>Member</th><th>Base</th><th>Penalty</th><th>Total</th><th>Status</th><th>Paid On</th><th>Action</th></tr></thead>
            <tbody>
              {summary?.payments?.map(p => (
                <tr key={p.id}>
                  <td><strong>{p.memberName}</strong></td>
                  <td>₹{p.baseAmount}</td>
                  <td>{p.penaltyAmount > 0 ? <span className="badge badge-warning">₹{p.penaltyAmount}</span> : '—'}</td>
                  <td><strong>₹{p.totalAmount}</strong></td>
                  <td><span className={`badge badge-${p.status==='PAID'?'success':p.status==='LATE'?'danger':p.status==='WAIVED'?'grey':'warning'}`}>{p.status}</span></td>
                  <td className="text-grey text-sm">{p.paidAt ? new Date(p.paidAt).toLocaleDateString() : '—'}</td>
                  <td>
                    {p.penaltyAmount > 0 && p.status !== 'PAID' && (
                      <button className="btn btn-ghost btn-sm" onClick={() => waivePenalty(p.id)}>Waive Penalty</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
