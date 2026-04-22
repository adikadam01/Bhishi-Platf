// src/pages/member/BiddingPage.js
// ============================================================
// DIGITAL BHISHI PLATFORM — Member Bidding Page
// Allows members to place bids when group payout method is BIDDING
// and a cycle is currently IN_PROGRESS.
// ============================================================
import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { payoutAPI, groupAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';

export default function BiddingPage() {
  const { id } = useParams();           // groupId
  const { user } = useAuth();

  const [group,   setGroup]   = useState(null);
  const [cycle,   setCycle]   = useState(null);
  const [bidding, setBidding] = useState(null);
  const [loading, setLoading] = useState(true);
  const [bidAmt,  setBidAmt]  = useState('');
  const [placing, setPlacing] = useState(false);

  // ── Load group + current cycle + bidding status ──────────
  const load = async () => {
    try {
      const [g, c] = await Promise.all([
        groupAPI.getGroupById(id),
        payoutAPI.getCurrentCycle(id),
      ]);
      setGroup(g.data.data);
      const cycleData = c.data.data;
      setCycle(cycleData);

      // Only fetch bidding status if cycle exists and is BIDDING method
      if (cycleData && cycleData.payoutMethod === 'BIDDING' && cycleData.status === 'IN_PROGRESS') {
        const b = await payoutAPI.getBiddingStatus(cycleData.id);
        setBidding(b.data.data);
      }
    } catch {
      // No active cycle is fine — we'll show a message
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [id]);

  // ── Place bid ─────────────────────────────────────────────
  const placeBid = async () => {
    const amount = Number(bidAmt);
    if (!amount || amount <= 0) return toast.error('Enter a valid bid amount');
    if (amount >= cycle.totalCollected) return toast.error(`Bid must be less than the full pot (₹${cycle.totalCollected?.toLocaleString()})`);
    if (amount < cycle.totalCollected * 0.5) return toast.error(`Bid must be at least 50% of the pot (₹${(cycle.totalCollected * 0.5)?.toLocaleString()})`);

    setPlacing(true);
    try {
      await payoutAPI.placeBid({ cycleId: cycle.id, groupId: id, bidAmount: amount });
      toast.success('🎉 Bid placed successfully! You will be notified if you win.');
      setBidAmt('');
      load(); // refresh status
    } catch (e) {
      toast.error(e.response?.data?.message || 'Could not place bid');
    } finally {
      setPlacing(false);
    }
  };

  // ── Helpers ──────────────────────────────────────────────
  const myBid   = bidding?.bids?.find(b => b.memberName === user?.name || b.memberId === user?.id);
  const minBid  = cycle ? Math.ceil(cycle.totalCollected * 0.5) : 0;
  const maxBid  = cycle ? cycle.totalCollected - 1 : 0;
  const discount = myBid ? (cycle?.totalCollected - myBid.bidAmount) : 0;
  const membersCount = group?.currentMemberCount || 1;
  const perMemberBack = discount > 0 ? (discount / (membersCount - 1)).toFixed(2) : 0;

  // ── Render ────────────────────────────────────────────────
  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      {/* Header */}
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h2>💰 Bidding — {group?.name}</h2>
          <p className="text-grey text-sm">
            Place your bid to win this cycle's payout. Lowest bidder wins!
          </p>
        </div>
        <Link to={`/member/groups/${id}`} className="btn btn-outline">← Back to Group</Link>
      </div>

      {/* No active BIDDING cycle */}
      {(!cycle || cycle.payoutMethod !== 'BIDDING' || cycle.status !== 'IN_PROGRESS') && (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🔒</div>
          <h3 style={{ marginBottom: '0.5rem' }}>No Active Bidding Round</h3>
          <p className="text-grey">
            {!cycle
              ? 'The group admin has not initiated a payout cycle yet.'
              : cycle.payoutMethod !== 'BIDDING'
              ? `This group uses ${cycle.payoutMethod?.replace(/_/g, ' ')} payout method, not bidding.`
              : 'The current cycle is already completed.'}
          </p>
          <Link to={`/member/groups/${id}`} className="btn btn-primary" style={{ marginTop: '1.5rem' }}>
            Return to Group
          </Link>
        </div>
      )}

      {/* Active bidding cycle */}
      {cycle && cycle.payoutMethod === 'BIDDING' && cycle.status === 'IN_PROGRESS' && (
        <>
          {/* Cycle stats */}
          <div className="grid-4" style={{ marginBottom: '2rem' }}>
            <div className="stat-card">
              <div className="stat-label">Total Pot</div>
              <div className="stat-value">₹{cycle.totalCollected?.toLocaleString()}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Cycle</div>
              <div className="stat-value">#{cycle.cycleNumber}</div>
              <div className="stat-sub">{cycle.cycleMonth}/{cycle.cycleYear}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total Bids</div>
              <div className="stat-value">{bidding?.totalBids ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Bid Range</div>
              <div className="stat-value" style={{ fontSize: '1rem' }}>
                ₹{minBid?.toLocaleString()} – ₹{maxBid?.toLocaleString()}
              </div>
            </div>
          </div>

          <div className="grid-2" style={{ gap: '1.5rem' }}>
            {/* Place bid / Your bid */}
            <div>
              {!myBid ? (
                /* ── Place new bid ── */
                <div className="card" style={{ borderLeft: '4px solid var(--primary)' }}>
                  <h3 style={{ marginBottom: '0.5rem' }}>Place Your Bid</h3>
                  <p className="text-grey text-sm" style={{ marginBottom: '1.25rem' }}>
                    Bid the <strong>minimum amount</strong> you're willing to accept as payout.
                    The member who bids the lowest wins. The discount is split equally among all other members.
                  </p>

                  <div className="form-group">
                    <label className="form-label">Your Bid Amount (₹)</label>
                    <input
                      className="form-input"
                      type="number"
                      placeholder={`e.g. ${Math.round(cycle.totalCollected * 0.85)}`}
                      min={minBid}
                      max={maxBid}
                      value={bidAmt}
                      onChange={e => setBidAmt(e.target.value)}
                    />
                    <div className="form-hint">
                      Min: ₹{minBid?.toLocaleString()} (50% of pot) &nbsp;|&nbsp; Max: ₹{(maxBid)?.toLocaleString()} (just under full pot)
                    </div>
                  </div>

                  {/* Live preview */}
                  {bidAmt && Number(bidAmt) > 0 && Number(bidAmt) < cycle.totalCollected && (
                    <div style={{
                      background: 'var(--primary-light)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '0.75rem 1rem',
                      marginBottom: '1rem',
                      fontSize: '0.88rem'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                        <span className="text-grey">If you win, you receive:</span>
                        <strong style={{ color: 'var(--primary)' }}>₹{Number(bidAmt)?.toLocaleString()}</strong>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                        <span className="text-grey">Discount to distribute:</span>
                        <span>₹{(cycle.totalCollected - Number(bidAmt))?.toLocaleString()}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span className="text-grey">Each other member gets back:</span>
                        <strong style={{ color: 'var(--success)' }}>
                          ₹{((cycle.totalCollected - Number(bidAmt)) / (membersCount - 1)).toFixed(2)}
                        </strong>
                      </div>
                    </div>
                  )}

                  <button
                    className="btn btn-primary btn-full"
                    onClick={placeBid}
                    disabled={placing || !bidAmt}
                  >
                    {placing ? 'Placing Bid...' : '🎯 Submit Bid'}
                  </button>
                </div>
              ) : (
                /* ── Already placed bid ── */
                <div className="card" style={{ borderLeft: `4px solid ${myBid.status === 'WON' ? 'var(--success)' : myBid.status === 'LOST' ? 'var(--grey)' : 'var(--warning)'}` }}>
                  <div className="flex-between" style={{ marginBottom: '1rem' }}>
                    <h3>Your Bid</h3>
                    <span className={`badge badge-${myBid.status === 'WON' ? 'success' : myBid.status === 'LOST' ? 'grey' : 'warning'}`}>
                      {myBid.status === 'WON' ? '🏆 Won!' : myBid.status === 'LOST' ? 'Lost' : '⏳ Pending'}
                    </span>
                  </div>

                  <div style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '0.25rem' }}>
                    ₹{myBid.bidAmount?.toLocaleString()}
                  </div>
                  <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>
                    Submitted on {new Date(myBid.placedAt || Date.now()).toLocaleDateString('en-IN', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' })}
                  </p>

                  {myBid.status === 'WON' && (
                    <div style={{ background: 'rgba(34,197,94,0.1)', borderRadius: 'var(--radius-sm)', padding: '0.75rem 1rem' }}>
                      <p style={{ color: 'var(--success)', fontWeight: 600 }}>
                        🎉 Congratulations! You won this cycle's payout of ₹{myBid.bidAmount?.toLocaleString()}.
                      </p>
                    </div>
                  )}

                  {myBid.status === 'PENDING' && (
                    <div style={{ background: 'rgba(245,158,11,0.1)', borderRadius: 'var(--radius-sm)', padding: '0.75rem 1rem' }}>
                      <p style={{ color: 'var(--warning)', fontSize: '0.88rem' }}>
                        ⏳ Your bid is submitted. Wait for the admin to close bidding and announce the winner.
                      </p>
                      <p className="text-grey text-sm" style={{ marginTop: '0.4rem' }}>
                        If you win: You get ₹{myBid.bidAmount?.toLocaleString()} &nbsp;|&nbsp;
                        Discount ₹{(cycle.totalCollected - myBid.bidAmount)?.toLocaleString()} split among others.
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* How bidding works */}
            <div>
              <div className="card" style={{ marginBottom: '1rem' }}>
                <h3 style={{ marginBottom: '0.75rem' }}>📖 How Bidding Works</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {[
                    { step: '1', title: 'Place a Bid', desc: 'Enter the minimum amount you are willing to accept as your payout. It must be between 50% and 99% of the total pot.' },
                    { step: '2', title: 'Admin Closes Bidding', desc: 'When the admin closes the bidding window, all bids are evaluated.' },
                    { step: '3', title: 'Lowest Bid Wins', desc: 'The member who bid the lowest amount wins the payout for this cycle.' },
                    { step: '4', title: 'Discount Distributed', desc: 'The difference between the full pot and the winning bid is split equally among all other members.' },
                  ].map(item => (
                    <div key={item.step} style={{ display: 'flex', gap: '0.75rem' }}>
                      <div style={{
                        width: 28, height: 28, borderRadius: '50%',
                        background: 'var(--primary)', color: '#fff',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: '0.8rem', fontWeight: 700, flexShrink: 0
                      }}>{item.step}</div>
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{item.title}</div>
                        <div className="text-grey text-sm">{item.desc}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Pot example */}
              <div className="card" style={{ background: 'var(--primary-light)' }}>
                <h4 style={{ marginBottom: '0.75rem', color: 'var(--primary)' }}>💡 Example</h4>
                <p className="text-grey text-sm">
                  Pot = ₹{cycle.totalCollected?.toLocaleString()} with {membersCount} members.
                  If you bid ₹{Math.round(cycle.totalCollected * 0.85)?.toLocaleString()} and win,
                  you receive that amount and each of the other {membersCount - 1} members
                  gets back ₹{((cycle.totalCollected - Math.round(cycle.totalCollected * 0.85)) / (membersCount - 1)).toFixed(0)} each.
                </p>
              </div>
            </div>
          </div>

          {/* All bids — only show status counts (member can't see others' amounts) */}
          {bidding && bidding.totalBids > 0 && (
            <div className="card" style={{ marginTop: '1.5rem' }}>
              <h3 style={{ marginBottom: '1rem' }}>Bidding Activity</h3>
              <div className="grid-3">
                <div className="stat-card">
                  <div className="stat-label">Bids Submitted</div>
                  <div className="stat-value">{bidding.totalBids}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Members Eligible</div>
                  <div className="stat-value">{membersCount}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Yet to Bid</div>
                  <div className="stat-value">{membersCount - bidding.totalBids}</div>
                </div>
              </div>
              <p className="text-grey text-sm" style={{ marginTop: '0.75rem' }}>
                Individual bid amounts are kept private until the admin closes bidding and announces the winner.
              </p>
            </div>
          )}
        </>
      )}
    </Layout>
  );
}