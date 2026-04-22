// src/pages/member/MyGroups.js
import React, { useEffect, useState } from 'react';
import Layout from '../../components/shared/Layout';
import { groupAPI } from '../../services/api';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

export default function MyGroups() {
  const [groups,  setGroups]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [code,    setCode]    = useState('');
  const [joining, setJoining] = useState(false);

  useEffect(() => {
    groupAPI.getJoinedGroups()
      .then(r => setGroups(r.data.data || []))
      .finally(() => setLoading(false));
  }, []);

  const joinGroup = async () => {
    if (!code || code.length !== 6) return toast.error('Enter a valid 6-character group code');
    setJoining(true);
    try {
      await groupAPI.joinGroup({ groupCode: code.toUpperCase() });
      toast.success('Join request sent! Waiting for admin approval.');
      setCode('');
    } catch (e) { toast.error(e.response?.data?.message || 'Could not join group'); }
    finally { setJoining(false); }
  };

  return (
    <Layout>
      <h2 style={{ marginBottom: '1.5rem' }}>My Groups</h2>

      {/* Join group card */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Join a Group</h3>
        <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>
          Enter the 6-character code given by your group admin
        </p>
        <div className="flex gap-2">
          <input className="form-input" style={{ maxWidth: 220 }} placeholder="e.g. PLB2K9"
            maxLength={6} value={code} onChange={e => setCode(e.target.value.toUpperCase())} />
          <button className="btn btn-primary" onClick={joinGroup} disabled={joining}>
            {joining ? 'Joining...' : 'Send Join Request'}
          </button>
        </div>
      </div>

      {loading ? <div className="spinner-wrap"><div className="spinner"/></div> :
       groups.length === 0 ? (
        <div className="empty"><div className="empty-icon">🏦</div><h3>No groups joined yet</h3></div>
       ) : (
        <div className="grid-3">
          {groups.map(g => (
            <Link key={g.id} to={`/member/groups/${g.id}`} style={{ textDecoration: 'none' }}>
              <div className="card-sm" style={{ cursor: 'pointer' }}>
                <div className="flex-between" style={{ marginBottom: '0.5rem' }}>
                  <h4>{g.name}</h4>
                  <span className={`badge badge-${g.status === 'ACTIVE' ? 'success' : 'warning'}`}>{g.status}</span>
                </div>
                <p className="text-grey text-sm">₹{g.contributionPerMember}/month</p>
                <p className="text-grey text-sm">{g.currentMemberCount}/{g.maxMembers} members</p>
                <p className="text-grey text-sm" style={{ marginTop: '0.5rem' }}>
                  Code: <strong>{g.groupCode}</strong>
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </Layout>
  );
}
