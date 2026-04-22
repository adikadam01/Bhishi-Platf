// src/pages/member/UrgencyPage.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { urgencyAPI, groupAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';

export default function UrgencyPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [requests, setRequests] = useState([]);
  const [group,    setGroup]    = useState(null);
  const [loading,  setLoading]  = useState(true);
  const [raising,  setRaising]  = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ reason: '', votingHours: 24 });

  const load = () => {
    Promise.all([urgencyAPI.getByGroup(id), groupAPI.getGroupById(id)])
      .then(([r, g]) => { setRequests(r.data.data || []); setGroup(g.data.data); })
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const raiseRequest = async () => {
    if (!form.reason || form.reason.length < 10) return toast.error('Reason must be at least 10 characters');
    setRaising(true);
    try {
      await urgencyAPI.raise({ groupId: id, reason: form.reason, votingHours: form.votingHours });
      toast.success('Urgency request raised! Members have been notified.');
      setShowForm(false);
      setForm({ reason: '', votingHours: 24 });
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Could not raise request'); }
    finally { setRaising(false); }
  };

  const castVote = async (urgencyRequestId, vote) => {
    try {
      await urgencyAPI.vote({ urgencyRequestId, vote });
      toast.success(`Voted ${vote}`);
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Could not cast vote'); }
  };

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h2>Urgency Requests — {group?.name}</h2>
          <p className="text-grey text-sm">Members can request early payouts. Majority vote decides.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>+ Raise Request</button>
      </div>

      {/* Raise form */}
      {showForm && (
        <div className="card" style={{ marginBottom: '2rem', borderLeft: '4px solid var(--warning)' }}>
          <h3 style={{ marginBottom: '1rem' }}>Raise Urgency Request</h3>
          <div className="form-group">
            <label className="form-label">Reason (min 10 characters)</label>
            <textarea className="form-input" placeholder="Explain your emergency situation..."
              value={form.reason} onChange={e => setForm(f => ({...f, reason: e.target.value}))} />
          </div>
          <div className="form-group">
            <label className="form-label">Voting Window (hours)</label>
            <select className="form-input" value={form.votingHours} onChange={e => setForm(f => ({...f, votingHours: Number(e.target.value)}))}>
              <option value={12}>12 hours</option>
              <option value={24}>24 hours</option>
              <option value={48}>48 hours</option>
              <option value={72}>72 hours</option>
            </select>
          </div>
          <div className="flex gap-1">
            <button className="btn btn-warning" onClick={raiseRequest} disabled={raising}>
              {raising ? 'Raising...' : 'Submit Request'}
            </button>
            <button className="btn btn-ghost" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}

      {/* Requests list */}
      {requests.length === 0 ? (
        <div className="empty"><div className="empty-icon">🤝</div><h3>No urgency requests</h3></div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {requests.map(r => {
            const majority = Math.floor((r.totalMembers - 1) / 2) + 1;
            const forPct   = r.totalMembers > 1 ? (r.votesFor / (r.totalMembers - 1)) * 100 : 0;
            const isOwn    = r.requestedByName === user?.name;

            return (
              <div key={r.id} className="card">
                <div className="flex-between" style={{ marginBottom: '0.75rem' }}>
                  <div>
                    <h4>{r.requestedByName} {isOwn && <span className="badge badge-purple">You</span>}</h4>
                    <p className="text-grey text-sm">{r.reason}</p>
                  </div>
                  <span className={`badge badge-${r.status==='APPROVED'?'success':r.status==='REJECTED'?'danger':r.status==='EXPIRED'?'grey':'warning'}`}>
                    {r.status}
                  </span>
                </div>

                <div style={{ marginBottom: '0.75rem' }}>
                  <div className="flex-between text-sm text-grey">
                    <span>For: {r.votesFor} | Against: {r.votesAgainst} | Need: {majority}</span>
                    <span>Deadline: {new Date(r.votingDeadline).toLocaleDateString()}</span>
                  </div>
                  <div className="vote-bar"><div className="vote-bar-fill" style={{ width: `${forPct}%` }}/></div>
                </div>

                {r.status === 'PENDING' && !isOwn && (
                  <div className="flex gap-1">
                    <button className="btn btn-success btn-sm" onClick={() => castVote(r.id, 'FOR')}>👍 Vote For</button>
                    <button className="btn btn-danger  btn-sm" onClick={() => castVote(r.id, 'AGAINST')}>👎 Vote Against</button>
                    <button className="btn btn-ghost   btn-sm" onClick={() => castVote(r.id, 'ABSTAIN')}>Abstain</button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Layout>
  );
}
