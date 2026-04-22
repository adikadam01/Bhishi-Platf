package com.bhishi.scheduler;

// ============================================================
// DIGITAL BHISHI PLATFORM — Monthly Scheduler
// Phase 7 | File: BhishiScheduler.java | Package: com.bhishi.scheduler
//
// All automated background jobs that run on cron schedule:
//
//  JOB 1 — Monthly Payment Seeding      (1st of every month, 00:01)
//           Creates PENDING payment records for all active members
//           in all active groups for the new cycle month.
//
//  JOB 2 — Penalty Enforcement          (Every day at 00:05)
//           Scans all PENDING payments past their due date
//           and marks them LATE + adds penalty amount.
//
//  JOB 3 — Due Date Reminder            (3 days before due date, 09:00)
//           Sends email reminders to members who haven't paid yet.
//
//  JOB 4 — Urgency Request Expiry       (Every hour)
//           Marks PENDING urgency requests as EXPIRED if
//           voting deadline has passed without majority.
//
//  JOB 5 — Cycle Completion Check       (10th of every month, 10:00)
//           Checks if all members in a group have paid
//           and notifies Group Admin to initiate payout.
//
//  JOB 6 — Group Completion Check       (Monthly)
//           Marks group as COMPLETED when all members
//           have received their payout.
//
// Links to Phase 1: All models + repositories
// Links to Phase 2: User model for notifications
// Links to Phase 3: GroupService group validation
// Links to Phase 4: PaymentService.applyPenaltiesForGroup()
// Links to Phase 5: PayoutService for cycle management
// Links to Phase 6: UrgencyService.expireDeadlinedRequests()
// ============================================================

