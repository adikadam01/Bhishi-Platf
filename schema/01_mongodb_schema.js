// ============================================================
// DIGITAL BHISHI PLATFORM — MongoDB Schema
// Phase 1: All 9 collections with indexes and sample documents
// ============================================================

// ─────────────────────────────────────────────
// 1. USERS
// ─────────────────────────────────────────────
db.createCollection("users");
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ phone: 1 }, { unique: true });
db.users.createIndex({ role: 1 });
db.users.createIndex({ status: 1 });
/*
{
  _id: ObjectId(), name: "Priya Sharma", email: "priya@example.com",
  phone: "+919876543210", passwordHash: "$2b$12$...", dob: ISODate("1995-06-15"),
  role: "MEMBER",           // SUPER_ADMIN | GROUP_ADMIN | MEMBER
  status: "ACTIVE",         // PENDING_VERIFICATION | ACTIVE | SUSPENDED | REJECTED
  profilePhotoUrl: "https://...", aadhaarLastFour: "6789",
  termsAccepted: true, createdAt: ISODate("..."), updatedAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 2. GROUPS
// ─────────────────────────────────────────────
db.createCollection("groups");
db.groups.createIndex({ groupCode: 1 }, { unique: true, sparse: true });
db.groups.createIndex({ adminId: 1 });
db.groups.createIndex({ status: 1 });
/*
{
  _id: ObjectId(), name: "Pune Ladies Bhishi", adminId: ObjectId("..."),
  totalAmount: 10000, maxMembers: 10, contributionPerMember: 1000,
  payoutMethod: "FIXED_ROTATION",  // FIXED_ROTATION | BIDDING | CONTROLLED_RANDOM
  status: "ACTIVE",                // PENDING_APPROVAL | ACTIVE | COMPLETED | CANCELLED
  groupCode: "PLB2024", dueDayOfMonth: 5, penaltyAmount: 50, currentCycleMonth: 3,
  superAdminApprovedAt: ISODate("..."), createdAt: ISODate("..."), updatedAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 3. GROUP_MEMBERS
// ─────────────────────────────────────────────
db.createCollection("group_members");
db.group_members.createIndex({ groupId: 1, userId: 1 }, { unique: true });
db.group_members.createIndex({ groupId: 1, status: 1 });
db.group_members.createIndex({ userId: 1 });
/*
{
  _id: ObjectId(), groupId: ObjectId("..."), userId: ObjectId("..."),
  status: "ACTIVE",           // PENDING | ACTIVE | REMOVED | LEFT
  joinRequestedAt: ISODate("..."), joinApprovedAt: ISODate("..."),
  rotationOrder: 3, hasReceivedPayout: false, payoutReceivedOnCycle: null,
  createdAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 4. PAYMENTS
// ─────────────────────────────────────────────
db.createCollection("payments");
db.payments.createIndex({ groupId: 1, cycleMonth: 1, cycleYear: 1 });
db.payments.createIndex({ userId: 1, groupId: 1 });
db.payments.createIndex({ status: 1 });
db.payments.createIndex({ razorpayOrderId: 1 }, { sparse: true });
/*
{
  _id: ObjectId(), groupId: ObjectId("..."), userId: ObjectId("..."),
  cycleMonth: 3, cycleYear: 2024,
  baseAmount: 1000, penaltyAmount: 50, totalAmount: 1050,
  status: "PAID",             // PENDING | PAID | LATE | WAIVED
  razorpayOrderId: "order_abc123", razorpayPaymentId: "pay_xyz789",
  dueDate: ISODate("2024-03-05T23:59:59Z"), paidAt: ISODate("..."), isLate: true,
  createdAt: ISODate("..."), updatedAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 5. PAYOUT_CYCLES
// ─────────────────────────────────────────────
db.createCollection("payout_cycles");
db.payout_cycles.createIndex({ groupId: 1, cycleNumber: 1 }, { unique: true });
db.payout_cycles.createIndex({ groupId: 1, winnerId: 1 });
db.payout_cycles.createIndex({ status: 1 });
/*
{
  _id: ObjectId(), groupId: ObjectId("..."), cycleNumber: 3, cycleMonth: 3, cycleYear: 2024,
  payoutMethod: "FIXED_ROTATION", totalCollected: 10000,
  winnerId: ObjectId("..."), winnerName: "Priya Sharma", winnerAmount: 10000,
  status: "COMPLETED",        // PENDING | IN_PROGRESS | COMPLETED
  biddingDetails: { lowestBid: 9200, discount: 800, distributedPerMember: 80 },
  randomDetails:  { eligibleMemberIds: ["..."], selectionSeed: "sha256-abc" },
  completedAt: ISODate("..."), createdAt: ISODate("..."), updatedAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 6. BIDS
// ─────────────────────────────────────────────
db.createCollection("bids");
db.bids.createIndex({ cycleId: 1, userId: 1 }, { unique: true });
db.bids.createIndex({ cycleId: 1, bidAmount: 1 });
db.bids.createIndex({ groupId: 1 });
/*
{
  _id: ObjectId(), groupId: ObjectId("..."), cycleId: ObjectId("..."), userId: ObjectId("..."),
  bidAmount: 9200, status: "WON",   // PENDING | WON | LOST | WITHDRAWN
  submittedAt: ISODate("..."), createdAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 7. URGENCY_REQUESTS
// ─────────────────────────────────────────────
db.createCollection("urgency_requests");
db.urgency_requests.createIndex({ groupId: 1, status: 1 });
db.urgency_requests.createIndex({ requestedByUserId: 1 });
/*
{
  _id: ObjectId(), groupId: ObjectId("..."), requestedByUserId: ObjectId("..."),
  reason: "Medical emergency",
  status: "APPROVED",         // PENDING | APPROVED | REJECTED | EXPIRED
  totalMembers: 10, votesFor: 7, votesAgainst: 2, votesAbstained: 1,
  votes: [ { userId: ObjectId("..."), vote: "FOR", votedAt: ISODate("...") } ],
  votingDeadline: ISODate("..."), resolvedAt: ISODate("..."), createdAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 8. OTP_TOKENS
// ─────────────────────────────────────────────
db.createCollection("otp_tokens");
db.otp_tokens.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 }); // TTL auto-delete
db.otp_tokens.createIndex({ phone: 1, purpose: 1 });
/*
{
  _id: ObjectId(), phone: "+919876543210", otp: "483920",
  purpose: "REGISTRATION",   // REGISTRATION | LOGIN | RESET
  expiresAt: ISODate("..."), used: false, createdAt: ISODate("...")
}
*/

// ─────────────────────────────────────────────
// 9. AUDIT_LOGS
// ─────────────────────────────────────────────
db.createCollection("audit_logs");
db.audit_logs.createIndex({ groupId: 1, createdAt: -1 });
db.audit_logs.createIndex({ actorId: 1 });
db.audit_logs.createIndex({ action: 1 });
/*
{
  _id: ObjectId(), actorId: ObjectId("..."), actorRole: "GROUP_ADMIN",
  action: "MEMBER_APPROVED", targetType: "GROUP_MEMBER", targetId: ObjectId("..."),
  groupId: ObjectId("..."), details: { memberName: "Priya Sharma" },
  createdAt: ISODate("...")
}
*/
