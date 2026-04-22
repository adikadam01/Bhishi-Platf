// src/pages/auth/RegisterPage.js
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function RegisterPage() {
  const { login }   = useAuth();
  const navigate    = useNavigate();
  const [step,      setStep]    = useState(0);   // 0 = role select, 1-3 = form steps
  const [loading,   setLoading] = useState(false);
  const [userId,    setUserId]  = useState('');
  const [form,      setForm]    = useState({
    role: '',
    name: '', email: '', phone: '', dob: '',
    organizationName: '', businessPhone: '',
    aadhaarLastFour: '',
    password: '', termsAccepted: false,
  });

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const selectRole = (role) => {
    set('role', role);
    setStep(1);
  };

  const submitStep1 = async () => {
    if (!form.name || !form.email || !form.phone || !form.dob)
      return toast.error('Fill all required fields');
    if (form.role === 'GROUP_ADMIN' && !form.organizationName)
      return toast.error('Organization name is required for Group Admin');
    setLoading(true);
    try {
      const payload = { name: form.name, email: form.email, phone: form.phone, dob: form.dob, role: form.role };
      if (form.role === 'GROUP_ADMIN') {
        payload.organizationName = form.organizationName;
        payload.businessPhone    = form.businessPhone || form.phone;
      }
      const { data } = await authAPI.registerStep1(payload);
      setUserId(data.data.userId);
      toast.success('Basic details saved!');
      setStep(2);
    } catch (e) {
      toast.error(e.response?.data?.message || 'Registration failed');
    } finally { setLoading(false); }
  };

  const submitStep2 = async () => {
    if (!form.aadhaarLastFour) return toast.error('Enter last 4 digits of Aadhaar');
    setLoading(true);
    try {
      await authAPI.registerStep2({ userId, aadhaarLastFour: form.aadhaarLastFour });
      toast.success('Identity saved!');
      setStep(3);
    } catch (e) {
      toast.error(e.response?.data?.message || 'Step 2 failed');
    } finally { setLoading(false); }
  };

  const submitStep3 = async () => {
    if (!form.password || form.password.length < 8) return toast.error('Password must be at least 8 characters');
    if (!form.termsAccepted) return toast.error('Accept terms to continue');
    setLoading(true);
    try {
      const { data } = await authAPI.registerStep3({ userId, password: form.password, termsAccepted: true });
      login(data.data, data.data.token);
      const msg = form.role === 'GROUP_ADMIN'
        ? 'Registration complete! Your Group Admin account is pending Super Admin approval.'
        : 'Registration complete! Welcome to Bhishi.';
      toast.success(msg);
      navigate('/');
    } catch (e) {
      toast.error(e.response?.data?.message || 'Step 3 failed');
    } finally { setLoading(false); }
  };

  const stepLabels = ['Basic Details', 'Identity', 'Confirm'];

  // ── Step 0: Role Selection ──────────────────────────────
  if (step === 0) {
    return (
      <div className="auth-page">
        <div className="auth-card" style={{ maxWidth: 520 }}>
          <div className="auth-logo">
            <h1>🪙 Bhishi</h1>
            <p>Create your account</p>
          </div>
          <p style={{ textAlign: 'center', marginBottom: '1.5rem', color: 'var(--grey)', fontSize: '0.9rem' }}>
            How will you use Bhishi?
          </p>

          {/* Member card */}
          <button
            onClick={() => selectRole('MEMBER')}
            style={{
              width: '100%', padding: '1.2rem 1.5rem', marginBottom: '1rem',
              border: '2px solid var(--border)', borderRadius: '12px',
              background: 'var(--card-bg)', cursor: 'pointer', textAlign: 'left',
              transition: 'border-color 0.2s',
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--primary)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontSize: '2rem' }}>👤</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: '1rem', marginBottom: '0.25rem' }}>Member</div>
                <div style={{ fontSize: '0.85rem', color: 'var(--grey)' }}>
                  Join existing Bhishi groups using a unique group code, make monthly payments, and participate in payouts
                </div>
              </div>
              <span style={{ color: 'var(--grey)', fontSize: '1.2rem' }}>›</span>
            </div>
          </button>

          {/* Group Admin card */}
          <button
            onClick={() => selectRole('GROUP_ADMIN')}
            style={{
              width: '100%', padding: '1.2rem 1.5rem',
              border: '2px solid var(--border)', borderRadius: '12px',
              background: 'var(--card-bg)', cursor: 'pointer', textAlign: 'left',
              transition: 'border-color 0.2s',
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--primary)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontSize: '2rem' }}>🏛️</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: '1rem', marginBottom: '0.25rem' }}>Group Admin</div>
                <div style={{ fontSize: '0.85rem', color: 'var(--grey)' }}>
                  Create and manage Bhishi groups, approve member join requests, and handle monthly payouts
                </div>
                <div style={{
                  display: 'inline-block', marginTop: '0.4rem',
                  fontSize: '0.75rem', color: '#92400e',
                  background: '#fef3c7', padding: '2px 8px', borderRadius: '999px',
                }}>
                  ⏳ Requires Super Admin approval
                </div>
              </div>
              <span style={{ color: 'var(--grey)', fontSize: '1.2rem' }}>›</span>
            </div>
          </button>

          <div className="auth-footer" style={{ marginTop: '1.5rem' }}>
            Already have an account? <Link to="/login">Login</Link>
          </div>
          <div style={{
            marginTop: '0.75rem', padding: '0.65rem', background: 'var(--bg-secondary, #f5f5f5)',
            borderRadius: '8px', fontSize: '0.78rem', color: 'var(--grey)', textAlign: 'center',
          }}>
            🔒 Super Admin accounts are created by the platform operator only
          </div>
        </div>
      </div>
    );
  }

  // ── Steps 1–3: Registration Form ────────────────────────
  return (
    <div className="auth-page">
      <div className="auth-card" style={{ maxWidth: 480 }}>
        <div className="auth-logo">
          <h1>🪙 Bhishi</h1>
          <p>
            {form.role === 'GROUP_ADMIN' ? '🏛️ Group Admin Registration' : '👤 Member Registration'}
            {' '}
            <button
              onClick={() => { setStep(0); setUserId(''); }}
              style={{ fontSize: '0.75rem', color: 'var(--grey)', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline', padding: 0 }}
            >
              (change)
            </button>
          </p>
        </div>

        {/* Step progress */}
        <div className="steps">
          {stepLabels.map((label, i) => (
            <React.Fragment key={i}>
              <div className="step">
                <div>
                  <div className={`step-circle ${step > i+1 ? 'completed' : step === i+1 ? 'active' : ''}`}>
                    {step > i+1 ? '✓' : i+1}
                  </div>
                  <div className="step-label">{label}</div>
                </div>
              </div>
              {i < stepLabels.length - 1 && <div className={`step-line ${step > i+1 ? 'completed' : ''}`} />}
            </React.Fragment>
          ))}
        </div>

        {/* Step 1 */}
        {step === 1 && (
          <>
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input className="form-input" placeholder="Priya Sharma" value={form.name} onChange={e => set('name', e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input className="form-input" type="email" placeholder="priya@example.com" value={form.email} onChange={e => set('email', e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Phone</label>
              <input className="form-input" placeholder="+91XXXXXXXXXX" value={form.phone} onChange={e => set('phone', e.target.value)} />
              <p className="form-hint">Format: +91 followed by 10 digits</p>
            </div>
            <div className="form-group">
              <label className="form-label">Date of Birth</label>
              <input className="form-input" type="date" value={form.dob} onChange={e => set('dob', e.target.value)} />
            </div>

            {form.role === 'GROUP_ADMIN' && (
              <>
                <div style={{ margin: '0.5rem 0', padding: '0.6rem 0.9rem', background: '#f0f4ff', borderRadius: '8px', fontSize: '0.82rem', color: '#4b5563' }}>
                  🏛️ Group Admin — additional details
                </div>
                <div className="form-group">
                  <label className="form-label">Organization / Group Name <span style={{ color: 'red' }}>*</span></label>
                  <input className="form-input" placeholder="e.g. Sharma Family Bhishi" value={form.organizationName} onChange={e => set('organizationName', e.target.value)} />
                  <p className="form-hint">The name that will appear on your groups</p>
                </div>
                <div className="form-group">
                  <label className="form-label">Business / Alternate Phone <span style={{ color: 'var(--grey)' }}>(optional)</span></label>
                  <input className="form-input" placeholder="+91XXXXXXXXXX" value={form.businessPhone} onChange={e => set('businessPhone', e.target.value)} />
                </div>
              </>
            )}

            <button className="btn btn-primary btn-full" onClick={submitStep1} disabled={loading}>
              {loading ? 'Saving...' : 'Continue →'}
            </button>
          </>
        )}

        {/* Step 2 */}
        {step === 2 && (
          <>
            <div className="form-group">
              <label className="form-label">Last 4 digits of Aadhaar</label>
              <input className="form-input" placeholder="XXXX" maxLength={4} value={form.aadhaarLastFour} onChange={e => set('aadhaarLastFour', e.target.value)} />
              <p className="form-hint">Only last 4 digits stored for security</p>
            </div>
            <button className="btn btn-primary btn-full" onClick={submitStep2} disabled={loading}>
              {loading ? 'Saving...' : 'Save & Continue →'}
            </button>
          </>
        )}

        {/* Step 3 */}
        {step === 3 && (
          <>
            <div className="form-group">
              <label className="form-label">Set Password</label>
              <input className="form-input" type="password" placeholder="Min 8 characters" value={form.password} onChange={e => set('password', e.target.value)} />
            </div>
            <div className="form-group" style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
              <input type="checkbox" id="terms" checked={form.termsAccepted} onChange={e => set('termsAccepted', e.target.checked)} style={{ marginTop: '3px' }} />
              <label htmlFor="terms" style={{ fontSize: '0.85rem', color: 'var(--grey)' }}>
                I agree to the Terms & Conditions of the Bhishi Platform (Simulation)
              </label>
            </div>
            {form.role === 'GROUP_ADMIN' && (
              <div style={{ padding: '0.75rem', background: '#fef3c7', borderRadius: '8px', fontSize: '0.82rem', color: '#92400e', marginBottom: '1rem' }}>
                ⏳ After registering, your Group Admin account will be reviewed by the Super Admin before you can create groups.
              </div>
            )}
            <button className="btn btn-primary btn-full" onClick={submitStep3} disabled={loading}>
              {loading ? 'Completing...' : 'Complete Registration ✓'}
            </button>
          </>
        )}

        <div className="auth-footer">Already have an account? <Link to="/login">Login</Link></div>
      </div>
    </div>
  );
}