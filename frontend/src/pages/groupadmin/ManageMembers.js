// src/pages/groupadmin/ManageMembers.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function ManageMembers() {
  const { id } = useParams();
  const [members, setMembers] = useState([]);
  const [pending, setPending] = useState([]);
  const [group,   setGroup]   = useState(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    Promise.all([groupAPI.getGroupById(id), groupAPI.getGroupMembers(id), groupAPI.getPendingMembers(id)])
      .then(([g, m, p]) => { setGroup(g.data.data); setMembers(m.data.data||[]); setPending(p.data.data||[]); })
      .finally(() => setLoading(false));
  };
  useEffect(load, [id]);

  const handleAction = async (memberId, action, reason='') => {
    try {
      await groupAPI.memberAction(id, { memberId, action, reason });
      toast.success(`Member ${action.toLowerCase()}d`);
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Action failed'); }
  };

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <h2 style={{ marginBottom: '1.5rem' }}>Manage Members — {group?.name}</h2>

      {pending.length > 0 && (
        <div className="card" style={{ marginBottom: '2rem', borderLeft: '4px solid var(--warning)' }}>
          <h3 style={{ marginBottom: '1rem' }}>⏳ Pending Join Requests ({pending.length})</h3>
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Name</th><th>Phone</th><th>Requested</th><th>Actions</th></tr></thead>
              <tbody>
                {pending.map(m => (
                  <tr key={m.id}>
                    <td><strong>{m.name}</strong></td>
                    <td>{m.phone}</td>
                    <td className="text-grey text-sm">{new Date(m.joinRequestedAt).toLocaleDateString()}</td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn btn-success btn-sm" onClick={()=>handleAction(m.id,'APPROVE')}>Approve</button>
                        <button className="btn btn-danger  btn-sm" onClick={()=>handleAction(m.id,'REJECT','Not eligible')}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Active Members ({members.filter(m=>m.status==='ACTIVE').length})</h3>
        <div className="table-wrapper">
          <table>
            <thead><tr><th>Name</th><th>Phone</th><th>Status</th>{group?.payoutMethod==='FIXED_ROTATION'&&<th>Order</th>}<th>Payout</th></tr></thead>
            <tbody>
              {members.filter(m=>m.status==='ACTIVE').map(m => (
                <tr key={m.id}>
                  <td><strong>{m.name}</strong></td>
                  <td>{m.phone}</td>
                  <td><span className="badge badge-success">ACTIVE</span></td>
                  {group?.payoutMethod==='FIXED_ROTATION' && <td>#{m.rotationOrder||'—'}</td>}
                  <td>{m.hasReceivedPayout ? <span className="badge badge-info">Cycle {m.payoutReceivedOnCycle}</span> : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
