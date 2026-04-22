package com.bhishi.payment;

// ============================================================
// DIGITAL BHISHI PLATFORM — Payment Service
// Phase 4 | File: PaymentService.java | Package: com.bhishi.payment
// Handles:
//   - Create Razorpay order for monthly contribution
//   - Verify payment (frontend callback + webhook)
//   - Penalty calculation (auto if past due date)
//   - Payment history for member and admin
//   - Cycle payment summary for admin dashboard
//   - Waive penalty (Group Admin)
// Links to Phase 1:
//   - Payment, Group, GroupMember, User, AuditLog models
//   - PaymentRepository, GroupRepository, GroupMemberRepository,
//     UserRepository, AuditLogRepository
// Links to Phase 2: User identity from JWT
// Links to Phase 3: Group + member validation
// ============================================================

import com.bhishi.exception.*;
import com.bhishi.model.*;
import com.bhishi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository     paymentRepository;
    private final GroupRepository       groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository        userRepository;
    private final AuditLogRepository    auditLogRepository;
    private final RazorpayConfig        razorpayConfig;
    private final JavaMailSender        mailSender;

    @Value("${app.mail.from}") private String mailFrom;

    // ─────────────────────────────────────────────
    // CREATE PAYMENT ORDER
    // Member calls this to initiate their monthly
    // contribution payment. Returns Razorpay order
    // details for the frontend checkout widget.
    // ─────────────────────────────────────────────
    public PaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest req, String userId) {

        Group group = findGroup(req.getGroupId());
        User  user  = findUser(userId);

        // Validate membership
        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(req.getGroupId(), userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this group"));

        if (member.getStatus() != GroupMember.MemberStatus.ACTIVE)
            throw new BadRequestException("Your membership is not active in this group");

        // Check if already paid for this cycle
        paymentRepository.findByGroupIdAndUserIdAndCycleMonthAndCycleYear(
                req.getGroupId(), userId, req.getCycleMonth(), req.getCycleYear())
                .ifPresent(p -> {
                    if (p.getStatus() == Payment.PaymentStatus.PAID)
                        throw new ConflictException("You have already paid for cycle "
                                + req.getCycleMonth() + "/" + req.getCycleYear());
                });

        // Calculate due date for the requested cycle
        LocalDateTime dueDate = LocalDateTime.of(
                req.getCycleYear(), req.getCycleMonth(), group.getDueDayOfMonth(), 23, 59, 59);

        // Determine if late and calculate penalty
        boolean isLate       = LocalDateTime.now().isAfter(dueDate);
        double  penalty      = isLate ? group.getPenaltyAmount() : 0.0;
        double  base         = group.getContributionPerMember();
        double  total        = base + penalty;

        // Create or update payment record
        Payment payment = paymentRepository
                .findByGroupIdAndUserIdAndCycleMonthAndCycleYear(
                        req.getGroupId(), userId, req.getCycleMonth(), req.getCycleYear())
                .orElse(Payment.builder()
                        .groupId(req.getGroupId())
                        .userId(userId)
                        .cycleMonth(req.getCycleMonth())
                        .cycleYear(req.getCycleYear())
                        .dueDate(dueDate)
                        .build());

        payment.setBaseAmount(base);
        payment.setPenaltyAmount(penalty);
        payment.setTotalAmount(total);
        payment.setLate(isLate);
        payment.setStatus(isLate ? Payment.PaymentStatus.LATE : Payment.PaymentStatus.PENDING);

        // Create Razorpay order (TEST MODE — returns fake order ID)
        String receipt    = "bhishi_" + userId.substring(0, 6) + "_" + req.getCycleMonth() + "_" + req.getCycleYear();
        String razorpayOrderId = razorpayConfig.createOrder(total, "INR", receipt);

        payment.setRazorpayOrderId(razorpayOrderId);
        payment = paymentRepository.save(payment);

        log.info("[TEST MODE] Payment order created: {} for user: {} group: {}",
                razorpayOrderId, userId, req.getGroupId());

        return PaymentOrderResponse.builder()
                .paymentId(payment.getId())
                .razorpayOrderId(razorpayOrderId)
                .razorpayKeyId(razorpayConfig.getKeyId())
                .amount(total)
                .baseAmount(base)
                .penaltyAmount(penalty)
                .isLate(isLate)
                .currency("INR")
                .groupName(group.getName())
                .cycleMonth(req.getCycleMonth())
                .cycleYear(req.getCycleYear())
                .dueDate(dueDate)
                .build();
    }

    // ─────────────────────────────────────────────
    // VERIFY PAYMENT (Frontend callback)
    // Called by frontend after Razorpay checkout
    // completes. Verifies signature and marks paid.
    // ─────────────────────────────────────────────
    public PaymentResponse verifyAndConfirmPayment(VerifyPaymentRequest req, String userId) {

        // Verify HMAC signature (bypassed in test mode)
        boolean signatureValid = razorpayConfig.verifySignature(
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature());

        if (!signatureValid)
            throw new BadRequestException("Payment verification failed — invalid signature");

        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for this order"));

        if (!payment.getUserId().equals(userId))
            throw new UnauthorizedException("This payment does not belong to you");

        if (payment.getStatus() == Payment.PaymentStatus.PAID)
            throw new ConflictException("This payment is already confirmed");

        // Mark payment as paid
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        User  user  = findUser(userId);
        Group group = findGroup(payment.getGroupId());

        audit(userId, "MEMBER", "PAYMENT_CONFIRMED", "PAYMENT", payment.getId(), payment.getGroupId(),
                Map.of("amount", payment.getTotalAmount(), "cycle",
                        payment.getCycleMonth() + "/" + payment.getCycleYear(),
                        "razorpayPaymentId", req.getRazorpayPaymentId()));

        sendPaymentConfirmationEmail(user, group, payment);

        log.info("Payment confirmed: {} for user: {} amount: ₹{}",
                payment.getId(), userId, payment.getTotalAmount());

        return toPaymentResponse(payment, group, user);
    }

    // ─────────────────────────────────────────────
    // RAZORPAY WEBHOOK HANDLER
    // Razorpay POSTs to this endpoint after payment.
    // Acts as backup to frontend callback.
    // ─────────────────────────────────────────────
    public void handleWebhook(String rawPayload, String razorpaySignature) {

        // Verify webhook authenticity
        boolean valid = razorpayConfig.verifyWebhookSignature(rawPayload, razorpaySignature);
        if (!valid) {
            log.warn("Invalid webhook signature received — ignoring");
            return;
        }

        // In test mode: just log — real parsing happens in production
        log.info("[TEST MODE] Webhook received and signature verified. Payload length: {}", rawPayload.length());

        // TODO (Go-Live): Parse rawPayload JSON, extract order_id and payment_id,
        //   find payment by razorpayOrderId, mark as PAID if not already.
        //   Use RazorpayWebhookPayload DTO for deserialization.
    }

    // ─────────────────────────────────────────────
    // GET PAYMENT HISTORY FOR A MEMBER
    // Shows all payments by this member in a group
    // ─────────────────────────────────────────────
    public MemberPaymentHistory getMemberPaymentHistory(String groupId, String userId) {
        User  user  = findUser(userId);
        Group group = findGroup(groupId);

        List<Payment> payments = paymentRepository.findByGroupIdAndStatus(groupId, null) // get all
                .stream().filter(p -> p.getUserId().equals(userId))
                .collect(Collectors.toList());

        // fetch all payments for this user in this group instead
        List<Payment> memberPayments = paymentRepository.findByUserId(userId)
                .stream().filter(p -> p.getGroupId().equals(groupId))
                .collect(Collectors.toList());

        long paid    = memberPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PAID).count();
        long pending = memberPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING).count();
        long late    = memberPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.LATE).count();
        double totalPaid = memberPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .mapToDouble(Payment::getTotalAmount).sum();

        List<PaymentResponse> responses = memberPayments.stream()
                .map(p -> toPaymentResponse(p, group, user))
                .collect(Collectors.toList());

        return MemberPaymentHistory.builder()
                .userId(userId)
                .memberName(user.getName())
                .totalPaid((int) paid)
                .totalPending((int) pending)
                .totalLate((int) late)
                .totalAmountPaid(totalPaid)
                .payments(responses)
                .build();
    }

    // ─────────────────────────────────────────────
    // GET CYCLE PAYMENT SUMMARY (Admin Dashboard)
    // Shows all members' payment status for a cycle
    // ─────────────────────────────────────────────
    public CyclePaymentSummary getCyclePaymentSummary(String groupId, int month, int year, String adminId) {
        Group group = findGroup(groupId);

        // Validate admin
        User admin = findUser(adminId);
        if (!group.getAdminId().equals(adminId) && admin.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("Not authorised to view this group's payments");

        List<Payment> cyclePayments = paymentRepository
                .findByGroupIdAndCycleMonthAndCycleYear(groupId, month, year);

        long paid    = cyclePayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PAID).count();
        long pending = cyclePayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING).count();
        long late    = cyclePayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.LATE).count();
        double collected = cyclePayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .mapToDouble(Payment::getTotalAmount).sum();
        double expected = group.getContributionPerMember() * group.getMaxMembers();

        LocalDateTime dueDate = LocalDateTime.of(year, month, group.getDueDayOfMonth(), 23, 59, 59);

        List<PaymentResponse> responses = cyclePayments.stream().map(p -> {
            User u = findUser(p.getUserId());
            return toPaymentResponse(p, group, u);
        }).collect(Collectors.toList());

        return CyclePaymentSummary.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .cycleMonth(month)
                .cycleYear(year)
                .totalMembers(group.getMaxMembers())
                .paidCount((int) paid)
                .pendingCount((int) pending)
                .lateCount((int) late)
                .totalCollected(collected)
                .totalExpected(expected)
                .dueDate(dueDate)
                .payments(responses)
                .build();
    }

    // ─────────────────────────────────────────────
    // WAIVE PENALTY (Group Admin)
    // Admin can waive penalty for a specific member
    // ─────────────────────────────────────────────
    public PaymentResponse waivePenalty(WaivePenaltyRequest req, String adminId) {
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Group group = findGroup(payment.getGroupId());
        User  admin = findUser(adminId);

        if (!group.getAdminId().equals(adminId) && admin.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("Only the group admin can waive penalties");

        if (payment.getStatus() == Payment.PaymentStatus.PAID)
            throw new BadRequestException("Cannot waive penalty on an already paid payment");

        // Reset penalty
        payment.setPenaltyAmount(0);
        payment.setTotalAmount(payment.getBaseAmount());
        payment.setStatus(Payment.PaymentStatus.WAIVED);
        paymentRepository.save(payment);

        User member = findUser(payment.getUserId());

        audit(adminId, "GROUP_ADMIN", "PENALTY_WAIVED", "PAYMENT", payment.getId(), payment.getGroupId(),
                Map.of("memberName", member.getName(), "reason", req.getReason(),
                        "cycle", payment.getCycleMonth() + "/" + payment.getCycleYear()));

        log.info("Penalty waived for payment: {} by admin: {} — reason: {}",
                payment.getId(), adminId, req.getReason());

        return toPaymentResponse(payment, group, member);
    }

    // ─────────────────────────────────────────────
    // GET SINGLE PAYMENT DETAIL
    // ─────────────────────────────────────────────
    public PaymentResponse getPaymentById(String paymentId, String requesterId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Group group = findGroup(payment.getGroupId());
        User  user  = findUser(payment.getUserId());

        // Allow if requester is the member, or the group admin, or super admin
        User requester = findUser(requesterId);
        boolean isOwner = payment.getUserId().equals(requesterId);
        boolean isAdmin = group.getAdminId().equals(requesterId) || requester.getRole() == User.Role.SUPER_ADMIN;

        if (!isOwner && !isAdmin)
            throw new UnauthorizedException("You are not authorised to view this payment");

        return toPaymentResponse(payment, group, user);
    }

    // ─────────────────────────────────────────────
    // APPLY PENALTIES — called by monthly scheduler
    // Marks all PENDING payments as LATE after due date
    // and adds penalty amount (Phase 5 scheduler calls this)
    // ─────────────────────────────────────────────
    public int applyPenaltiesForGroup(String groupId, int month, int year) {
        Group group = findGroup(groupId);
        LocalDateTime dueDate = LocalDateTime.of(year, month, group.getDueDayOfMonth(), 23, 59, 59);

        if (LocalDateTime.now().isBefore(dueDate)) return 0;

        List<Payment> pendingPayments = paymentRepository
                .findByGroupIdAndCycleMonthAndCycleYear(groupId, month, year)
                .stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .collect(Collectors.toList());

        pendingPayments.forEach(p -> {
            p.setLate(true);
            p.setPenaltyAmount(group.getPenaltyAmount());
            p.setTotalAmount(p.getBaseAmount() + group.getPenaltyAmount());
            p.setStatus(Payment.PaymentStatus.LATE);
            paymentRepository.save(p);
            log.info("Penalty applied to payment: {} — member: {}", p.getId(), p.getUserId());
        });

        return pendingPayments.size();
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private Group findGroup(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void audit(String actorId, String actorRole, String action,
                       String targetType, String targetId, String groupId, Map<String, Object> details) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId).actorRole(actorRole).action(action)
                .targetType(targetType).targetId(targetId)
                .groupId(groupId).details(details).build());
    }

    private void sendPaymentConfirmationEmail(User user, Group group, Payment payment) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(user.getEmail());
            msg.setSubject("Payment Confirmed — " + group.getName());
            msg.setText(
                "Hello " + user.getName() + ",\n\n" +
                "Your contribution of ₹" + payment.getTotalAmount() + " for " +
                group.getName() + " (Cycle " + payment.getCycleMonth() + "/" + payment.getCycleYear() +
                ") has been confirmed.\n\n" +
                "Payment ID: " + payment.getRazorpayPaymentId() + "\n\n" +
                "— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send payment confirmation email: {}", e.getMessage());
        }
    }

    private PaymentResponse toPaymentResponse(Payment p, Group group, User user) {
        return PaymentResponse.builder()
                .id(p.getId())
                .groupId(p.getGroupId())
                .groupName(group.getName())
                .userId(p.getUserId())
                .memberName(user.getName())
                .cycleMonth(p.getCycleMonth())
                .cycleYear(p.getCycleYear())
                .baseAmount(p.getBaseAmount())
                .penaltyAmount(p.getPenaltyAmount())
                .totalAmount(p.getTotalAmount())
                .status(p.getStatus().name())
                .isLate(p.isLate())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .dueDate(p.getDueDate())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
