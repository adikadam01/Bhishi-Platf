// src/pages/auth/LoginPage.js
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate  = useNavigate();
  const [mode,    setMode]    = useState('otp');   // 'otp' | 'password'
  const [step,    setStep]    = useState(1);        // 1=phone, 2=otp
  const [loading, setLoading] = useState(false);
  const [form,    setForm]    = useState({ phone: '', otp: '', identifier: '', password: '' });

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const sendOtp = async () => {
    if (!form.phone) return toast.error('Enter your phone number');
    setLoading(true);
    try {
      await authAPI.sendOtp({ phone: form.phone, purpose: 'LOGIN' });
      toast.success('OTP sent!');
      setStep(2);
    } catch (e) {
      toast.error(e.response?.data?.message || 'Could not send OTP');
    } finally { setLoading(false); }
  };

  const loginOtp = async () => {
    if (!form.otp) return toast.error('Enter OTP');
    setLoading(true);
    try {
      const { data } = await authAPI.loginOtp({ phone: form.phone, otp: form.otp });
      login(data.data, data.data.token);
      toast.success(`Welcome back, ${data.data.name}!`);
      navigate('/');
    } catch (e) {
      toast.error(e.response?.data?.message || 'Login failed');
    } finally { setLoading(false); }
  };

  const loginPassword = async () => {
    if (!form.identifier || !form.password) return toast.error('Fill all fields');
    setLoading(true);
    try {
      const { data } = await authAPI.loginPassword({ identifier: form.identifier, password: form.password });
      login(data.data, data.data.token);
      toast.success(`Welcome back, ${data.data.name}!`);
      navigate('/');
    } catch (e) {
      toast.error(e.response?.data?.message || 'Login failed');
    } finally { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>🪙 Bhishi</h1>
          <p>Digital Group Savings Platform</p>
        </div>

        <div className="flex gap-1" style={{ marginBottom: '1.5rem' }}>
          <button className={`btn btn-sm ${mode==='otp'?'btn-primary':'btn-outline'}`} style={{flex:1}} onClick={()=>{setMode('otp');setStep(1);}}>OTP Login</button>
          <button className={`btn btn-sm ${mode==='password'?'btn-primary':'btn-outline'}`} style={{flex:1}} onClick={()=>setMode('password')}>Password Login</button>
        </div>

        {mode === 'otp' ? (
          <>
            {step === 1 && (
              <>
                <div className="form-group">
                  <label className="form-label">Phone Number</label>
                  <input className="form-input" placeholder="+91XXXXXXXXXX" value={form.phone} onChange={e=>set('phone',e.target.value)} />
                </div>
                <button className="btn btn-primary btn-full" onClick={sendOtp} disabled={loading}>
                  {loading ? 'Sending...' : 'Send OTP'}
                </button>
              </>
            )}
            {step === 2 && (
              <>
                <p style={{marginBottom:'1rem',color:'var(--grey)',fontSize:'0.9rem'}}>OTP sent to {form.phone}</p>
                <div className="form-group">
                  <label className="form-label">Enter OTP</label>
                  <input className="form-input" placeholder="6-digit OTP" maxLength={6} value={form.otp} onChange={e=>set('otp',e.target.value)} />
                </div>
                <button className="btn btn-primary btn-full" onClick={loginOtp} disabled={loading}>
                  {loading ? 'Verifying...' : 'Verify & Login'}
                </button>
                <button className="btn btn-ghost btn-full" style={{marginTop:'0.5rem'}} onClick={()=>setStep(1)}>← Change Number</button>
              </>
            )}
          </>
        ) : (
          <>
            <div className="form-group">
              <label className="form-label">Phone or Email</label>
              <input className="form-input" placeholder="+91XXXXXXXXXX or email" value={form.identifier} onChange={e=>set('identifier',e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input className="form-input" type="password" placeholder="••••••••" value={form.password} onChange={e=>set('password',e.target.value)} />
            </div>
            <button className="btn btn-primary btn-full" onClick={loginPassword} disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
            </button>
          </>
        )}
        <div className="auth-footer">
          New to Bhishi? <Link to="/register">Register here</Link>
        </div>
        <div style={{
          marginTop: '0.75rem', padding: '0.65rem',
          background: 'var(--bg-secondary, #f5f5f5)',
          borderRadius: '8px', fontSize: '0.78rem',
          color: 'var(--grey)', textAlign: 'center', lineHeight: '1.5',
        }}>
          Members &amp; Group Admins can register below.<br/>
          Super Admin logs in using the <strong>Password</strong> tab.
        </div>
      </div>
    </div>
  );
}