// src/services/api.js
// ============================================================
// DIGITAL BHISHI PLATFORM — API Service
// Phase 8 | Central axios instance with JWT interceptor.
// Connects React frontend to all Spring Boot endpoints.
// ============================================================
import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

// Attach JWT token to every request automatically
api.interceptors.request.use(config => {
  const token = localStorage.getItem('bhishi_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Redirect to login on 401
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('bhishi_token');
      localStorage.removeItem('bhishi_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ── Phase 2: Auth ─────────────────────────────────────────
export const authAPI = {
  registerStep1:    data => api.post('/auth/register/step1',    data),
  sendOtp:          data => api.post('/auth/otp/send',          data),
  verifyOtp:        data => api.post('/auth/otp/verify',        data),
  registerStep2:    data => api.post('/auth/register/step2',    data),
  registerStep3:    data => api.post('/auth/register/step3',    data),
  loginOtp:         data => api.post('/auth/login/otp',         data),
  loginPassword:    data => api.post('/auth/login/password',    data),
  getProfile:       ()   => api.get('/auth/me'),
};

// ── Phase 3: Groups ──────────────────────────────────────
export const groupAPI = {
  createGroup:       data        => api.post('/groups',                          data),
  getMyGroups:       ()          => api.get('/groups/my-groups'),
  getJoinedGroups:   ()          => api.get('/groups/joined'),
  getGroupById:      id          => api.get(`/groups/${id}`),
  getGroupMembers:   id          => api.get(`/groups/${id}/members`),
  getPendingMembers: id          => api.get(`/groups/${id}/pending`),
  memberAction:      (id, data)  => api.post(`/groups/${id}/members/action`,    data),
  removeMember:      (id, data)  => api.delete(`/groups/${id}/members/remove`,  { data }),
  getGroupStats:     id          => api.get(`/groups/${id}/stats`),
  joinGroup:         data        => api.post('/groups/join',                     data),
  getPendingGroups:  ()          => api.get('/super-admin/groups/pending'),
  approveGroup:      data        => api.post('/super-admin/groups/action',       data),
};

// ── Phase 4: Payments ────────────────────────────────────
export const paymentAPI = {
  createOrder:      data              => api.post('/payments/order',                data),
  verifyPayment:    data              => api.post('/payments/verify',               data),
  getHistory:       groupId          => api.get(`/payments/history/${groupId}`),
  getCycleSummary:  (gId, m, y)      => api.get('/payments/cycle-summary',         { params: { groupId: gId, month: m, year: y } }),
  waivePenalty:     data             => api.post('/payments/waive-penalty',         data),
  getPaymentById:   id               => api.get(`/payments/${id}`),
};

// ── Phase 5: Payouts ─────────────────────────────────────
export const payoutAPI = {
  initiatePayout:        data     => api.post('/payouts/initiate',         data),
  placeBid:              data     => api.post('/payouts/bidding/bid',      data),
  executeBidding:        data     => api.post('/payouts/bidding/execute',  data),
  executeBiddingPayout:  data     => api.post('/payouts/bidding/execute',  data), // alias used in ManagePayout
  getBiddingStatus:      cycleId  => api.get(`/payouts/bidding/${cycleId}/status`),
  getHistory:            groupId  => api.get(`/payouts/history/${groupId}`),
  getCurrentCycle:       groupId  => api.get(`/payouts/current/${groupId}`),
};

// ── Phase 6: Urgency ─────────────────────────────────────
export const urgencyAPI = {
  raise:          data      => api.post('/urgency/raise',                   data),
  vote:           data      => api.post('/urgency/vote',                    data),
  resolve:        data      => api.post('/urgency/resolve',                 data),
  getByGroup:     groupId   => api.get(`/urgency/group/${groupId}`),
  getPending:     groupId   => api.get(`/urgency/group/${groupId}/pending`),
  getById:        id        => api.get(`/urgency/${id}`),
};

// ── Phase 7: Scheduler (Super Admin) ─────────────────────
export const schedulerAPI = {
  seedPayments:      () => api.post('/super-admin/scheduler/seed-payments'),
  enforcePenalties:  () => api.post('/super-admin/scheduler/enforce-penalties'),
  sendReminders:     () => api.post('/super-admin/scheduler/send-reminders'),
  expireUrgency:     () => api.post('/super-admin/scheduler/expire-urgency'),
  notifyAdmins:      () => api.post('/super-admin/scheduler/notify-admins'),
  checkCompletion:   () => api.post('/super-admin/scheduler/check-completion'),
};

export default api;