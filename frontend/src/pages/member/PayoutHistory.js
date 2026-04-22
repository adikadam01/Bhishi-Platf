// src/pages/member/PayoutHistory.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { payoutAPI, groupAPI } from '../../services/api';

export default function PayoutHistory() {
  const { id } = useParams();
  const [history, setHistory] = useState(null);
  const [group,   setGroup]   = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([payoutAPI.getHistory(id), groupAPI.getGroupById(id)])
      .then(([h, g]) => { setHistory(h.data.data); setGroup(g.data.data); })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <h2 style={{ marginBottom: '0.5rem' }}>Payout History — {group?.name}</h2>
      <p className="text-grey" style={{ marginBottom: '1.5rem' }}>
        {history?.completedCycles}/{history?.totalCycles} cycles completed
      </p>

      <div className="grid-3" style={{ marginBottom: '2rem' }}>
        <div className="stat-card"><div className="stat-label">Total Cycles</div><div className="stat-value">{history?.totalCycles}</div></div>
        <div className="stat-card"><div className="stat-label">Completed</div><div className="stat-value">{history?.completedCycles}</div></div>
        <div className="stat-card"><div className="stat-label">Remaining</div><div className="stat-value">{history?.remainingCycles}</div></div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>All Cycles</h3>
        {history?.cycles?.length === 0 ? (
          <div className="empty"><div className="empty-icon">🎯</div><p>No payout cycles yet</p></div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Cycle</th><th>Month/Year</th><th>Method</th><th>Winner</th><th>Amount</th><th>Status</th></tr></thead>
              <tbody>
                {history?.cycles?.map(c => (
                  <tr key={c.id}>
                    <td><strong>#{c.cycleNumber}</strong></td>
                    <td>{c.cycleMonth}/{c.cycleYear}</td>
                    <td><span className="badge badge-purple">{c.payoutMethod?.replace(/_/g,' ')}</span></td>
                    <td>{c.winnerName || '—'}</td>
                    <td>{c.winnerAmount ? `₹${c.winnerAmount?.toLocaleString()}` : '—'}</td>
                    <td><span className={`badge badge-${c.status==='COMPLETED'?'success':c.status==='IN_PROGRESS'?'info':'grey'}`}>{c.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
