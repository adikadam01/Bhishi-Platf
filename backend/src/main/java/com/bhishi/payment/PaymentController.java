package com.bhishi.payment;

// ============================================================
// DIGITAL BHISHI PLATFORM — Payment Controller
// Phase 4 | File: PaymentController.java | Package: com.bhishi.payment
// REST Endpoints:
//
// MEMBER:
//   POST  /api/payments/order              → Create Razorpay order
//   POST  /api/payments/verify             → Verify payment after checkout
//   GET   /api/payments/history/{groupId}  → My payment history in a group
//   GET   /api/payments/{paymentId}        → Single payment detail
//
// GROUP ADMIN:
//   GET   /api/payments/cycle-summary      → All members payment status for a cycle
//   POST  /api/payments/waive-penalty      → Waive penalty for a member
//
// RAZORPAY WEBHOOK (public — verified by signature):
//   POST  /api/payments/webhook            → Razorpay server-to-server callback
//
// Links to Phase 1: Payment model + PaymentRepository
// Links to Phase 2: JWT auth, @AuthenticationPrincipal
// Links to Phase 3: Group validation inside PaymentService
// ============================================================

import com.bhishi.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ═══════════════════════════════════════════════
    // MEMBER — Create payment order
    // Returns Razorpay order details for frontend checkout
    // ═══════════════════════════════════════════════
    @PostMapping("/order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "[TEST MODE] Order created. Use Razorpay test card 4111 1111 1111 1111 to complete.", response));
    }

    // ═══════════════════════════════════════════════
    // MEMBER — Verify payment after Razorpay checkout
    // Frontend calls this after user completes payment
    // ═══════════════════════════════════════════════
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.verifyAndConfirmPayment(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully!", response));
    }

    // ═══════════════════════════════════════════════
    // MEMBER — Payment history in a group
    // ═══════════════════════════════════════════════
    @GetMapping("/history/{groupId}")
    public ResponseEntity<ApiResponse<MemberPaymentHistory>> getMyPaymentHistory(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        MemberPaymentHistory history = paymentService.getMemberPaymentHistory(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // ═══════════════════════════════════════════════
    // MEMBER / ADMIN — Single payment detail
    // ═══════════════════════════════════════════════
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable String paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.getPaymentById(paymentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════════════════════════════════════
    // GROUP ADMIN — Cycle payment summary
    // Shows all members' payment status for a given month/year
    // ═══════════════════════════════════════════════
    @GetMapping("/cycle-summary")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CyclePaymentSummary>> getCycleSummary(
            @RequestParam String groupId,
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails) {
        CyclePaymentSummary summary = paymentService.getCyclePaymentSummary(
                groupId, month, year, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ═══════════════════════════════════════════════
    // GROUP ADMIN — Waive penalty for a member
    // ═══════════════════════════════════════════════
    @PostMapping("/waive-penalty")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> waivePenalty(
            @Valid @RequestBody WaivePenaltyRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.waivePenalty(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Penalty waived successfully", response));
    }

    // ═══════════════════════════════════════════════
    // RAZORPAY WEBHOOK — public endpoint
    // Razorpay POSTs here after payment success/failure
    // Signature verified inside service before processing
    // ⚠️ This endpoint is intentionally permitAll in SecurityConfig
    // ═══════════════════════════════════════════════
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(rawPayload, signature != null ? signature : "");
        return ResponseEntity.ok("OK");
    }
}
