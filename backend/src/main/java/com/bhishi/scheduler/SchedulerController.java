package com.bhishi.scheduler;

// ============================================================
// DIGITAL BHISHI PLATFORM — Scheduler Admin Controller
// Phase 7 | File: SchedulerController.java
// Package: com.bhishi.scheduler
//
// Super Admin endpoints to manually trigger any scheduler job.
// Useful for testing, recovery, or forcing a cycle mid-month.
//
// POST /api/super-admin/scheduler/seed-payments     → JOB 1
// POST /api/super-admin/scheduler/enforce-penalties → JOB 2
// POST /api/super-admin/scheduler/send-reminders   → JOB 3
// POST /api/super-admin/scheduler/expire-urgency   → JOB 4
// POST /api/super-admin/scheduler/notify-admins    → JOB 5
// POST /api/super-admin/scheduler/check-completion → JOB 6
//
// Links to Phase 2: SUPER_ADMIN role enforcement via SecurityConfig
// ============================================================

import com.bhishi.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin/scheduler")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SchedulerController {

    private final BhishiScheduler scheduler;

    // Manually seed payments for current month
    @PostMapping("/seed-payments")
    public ResponseEntity<ApiResponse<String>> seedPayments() {
        log.info("[MANUAL TRIGGER] seed-payments by Super Admin");
        scheduler.seedMonthlyPayments();
        return ResponseEntity.ok(ApiResponse.success("Monthly payment seeding triggered successfully", null));
    }

    // Manually enforce penalties for current month
    @PostMapping("/enforce-penalties")
    public ResponseEntity<ApiResponse<String>> enforcePenalties() {
        log.info("[MANUAL TRIGGER] enforce-penalties by Super Admin");
        scheduler.enforcePenalties();
        return ResponseEntity.ok(ApiResponse.success("Penalty enforcement triggered successfully", null));
    }

    // Manually send due date reminders
    @PostMapping("/send-reminders")
    public ResponseEntity<ApiResponse<String>> sendReminders() {
        log.info("[MANUAL TRIGGER] send-reminders by Super Admin");
        scheduler.sendDueDateReminders();
        return ResponseEntity.ok(ApiResponse.success("Due date reminders triggered successfully", null));
    }

    // Manually expire stale urgency requests
    @PostMapping("/expire-urgency")
    public ResponseEntity<ApiResponse<String>> expireUrgency() {
        log.info("[MANUAL TRIGGER] expire-urgency by Super Admin");
        scheduler.expireUrgencyRequests();
        return ResponseEntity.ok(ApiResponse.success("Urgency expiry check triggered successfully", null));
    }

    // Manually notify admins that payout is ready
    @PostMapping("/notify-admins")
    public ResponseEntity<ApiResponse<String>> notifyAdmins() {
        log.info("[MANUAL TRIGGER] notify-admins by Super Admin");
        scheduler.notifyAdminForPayout();
        return ResponseEntity.ok(ApiResponse.success("Admin payout notifications triggered successfully", null));
    }

    // Manually check group completion
    @PostMapping("/check-completion")
    public ResponseEntity<ApiResponse<String>> checkCompletion() {
        log.info("[MANUAL TRIGGER] check-completion by Super Admin");
        scheduler.checkGroupCompletion();
        return ResponseEntity.ok(ApiResponse.success("Group completion check triggered successfully", null));
    }
}
