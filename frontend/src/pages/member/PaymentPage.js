// src/pages/member/PaymentPage.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../../components/shared/Layout';
import { paymentAPI, groupAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function PaymentPage() {
  const { id } = useParams();
  const [group,   setGroup]   = useState(null);
  const [history, setHistory] = useState(null);
  const [order,   setOrder]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [paying,  setPaying]  = useState(false);
  const now = new Date();

  useEffect(() => {
    Promise.all([groupAPI.getGroupById(id), paymentAPI.getHistory(id)])
      .then(([g, h]) => { setGroup(g.data.data); setHistory(h.data.data); })
      .finally(() => setLoading(false));
  }, [id]);

  const createOrder = async () => {
    setPaying(true);
    try {
      const { data } = await paymentAPI.createOrder({
        groupId: id, cycleMonth: now.getMonth() + 1, cycleYear: now.getFullYear()
      });
      setOrder(data.data);
      toast.success('[TEST MODE] Order created! Use test card to pay.');
    } catch (e) { toast.error(e.response?.data?.message || 'Could not create order'); }
    finally { setPaying(false); }
  };

  const simulatePay = async () => {
    if (!order) return;
    setPaying(true);
    try {
      await paymentAPI.verifyPayment({
        razorpayOrderId: order.razorpayOrderId,
        razorpayPaymentId: 'pay_TEST_' + Date.now(),
        razorpaySignature: 'test_signature'
      });
      toast.success('Payment confirmed!');
      setOrder(null);
      const h = await paymentAPI.getHistory(id);
      setHistory(h.data.data);
    } catch (e) { toast.error(e.response?.data?.message || 'Payment failed'); }
    finally { setPaying(false); }
  };

  if (loading) return <Layout><div className="spinner-wrap"><div className="spinner"/></div></Layout>;

  return (
    <Layout>
      <h2 style={{ marginBottom: '1.5rem' }}>Payments — {group?.name}</h2>

      {/* Pay now card */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginBottom: '0.5rem' }}>This Month's Contribution</h3>
        <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>
          Due on {group?.dueDayOfMonth}th of every month • Penalty: ₹{group?.penaltyAmount} if late
        </p>

        {!order ? (
          <div className="flex gap-2" style={{ alignItems: 'center' }}>
            <div>
              <div style={{ fontSize: '1.8rem', fontWeight: 700, color: 'var(--primary)' }}>
                ₹{group?.contributionPerMember?.toLocaleString()}
              </div>
              <div className="text-grey text-sm">Base contribution</div>
            </div>
            <button className="btn btn-primary" onClick={createOrder} disabled={paying}>
              {paying ? 'Creating order...' : 'Pay Now'}
            </button>
          </div>
        ) : (
          <div className="card-sm" style={{ background: 'var(--primary-light)' }}>
            <h4 style={{ marginBottom: '0.5rem' }}>Order Created ✓</h4>
            <p className="text-sm">Amount: ₹{order.amount} {order.isLate && <span className="badge badge-warning">Late + ₹{order.penaltyAmount} penalty</span>}</p>
            <p className="text-sm text-grey" style={{ marginBottom: '1rem' }}>Order ID: {order.razorpayOrderId}</p>
            <div className="flex gap-1">
              <button className="btn btn-success" onClick={simulatePay} disabled={paying}>
                {paying ? 'Processing...' : '[TEST] Simulate Payment'}
              </button>
              <button className="btn btn-ghost" onClick={() => setOrder(null)}>Cancel</button>
            </div>
          </div>
        )}
      </div>

      {/* Payment history */}
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Payment History</h3>
        {history?.payments?.length === 0 ? (
          <div className="empty"><p>No payments yet</p></div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Cycle</th><th>Amount</th><th>Status</th><th>Paid On</th></tr></thead>
              <tbody>
                {history?.payments?.map(p => (
                  <tr key={p.id}>
                    <td>{p.cycleMonth}/{p.cycleYear}</td>
                    <td>₹{p.totalAmount} {p.penaltyAmount > 0 && <span className="badge badge-warning text-sm">+₹{p.penaltyAmount} penalty</span>}</td>
                    <td><span className={`badge badge-${p.status==='PAID'?'success':p.status==='LATE'?'danger':'warning'}`}>{p.status}</span></td>
                    <td className="text-grey text-sm">{p.paidAt ? new Date(p.paidAt).toLocaleDateString() : '—'}</td>
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
