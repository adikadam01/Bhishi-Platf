// src/pages/superadmin/SchedulerPanel.js
import React, { useState } from 'react';
import Layout from '../../components/shared/Layout';
import { schedulerAPI } from '../../services/api';
import toast from 'react-hot-toast';

const JOBS = [
  { key: 'seedPayments',     label: 'Seed Monthly Payments',    desc: 'Create PENDING payment records for all active members in all active groups for this month.',         fn: schedulerAPI.seedPayments,     icon: '🌱', color: 'var(--success)' },
  { key: 'enforcePenalties', label: 'Enforce Penalties',        desc: 'Mark all overdue PENDING payments as LATE and add penalty amounts.',                                  fn: schedulerAPI.enforcePenalties, icon: '⚠️', color: 'var(--warning)' },
  { key: 'sendReminders',    label: 'Send Due Date Reminders',  desc: 'Email all members who have not paid yet. Normally runs automatically 3 days, 1 day, and on due date.', fn: schedulerAPI.sendReminders,    icon: '📧', color: 'var(--info)'    },
  { key: 'expireUrgency',    label: 'Expire Urgency Requests',  desc: 'Mark PENDING urgency requests as EXPIRED if their voting deadline has passed.',                       fn: schedulerAPI.expireUrgency,    icon: '⏰', color: 'var(--danger)'  },
  { key: 'notifyAdmins',     label: 'Notify Admins for Payout', desc: 'Email all group admins that their cycle is ready for payout.',                                        fn: schedulerAPI.notifyAdmins,     icon: '🔔', color: 'var(--primary)' },
  { key: 'checkCompletion',  label: 'Check Group Completion',   desc: 'Mark groups as COMPLETED when all members have received their payout.',                               fn: schedulerAPI.checkCompletion,  icon: '🏁', color: 'var(--success)' },
];

export default function SchedulerPanel() {
  const [running, setRunning] = useState('');
  const [results, setResults] = useState({});

  const run = async (job) => {
    setRunning(job.key);
    try {
      await job.fn();
      setResults(r => ({ ...r, [job.key]: { ok: true, time: new Date().toLocaleTimeString() } }));
      toast.success(`${job.label} — completed`);
    } catch (e) {
      setResults(r => ({ ...r, [job.key]: { ok: false, time: new Date().toLocaleTimeString() } }));
      toast.error(e.response?.data?.message || `${job.label} failed`);
    } finally { setRunning(''); }
  };

  return (
    <Layout>
      <h2 style={{ marginBottom: '0.5rem' }}>Scheduler Panel</h2>
      <p className="text-grey" style={{ marginBottom: '1.5rem' }}>
        All 6 jobs run automatically on their cron schedule. Use these buttons to trigger manually for testing or recovery.
      </p>

      <div className="grid-2">
        {JOBS.map(job => (
          <div key={job.key} className="card" style={{ borderLeft: `4px solid ${job.color}` }}>
            <div className="flex-between" style={{ marginBottom: '0.5rem' }}>
              <h3 style={{ fontSize: '1rem' }}>{job.icon} {job.label}</h3>
              {results[job.key] && (
                <span className={`badge badge-${results[job.key].ok ? 'success' : 'danger'}`}>
                  {results[job.key].ok ? '✓' : '✗'} {results[job.key].time}
                </span>
              )}
            </div>
            <p className="text-grey text-sm" style={{ marginBottom: '1rem' }}>{job.desc}</p>
            <button
              className="btn btn-outline btn-sm"
              disabled={!!running}
              onClick={() => run(job)}>
              {running === job.key ? '⏳ Running...' : '▶ Run Now'}
            </button>
          </div>
        ))}
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Automatic Schedule</h3>
        <div className="table-wrapper">
          <table>
            <thead><tr><th>Job</th><th>Cron</th><th>When it runs</th></tr></thead>
            <tbody>
              <tr><td>Seed Payments</td>     <td><code>0 1 0 1 * *</code></td>    <td>1st of every month at 00:01</td></tr>
              <tr><td>Enforce Penalties</td> <td><code>0 5 0 * * *</code></td>    <td>Every day at 00:05</td></tr>
              <tr><td>Send Reminders</td>    <td><code>0 0 9 * * *</code></td>    <td>Every day at 09:00</td></tr>
              <tr><td>Expire Urgency</td>    <td><code>0 0 * * * *</code></td>    <td>Every hour</td></tr>
              <tr><td>Notify Admins</td>     <td><code>0 0 10 10 * *</code></td>  <td>10th of every month at 10:00</td></tr>
              <tr><td>Check Completion</td>  <td><code>0 0 23 L * *</code></td>   <td>Last day of month at 23:00</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}
