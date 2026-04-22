// src/pages/LandingPage.js
import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LandingPage() {
  const { user } = useAuth();

  const dashboardLink =
    user?.role === 'SUPER_ADMIN' ? '/super/dashboard' :
    user?.role === 'GROUP_ADMIN' ? '/admin/dashboard' :
                                   '/member/dashboard';

  return (
    <div style={{ fontFamily: "'Segoe UI', system-ui, sans-serif", background: '#F8FAFC', minHeight: '100vh' }}>

      {/* ── Navbar ─────────────────────────────────── */}
      <nav style={{
        background: '#fff', borderBottom: '1px solid #E2E8F0',
        padding: '0 2rem', height: 64,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        position: 'sticky', top: 0, zIndex: 100,
        boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
      }}>
        <div style={{ fontWeight: 700, fontSize: '1.25rem', color: '#6C63FF' }}> Bhishi Platform</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <a href="#how-it-works" style={{ padding: '0.4rem 0.85rem', color: '#64748B', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>How it Works</a>
          <a href="#features"     style={{ padding: '0.4rem 0.85rem', color: '#64748B', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>Features</a>
          <a href="#roles"        style={{ padding: '0.4rem 0.85rem', color: '#64748B', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>Roles</a>
          {user ? (
            <Link to={dashboardLink} style={{
              padding: '0.45rem 1.1rem', background: '#6C63FF', color: '#fff',
              borderRadius: 8, textDecoration: 'none', fontSize: '0.9rem', fontWeight: 600,
            }}>
              Dashboard →
            </Link>
          ) : (
            <>
              <Link to="/login" style={{
                padding: '0.45rem 1rem', color: '#6C63FF', border: '1.5px solid #6C63FF',
                borderRadius: 8, textDecoration: 'none', fontSize: '0.9rem', fontWeight: 600,
              }}>Login</Link>
              <Link to="/register" style={{
                padding: '0.45rem 1.1rem', background: '#6C63FF', color: '#fff',
                borderRadius: 8, textDecoration: 'none', fontSize: '0.9rem', fontWeight: 600,
              }}>Register</Link>
            </>
          )}
        </div>
      </nav>

      {/* ── Hero ───────────────────────────────────── */}
      <section style={{
        background: 'linear-gradient(135deg, #6C63FF 0%, #4B44CC 100%)',
        color: '#fff', padding: '5rem 2rem', textAlign: 'center',
      }}>
        <div style={{ maxWidth: 720, margin: '0 auto' }}>
          <div style={{ fontSize: '3.5rem', marginBottom: '1rem' }}></div>
          <h1 style={{ fontSize: '3rem', fontWeight: 800, marginBottom: '1rem', lineHeight: 1.2 }}>
            Digital Bhishi Platform
          </h1>
          <p style={{ fontSize: '1.2rem', opacity: 0.9, marginBottom: '0.75rem', lineHeight: 1.7 }}>
            A modern, transparent group savings system. Pool money with trusted people,
            receive payouts through fair algorithms, and manage everything online.
          </p>
          <p style={{ fontSize: '0.95rem', opacity: 0.75, marginBottom: '2.5rem' }}>
            The traditional Chit Fund — now digital, auditable, and secure.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            {user ? (
              <Link to={dashboardLink} style={{
                padding: '0.9rem 2rem', background: '#fff', color: '#6C63FF',
                borderRadius: 10, textDecoration: 'none', fontWeight: 700, fontSize: '1rem',
              }}>
                Go to Dashboard →
              </Link>
            ) : (
              <>
                <Link to="/register" style={{
                  padding: '0.9rem 2rem', background: '#fff', color: '#6C63FF',
                  borderRadius: 10, textDecoration: 'none', fontWeight: 700, fontSize: '1rem',
                }}>
                  Get Started Free
                </Link>
                <Link to="/login" style={{
                  padding: '0.9rem 2rem', background: 'transparent',
                  border: '2px solid rgba(255,255,255,0.6)', color: '#fff',
                  borderRadius: 10, textDecoration: 'none', fontWeight: 600, fontSize: '1rem',
                }}>
                  Login
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* ── Stats Bar ──────────────────────────────── */}
      <section style={{ background: '#fff', borderBottom: '1px solid #E2E8F0', padding: '2rem' }}>
        <div style={{ maxWidth: 900, margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', textAlign: 'center' }}>
          {[
            { value: '3',        label: 'Payout Algorithms' },
            { value: '100%',     label: 'Transparent & Audited' },
            { value: '₹0',       label: 'Platform Fee (Simulation)' },
            { value: 'Instant',  label: 'Group Code Generation' },
          ].map((s, i) => (
            <div key={i}>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#6C63FF' }}>{s.value}</div>
              <div style={{ fontSize: '0.85rem', color: '#64748B', marginTop: '0.2rem' }}>{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── How it Works ───────────────────────────── */}
      <section id="how-it-works" style={{ padding: '5rem 2rem', background: '#F8FAFC' }}>
        <div style={{ maxWidth: 900, margin: '0 auto' }}>
          <h2 style={{ textAlign: 'center', fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem' }}>How It Works</h2>
          <p style={{ textAlign: 'center', color: '#64748B', marginBottom: '3rem' }}>Simple 4-step process from registration to payout</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
            {[
              { step: '01', icon: '📝', title: 'Register',        desc: 'Sign up as a Member or Group Admin. Verify your identity with OTP and Aadhaar.' },
              { step: '02', icon: '🏛️', title: 'Create / Join',   desc: 'Group Admin creates a group. Super Admin approves it and generates a unique join code.' },
              { step: '03', icon: '💳', title: 'Pay Monthly',     desc: 'Every member pays their fixed contribution each month. Late payments attract a penalty.' },
              { step: '04', icon: '🏆', title: 'Receive Payout',  desc: 'One member receives the full pot each cycle via Fixed Rotation, Bidding, or Random draw.' },
            ].map((item, i) => (
              <div key={i} style={{
                background: '#fff', borderRadius: 12, padding: '1.75rem',
                boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
                borderTop: '4px solid #6C63FF',
              }}>
                <div style={{ fontSize: '2rem', marginBottom: '0.75rem' }}>{item.icon}</div>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#6C63FF', letterSpacing: '0.1em', marginBottom: '0.4rem' }}>STEP {item.step}</div>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.5rem' }}>{item.title}</h3>
                <p style={{ fontSize: '0.88rem', color: '#64748B', lineHeight: 1.6 }}>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ───────────────────────────────── */}
      <section id="features" style={{ padding: '5rem 2rem', background: '#fff' }}>
        <div style={{ maxWidth: 900, margin: '0 auto' }}>
          <h2 style={{ textAlign: 'center', fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem' }}>Platform Features</h2>
          <p style={{ textAlign: 'center', color: '#64748B', marginBottom: '3rem' }}>Everything you need to run a trusted group savings circle</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
            {[
              { icon: '🔄', title: 'Fixed Rotation Payout',   desc: 'Members are paid out in a pre-set order assigned at the time they join the group.' },
              { icon: '🏷️', title: 'Bidding Payout',          desc: 'Members bid the minimum they accept. Lowest bidder wins; the discount is split among others.' },
              { icon: '🎲', title: 'Controlled Random',        desc: 'Winner drawn randomly from eligible members each cycle. Full audit trail with stored seed.' },
              { icon: '⚡', title: 'Urgency Requests',         desc: 'Members in financial need can raise an urgency request. Others vote to grant early payout.' },
              { icon: '🔑', title: 'Unique Group Codes',       desc: 'Each approved group gets a 6-character code. Members use it to request to join.' },
              { icon: '📊', title: 'Full Audit Logs',          desc: 'Every action — payment, approval, payout — is logged for complete transparency.' },
              { icon: '⏰', title: 'Auto Penalties',           desc: 'Scheduler automatically applies late payment penalties on the due date each month.' },
              { icon: '📧', title: 'Email Notifications',      desc: 'Members get notified for payout wins, approvals, penalties and due date reminders.' },
            ].map((f, i) => (
              <div key={i} style={{
                display: 'flex', gap: '1rem', padding: '1.25rem',
                background: '#F8FAFC', borderRadius: 10,
                border: '1px solid #E2E8F0',
              }}>
                <div style={{ fontSize: '1.75rem', flexShrink: 0 }}>{f.icon}</div>
                <div>
                  <h4 style={{ fontWeight: 600, marginBottom: '0.3rem', fontSize: '0.95rem' }}>{f.title}</h4>
                  <p style={{ fontSize: '0.83rem', color: '#64748B', lineHeight: 1.6 }}>{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Roles ──────────────────────────────────── */}
      <section id="roles" style={{ padding: '5rem 2rem', background: '#F8FAFC' }}>
        <div style={{ maxWidth: 900, margin: '0 auto' }}>
          <h2 style={{ textAlign: 'center', fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem' }}>Three Roles, One Platform</h2>
          <p style={{ textAlign: 'center', color: '#64748B', marginBottom: '3rem' }}>Each role has a clear purpose and dedicated dashboard</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
            {[
              {
                icon: '👤', title: 'Member', color: '#3B82F6', bg: '#EFF6FF',
                points: ['Register and verify via OTP', 'Join groups using a unique code', 'Pay monthly contributions', 'Place bids or receive payouts', 'Raise urgency requests'],
              },
              {
                icon: '🏛️', title: 'Group Admin', color: '#F59E0B', bg: '#FFFBEB',
                points: ['Create and configure Bhishi groups', 'Get approved by Super Admin', 'Share unique group code with members', 'Approve or reject join requests', 'Initiate monthly payout cycles'],
              },
              {
                icon: '🛡️', title: 'Super Admin', color: '#EF4444', bg: '#FEF2F2',
                points: ['Review new group applications', 'Approve or reject groups', 'Trigger automated scheduler jobs', 'Platform-wide oversight', 'Inserted directly via MongoDB'],
              },
            ].map((role, i) => (
              <div key={i} style={{
                background: '#fff', borderRadius: 12, padding: '1.75rem',
                boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
                border: `2px solid ${role.bg}`,
              }}>
                <div style={{
                  width: 52, height: 52, borderRadius: 12,
                  background: role.bg, display: 'flex', alignItems: 'center',
                  justifyContent: 'center', fontSize: '1.6rem', marginBottom: '1rem',
                }}>
                  {role.icon}
                </div>
                <h3 style={{ fontWeight: 700, marginBottom: '1rem', color: role.color }}>{role.title}</h3>
                <ul style={{ listStyle: 'none', padding: 0 }}>
                  {role.points.map((p, j) => (
                    <li key={j} style={{ fontSize: '0.86rem', color: '#475569', marginBottom: '0.5rem', display: 'flex', gap: '0.5rem' }}>
                      <span style={{ color: role.color, fontWeight: 700 }}>✓</span> {p}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ────────────────────────────────────── */}
      <section style={{
        background: 'linear-gradient(135deg, #6C63FF 0%, #4B44CC 100%)',
        padding: '4rem 2rem', textAlign: 'center', color: '#fff',
      }}>
        <h2 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.75rem' }}>
          Ready to start your Bhishi group?
        </h2>
        <p style={{ opacity: 0.85, marginBottom: '2rem', fontSize: '1rem' }}>
          Register in under 2 minutes. No fees. Full transparency.
        </p>
        {user ? (
          <Link to={dashboardLink} style={{
            padding: '0.9rem 2.5rem', background: '#fff', color: '#6C63FF',
            borderRadius: 10, textDecoration: 'none', fontWeight: 700, fontSize: '1rem',
          }}>
            Go to your Dashboard →
          </Link>
        ) : (
          <Link to="/register" style={{
            padding: '0.9rem 2.5rem', background: '#fff', color: '#6C63FF',
            borderRadius: 10, textDecoration: 'none', fontWeight: 700, fontSize: '1rem',
          }}>
            Create your Account →
          </Link>
        )}
      </section>

      {/* ── Footer ─────────────────────────────────── */}
      <footer style={{ background: '#1E1E2E', color: '#94A3B8', padding: '2rem', textAlign: 'center', fontSize: '0.85rem' }}>
        <div style={{ marginBottom: '0.5rem', color: '#fff', fontWeight: 600 }}>🪙 Bhishi Platform</div>
        Digital group savings simulation. Not a regulated financial service.
      </footer>

    </div>
  );
}