import com.bhishi.model.*;
import com.bhishi.payment.PaymentService;
import com.bhishi.repository.*;
import com.bhishi.urgency.UrgencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BhishiScheduler {

    private final GroupRepository       groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PaymentRepository     paymentRepository;
    private final PayoutCycleRepository payoutCycleRepository;
    private final UserRepository        userRepository;
    private final AuditLogRepository    auditLogRepository;
    private final PaymentService        paymentService;
    private final UrgencyService        urgencyService;
    private final JavaMailSender        mailSender;

    // ─────────────────────────────────────────────
    // JOB 1 — MONTHLY PAYMENT SEEDING
    // Runs: 1st of every month at 00:01
    // Creates a PENDING Payment record for every
    // active member in every active group.
    // Members then use PaymentController to pay.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 1 0 1 * *")
    public void seedMonthlyPayments() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();

        log.info("[SCHEDULER] JOB 1 — Seeding payments for {}/{}", month, year);

        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);
        int totalSeeded = 0;

        for (Group group : activeGroups) {
            List<GroupMember> activeMembers = groupMemberRepository
                    .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);

            LocalDateTime dueDate = LocalDateTime.of(year, month, group.getDueDayOfMonth(), 23, 59, 59);

            for (GroupMember member : activeMembers) {
                // Skip if payment already exists for this cycle
                boolean exists = paymentRepository
                        .findByGroupIdAndUserIdAndCycleMonthAndCycleYear(
                                group.getId(), member.getUserId(), month, year)
                        .isPresent();

                if (!exists) {
                    Payment payment = Payment.builder()
                            .groupId(group.getId())
                            .userId(member.getUserId())
                            .cycleMonth(month)
                            .cycleYear(year)
                            .baseAmount(group.getContributionPerMember())
                            .penaltyAmount(0)
                            .totalAmount(group.getContributionPerMember())
                            .status(Payment.PaymentStatus.PENDING)
                            .dueDate(dueDate)
                            .isLate(false)
                            .build();

                    paymentRepository.save(payment);
                    totalSeeded++;
                }
            }

            // Advance group cycle month
            group.setCurrentCycleMonth(group.getCurrentCycleMonth() + 1);
            groupRepository.save(group);

            log.info("[SCHEDULER] Seeded {} payments for group: {}", activeMembers.size(), group.getName());
        }

        audit("SYSTEM", "SCHEDULER", "MONTHLY_PAYMENTS_SEEDED", "PAYMENT", null, null,
                Map.of("month", month, "year", year, "totalSeeded", totalSeeded));

        log.info("[SCHEDULER] JOB 1 DONE — {} total payments seeded for {}/{}", totalSeeded, month, year);
    }

    // ─────────────────────────────────────────────
    // JOB 2 — PENALTY ENFORCEMENT
    // Runs: Every day at 00:05
    // For each active group, checks if due date
    // has passed and marks PENDING payments as
    // LATE with penalty added.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 5 0 * * *")
    public void enforcePenalties() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();

        log.info("[SCHEDULER] JOB 2 — Enforcing penalties for {}/{}", month, year);

        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);
        int totalPenalised = 0;

        for (Group group : activeGroups) {
            LocalDateTime dueDate = LocalDateTime.of(year, month, group.getDueDayOfMonth(), 23, 59, 59);

            // Only apply penalties if due date has passed
            if (LocalDateTime.now().isAfter(dueDate)) {
                int count = paymentService.applyPenaltiesForGroup(group.getId(), month, year);
                totalPenalised += count;

                if (count > 0) {
                    // Notify each late member
                    List<Payment> latePayments = paymentRepository
                            .findByGroupIdAndCycleMonthAndCycleYear(group.getId(), month, year)
                            .stream()
                            .filter(p -> p.getStatus() == Payment.PaymentStatus.LATE)
                            .toList();

                    latePayments.forEach(p -> notifyLateMember(p, group));
                    log.info("[SCHEDULER] Penalised {} members in group: {}", count, group.getName());
                }
            }
        }

        audit("SYSTEM", "SCHEDULER", "PENALTIES_ENFORCED", "PAYMENT", null, null,
                Map.of("month", month, "year", year, "totalPenalised", totalPenalised));

        log.info("[SCHEDULER] JOB 2 DONE — {} late payments penalised", totalPenalised);
    }

    // ─────────────────────────────────────────────
    // JOB 3 — DUE DATE REMINDER EMAILS
    // Runs: Every day at 09:00
    // Sends reminders 3 days, 1 day before due date
    // and on the due date itself to unpaid members.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueDateReminders() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();
        LocalDate today = LocalDate.now();

        log.info("[SCHEDULER] JOB 3 — Sending due date reminders");

        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);
        int totalReminders = 0;

        for (Group group : activeGroups) {
            LocalDate dueDate = LocalDate.of(year, month, group.getDueDayOfMonth());
            long daysUntilDue = today.until(dueDate).getDays();

            // Send reminders at 3 days before, 1 day before, and on due date
            if (daysUntilDue == 3 || daysUntilDue == 1 || daysUntilDue == 0) {
                List<Payment> pendingPayments = paymentRepository
                        .findByGroupIdAndCycleMonthAndCycleYear(group.getId(), month, year)
                        .stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING
                                  || p.getStatus() == Payment.PaymentStatus.LATE)
                        .toList();

                for (Payment payment : pendingPayments) {
                    try {
                        User member = findUser(payment.getUserId());
                        sendReminderEmail(member, group, payment, daysUntilDue);
                        totalReminders++;
                    } catch (Exception e) {
                        log.warn("[SCHEDULER] Could not send reminder to user {}: {}",
                                payment.getUserId(), e.getMessage());
                    }
                }

                log.info("[SCHEDULER] Sent {} reminders for group: {} ({} days to due)",
                        pendingPayments.size(), group.getName(), daysUntilDue);
            }
        }

        log.info("[SCHEDULER] JOB 3 DONE — {} reminder emails sent", totalReminders);
    }

    // ─────────────────────────────────────────────
    // JOB 4 — URGENCY REQUEST EXPIRY
    // Runs: Every hour at :00
    // Delegates to UrgencyService to mark expired
    // requests and notify requesters.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 * * * *")
    public void expireUrgencyRequests() {
        log.info("[SCHEDULER] JOB 4 — Checking for expired urgency requests");
        try {
            urgencyService.expireDeadlinedRequests();
            log.info("[SCHEDULER] JOB 4 DONE — urgency expiry check complete");
        } catch (Exception e) {
            log.error("[SCHEDULER] JOB 4 ERROR — {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // JOB 5 — CYCLE COMPLETION NOTIFICATION
    // Runs: 10th of every month at 10:00
    // Checks if all members have paid for this cycle
    // and notifies Group Admin to initiate payout.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 10 10 * *")
    public void notifyAdminForPayout() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();

        log.info("[SCHEDULER] JOB 5 — Checking cycle completion for {}/{}", month, year);

        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);

        for (Group group : activeGroups) {
            long totalActive = groupMemberRepository
                    .countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
            long paidCount   = paymentRepository
                    .countPaidByCycle(group.getId(), month, year);

            boolean allPaid  = paidCount >= totalActive;
            boolean anyPaid  = paidCount > 0;

            // Check if payout already initiated for this cycle
            boolean payoutExists = payoutCycleRepository
                    .findByGroupIdAndCycleNumber(group.getId(), group.getCurrentCycleMonth())
                    .isPresent();

            if (!payoutExists) {
                try {
                    User admin = findUser(group.getAdminId());
                    notifyAdminPayoutReady(admin, group, (int) paidCount, (int) totalActive, allPaid);
                    log.info("[SCHEDULER] Notified admin of group {} — {}/{} paid",
                            group.getName(), paidCount, totalActive);
                } catch (Exception e) {
                    log.warn("[SCHEDULER] Could not notify admin for group {}: {}",
                            group.getId(), e.getMessage());
                }
            }
        }

        log.info("[SCHEDULER] JOB 5 DONE");
    }

    // ─────────────────────────────────────────────
    // JOB 6 — GROUP COMPLETION CHECK
    // Runs: Last day of every month at 23:00
    // Marks group COMPLETED when all members
    // have received their payout.
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 23 L * *")
    public void checkGroupCompletion() {
        log.info("[SCHEDULER] JOB 6 — Checking group completion");

        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);
        int completedCount = 0;

        for (Group group : activeGroups) {
            long totalActive     = groupMemberRepository
                    .countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
            long completedCycles = payoutCycleRepository
                    .findByGroupIdAndStatus(group.getId(), PayoutCycle.CycleStatus.COMPLETED)
                    .size();

            // Group is complete when every member has received payout
            if (completedCycles >= totalActive && totalActive > 0) {
                group.setStatus(Group.GroupStatus.COMPLETED);
                groupRepository.save(group);
                completedCount++;

                notifyGroupCompleted(group);
                audit("SYSTEM", "SCHEDULER", "GROUP_COMPLETED", "GROUP",
                        group.getId(), group.getId(),
                        Map.of("totalCycles", completedCycles, "totalMembers", totalActive));

                log.info("[SCHEDULER] Group {} marked COMPLETED — all {} payouts done",
                        group.getName(), totalActive);
            }
        }

        log.info("[SCHEDULER] JOB 6 DONE — {} groups completed", completedCount);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private void audit(String actorId, String role, String action,
                       String targetType, String targetId, String groupId,
                       Map<String, Object> details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actorId != null ? actorId : "SYSTEM")
                    .actorRole(role)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .groupId(groupId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.warn("[SCHEDULER] Could not write audit log: {}", e.getMessage());
        }
    }

    private void sendReminderEmail(User member, Group group, Payment payment, long daysUntilDue) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(member.getEmail());
            String subject = daysUntilDue == 0
                    ? "⚠️ Payment Due TODAY — " + group.getName()
                    : "Reminder: Payment due in " + daysUntilDue + " day(s) — " + group.getName();
            msg.setSubject(subject);
            msg.setText(
                "Hello " + member.getName() + ",\n\n" +
                (daysUntilDue == 0
                    ? "Your contribution of ₹" + payment.getTotalAmount() + " for \"" +
                      group.getName() + "\" is due TODAY.\n"
                    : "Your contribution of ₹" + payment.getTotalAmount() + " for \"" +
                      group.getName() + "\" is due in " + daysUntilDue + " day(s).\n") +
                (payment.isLate()
                    ? "⚠️ A penalty of ₹" + payment.getPenaltyAmount() + " has already been applied.\n"
                    : "") +
                "\nPlease log in and complete your payment to avoid penalties.\n\n" +
                "— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[SCHEDULER] Could not send reminder to {}: {}", member.getEmail(), e.getMessage());
        }
    }

    private void notifyLateMember(Payment payment, Group group) {
        try {
            User member = findUser(payment.getUserId());
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(member.getEmail());
            msg.setSubject("Late Payment Penalty Applied — " + group.getName());
            msg.setText(
                "Hello " + member.getName() + ",\n\n" +
                "Your contribution for \"" + group.getName() + "\" was not paid on time.\n\n" +
                "Base amount:    ₹" + payment.getBaseAmount()    + "\n" +
                "Penalty added:  ₹" + payment.getPenaltyAmount() + "\n" +
                "Total now due:  ₹" + payment.getTotalAmount()   + "\n\n" +
                "Please log in and pay as soon as possible.\n\n— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[SCHEDULER] Could not notify late member {}: {}", payment.getUserId(), e.getMessage());
        }
    }

    private void notifyAdminPayoutReady(User admin, Group group,
                                         int paidCount, int totalMembers, boolean allPaid) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(admin.getEmail());
            msg.setSubject((allPaid ? "✅ All Paid — " : "⚠️ Partial Collection — ")
                    + "Initiate Payout for " + group.getName());
            msg.setText(
                "Hello " + admin.getName() + ",\n\n" +
                (allPaid
                    ? "All " + totalMembers + " members have paid their contribution for this cycle.\n"
                    : paidCount + " out of " + totalMembers + " members have paid.\n") +
                "\nCycle: " + group.getCurrentCycleMonth() + "\n" +
                "Payout method: " + group.getPayoutMethod().name() + "\n\n" +
                "Please log in to initiate the payout for this cycle.\n\n" +
                "— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[SCHEDULER] Could not notify admin {}: {}", admin.getEmail(), e.getMessage());
        }
    }

    private void notifyGroupCompleted(Group group) {
        try {
            User admin = findUser(group.getAdminId());
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(admin.getEmail());
            msg.setSubject("🎉 Bhishi Group Completed — " + group.getName());
            msg.setText(
                "Hello " + admin.getName() + ",\n\n" +
                "Congratulations! Your Bhishi group \"" + group.getName() + "\" has been successfully completed.\n\n" +
                "All members have received their payout.\n\n" +
                "Thank you for using the Bhishi Platform!\n\n" +
                "— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[SCHEDULER] Could not notify admin of group completion: {}", e.getMessage());
        }
    }
}
