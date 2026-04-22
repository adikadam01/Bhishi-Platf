// src/pages/groupadmin/CreateGroup.js
import React, { useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

export default function CreateGroup() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    name:'', description:'', totalAmount:'', maxMembers:'',
    payoutMethod:'FIXED_ROTATION', dueDayOfMonth:'5', penaltyAmount:'50'
  });
  const set = (k,v) => setForm(f=>({...f,[k]:v}));

  const contribution = form.totalAmount && form.maxMembers
    ? (Number(form.totalAmount) / Number(form.maxMembers)).toFixed(2) : 0;

  const submit = async () => {
    if (!form.name || !form.totalAmount || !form.maxMembers)
      return toast.error('Fill all required fields');
    setLoading(true);
    try {
      await groupAPI.createGroup({
        name: form.name, description: form.description,
        totalAmount: Number(form.totalAmount), maxMembers: Number(form.maxMembers),
        payoutMethod: form.payoutMethod,
        dueDayOfMonth: Number(form.dueDayOfMonth),
        penaltyAmount: Number(form.penaltyAmount)
      });
      toast.success('Group created! Awaiting Super Admin approval.');
      navigate('/admin/dashboard');
    } catch (e) { toast.error(e.response?.data?.message || 'Failed to create group'); }
    finally { setLoading(false); }
  };

  return (
    <Layout>
      <div style={{ maxWidth: 600 }}>
        <h2 style={{ marginBottom: '0.5rem' }}>Create New Group</h2>
        <p className="text-grey" style={{ marginBottom: '1.5rem' }}>
          Group will be sent to Super Admin for approval before members can join.
        </p>

        <div className="card">
          <div className="form-group"><label className="form-label">Group Name *</label>
            <input className="form-input" placeholder="e.g. Pune Ladies Bhishi" value={form.name} onChange={e=>set('name',e.target.value)}/></div>
          <div className="form-group"><label className="form-label">Description</label>
            <textarea className="form-input" placeholder="Optional description" value={form.description} onChange={e=>set('description',e.target.value)}/></div>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Total Monthly Amount (₹) *</label>
              <input className="form-input" type="number" placeholder="10000" value={form.totalAmount} onChange={e=>set('totalAmount',e.target.value)}/></div>
            <div className="form-group"><label className="form-label">Number of Members *</label>
              <input className="form-input" type="number" min="2" max="50" placeholder="10" value={form.maxMembers} onChange={e=>set('maxMembers',e.target.value)}/></div>
          </div>

          {contribution > 0 && (
            <div className="card-sm" style={{ background:'var(--primary-light)', marginBottom:'1rem' }}>
              <p className="text-sm"><strong>Contribution per member:</strong> ₹{contribution}/month</p>
            </div>
          )}

          <div className="form-group"><label className="form-label">Payout Method *</label>
            <select className="form-input" value={form.payoutMethod} onChange={e=>set('payoutMethod',e.target.value)}>
              <option value="FIXED_ROTATION">Fixed Rotation — Predefined order</option>
              <option value="BIDDING">Bidding — Lowest bid wins</option>
              <option value="CONTROLLED_RANDOM">Controlled Random — Fair draw</option>
            </select>
            <p className="form-hint">This cannot be changed after group is created</p>
          </div>

          <div className="grid-2">
            <div className="form-group"><label className="form-label">Due Day of Month</label>
              <input className="form-input" type="number" min="1" max="28" value={form.dueDayOfMonth} onChange={e=>set('dueDayOfMonth',e.target.value)}/></div>
            <div className="form-group"><label className="form-label">Late Penalty (₹)</label>
              <input className="form-input" type="number" min="0" value={form.penaltyAmount} onChange={e=>set('penaltyAmount',e.target.value)}/></div>
          </div>

          <button className="btn btn-primary" onClick={submit} disabled={loading}>
            {loading ? 'Creating...' : 'Create Group'}
          </button>
        </div>
      </div>
    </Layout>
  );
}
