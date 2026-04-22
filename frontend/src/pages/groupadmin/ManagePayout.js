// src/pages/groupadmin/ManagePayout.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { payoutAPI, groupAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function ManagePayout() {
  const { id } = useParams();
  const [group,   setGroup]   = useState(null);
  const [history, setHistory] = useState(null);
  const [current, setCurrent] = useState(null);
  const [bidding, setBidding] = useState(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const now = new Date();

  const load = async () => {
    try {
      const [g, h] = await Promise.all([groupAPI.getGroupById(id), payoutAPI.getHistory(id)]);
      setGroup(g.data.data);
      setHistory(h.data.data);
      try {
        const c = await payoutAPI.getCurrentCycle(id);
        setCurrent(c.data.data);
        if (c.data.data?.payoutMethod === 'BIDDING' && c.data.data?.status === 'IN_PROGRESS') {
          const b = await payoutAPI.getBiddingStatus(c.data.data.id);
          setBidding(b.data.data);
        }
      } catch { setCurrent(null); }
    } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [id]);

  const initiatePayout = async () => {
    setWorking(true);
    try {
      await payoutAPI.initiatePayout({
        groupId: id,
        cycleNumber: group.currentCycleMonth,
        cycleMonth: now.getMonth() + 1,
        cycleYear: now.getFullYear()
      });
      toast.success('Payout initiated!');
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Failed to initiate payout'); }
    finally { setWorking(false); }
  };

  const executeBidding = async () => {
    if (!current) return;
    setWorking(true);
    try {
      await payoutAPI.executeBiddingPayout({ cycleId: current.id });
      toast.success('Bidding payout executed!');
      load();
    } catch (e) { toast.error(e.response?.data?.message || 'Failed to execute payout'); }
    finally { setWorking(false); }
  };

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <h2 style={{ marginBottom: '0.5rem' }}>Manage Payout — {group?.name}</h2>
      <p className="text-grey" style={{ marginBottom: '1.5rem' }}>
        Method: <strong>{group?.payoutMethod?.replace(/_/g,' ')}</strong> •
        Current Cycle: <strong>{group?.currentCycleMonth}</strong>
      </p>

      {/* Initiate payout */}
      {!current && (
        <div className="card" style={{ marginBottom: '2rem' }}>
          <h3 style={{ marginBottom: '0.5rem' }}>Initiate Cycle {group?.currentCycleMonth} Payout</h3>
          <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>
            {group?.payoutMethod === 'FIXED_ROTATION'    && 'Winner will be selected by rotation order immediately.'}
            {group?.payoutMethod === 'CONTROLLED_RANDOM' && 'Winner will be drawn randomly with an audit seed immediately.'}
            {group?.payoutMethod === 'BIDDING'           && 'This will open a bidding window. Members can then place bids.'}
          </p>
          <button className="btn btn-primary" onClick={initiatePayout} disabled={working}>
            {working ? 'Initiating...' : 'Initiate Payout'}
          </button>
        </div>
      )}

      {/* Active cycle status */}
      {current && (
        <div className="card" style={{ marginBottom: '2rem', borderLeft: `4px solid var(--${current.status==='COMPLETED'?'success':'primary'})` }}>
          <div className="flex-between" style={{ marginBottom: '0.75rem' }}>
            <h3>Cycle #{current.cycleNumber} — {current.status}</h3>
            <span className={`badge badge-${current.status==='COMPLETED'?'success':'info'}`}>{current.status}</span>
          </div>
          {current.status === 'COMPLETED' ? (
            <>
              <p><strong>Winner:</strong> {current.winnerName}</p>
              <p><strong>Amount:</strong> ₹{current.winnerAmount?.toLocaleString()}</p>
              {current.lowestBid && <p><strong>Winning Bid:</strong> ₹{current.lowestBid} (Discount: ₹{current.discount}, Each member gets back ₹{current.distributedPerMember?.toFixed(2)})</p>}
              {current.selectionSeed && <p className="text-grey text-sm" style={{marginTop:'0.5rem'}}>Audit Seed: {current.selectionSeed}</p>}
            </>
          ) : (
            <>
              {/* BIDDING: show bids and execute button */}
              {group?.payoutMethod === 'BIDDING' && bidding && (
                <>
                  <p style={{ marginBottom: '1rem' }}>
                    <strong>{bidding.totalBids}</strong> bids received • Total pot: ₹{current.totalCollected?.toLocaleString()}
                  </p>
                  {bidding.bids?.length > 0 && (
                    <div className="table-wrapper" style={{ marginBottom: '1rem' }}>
                      <table>
                        <thead><tr><th>Member</th><th>Bid Amount</th><th>Status</th></tr></thead>
                        <tbody>
                          {bidding.bids.map(b => (
                            <tr key={b.id}>
                              <td>{b.memberName}</td>
                              <td><strong>₹{b.bidAmount?.toLocaleString()}</strong></td>
                              <td><span className={`badge badge-${b.status==='WON'?'success':b.status==='LOST'?'grey':'warning'}`}>{b.status}</span></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                  <button className="btn btn-primary" onClick={executeBidding} disabled={working || bidding.totalBids === 0}>
                    {working ? 'Executing...' : bidding.totalBids === 0 ? 'No bids yet' : 'Close Bidding & Execute Payout'}
                  </button>
                </>
              )}
            </>
          )}
        </div>
      )}

      {/* Payout history table */}
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Payout History ({history?.completedCycles}/{history?.totalCycles} done)</h3>
        <div className="table-wrapper">
          <table>
            <thead><tr><th>Cycle</th><th>Month/Year</th><th>Winner</th><th>Amount</th><th>Method</th></tr></thead>
            <tbody>
              {history?.cycles?.filter(c => c.status === 'COMPLETED').map(c => (
                <tr key={c.id}>
                  <td><strong>#{c.cycleNumber}</strong></td>
                  <td>{c.cycleMonth}/{c.cycleYear}</td>
                  <td>{c.winnerName}</td>
                  <td>₹{c.winnerAmount?.toLocaleString()}</td>
                  <td><span className="badge badge-purple">{c.payoutMethod?.replace(/_/g,' ')}</span></td>
                </tr>
              ))}
              {history?.cycles?.filter(c => c.status === 'COMPLETED').length === 0 && (
                <tr><td colSpan={5} style={{textAlign:'center',color:'var(--grey)',padding:'2rem'}}>No completed payouts yet</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
