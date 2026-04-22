package com.bhishi.payout;

// ============================================================
// DIGITAL BHISHI PLATFORM — Payout Controller
// Phase 5 | File: PayoutController.java | Package: com.bhishi.payout
// REST Endpoints:
//
// GROUP ADMIN:
//   POST /api/payouts/initiate          → Start a payout cycle
//   POST /api/payouts/bidding/execute   → Close bidding + execute winner
//   GET  /api/payouts/bidding/{cycleId}/status → See all bids (admin sees all)
//
// MEMBER:
//   POST /api/payouts/bidding/bid       → Place a bid (BIDDING method only)
//   GET  /api/payouts/bidding/{cycleId}/status → See own bid
//
// ALL AUTHENTICATED:
//   GET  /api/payouts/history/{groupId} → Full payout history
//   GET  /api/payouts/current/{groupId} → Current active cycle
//
// Links to Phase 1: PayoutCycle, Bid models + repositories
// Links to Phase 2: JWT auth via @AuthenticationPrincipal
// Links to Phase 3: Group validation inside PayoutService
// Links to Phase 4: PaymentService.applyPenaltiesForGroup()
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
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    // ═══════════════════════════════════════════════
    // GROUP ADMIN — Initiate a payout cycle
    // For FIXED_ROTATION + CONTROLLED_RANDOM:
    //   winner is decided immediately and returned
    // For BIDDING:
    //   cycle opens, members can now place bids
    // ═══════════════════════════════════════════════
    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PayoutCycleResponse>> initiatePayoutCycle(
            @Valid @RequestBody InitiatePayoutRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        PayoutCycleResponse response = payoutService.initiatePayoutCycle(req, userDetails.getUsername());

        String message = switch (response.getPayoutMethod()) {
            case "FIXED_ROTATION"    -> "Payout complete! Winner selected by rotation order.";
            case "CONTROLLED_RANDOM" -> "Payout complete! Winner selected randomly with audit trail.";
            case "BIDDING"           -> "Bidding cycle opened. Members can now place their bids.";
            default                  -> "Payout cycle initiated.";
        };

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // ═══════════════════════════════════════════════
    // MEMBER — Place a bid (BIDDING method only)
    // One bid per member per cycle
    // Bid must be less than full pot amount
    // ═══════════════════════════════════════════════
    @PostMapping("/bidding/bid")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(
            @Valid @RequestBody PlaceBidRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        BidResponse response = payoutService.placeBid(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "Bid placed successfully! You will be notified if you win.", response));
    }

    // ═══════════════════════════════════════════════
    // GROUP ADMIN — Close bidding and execute winner
    // Lowest bidder wins. Discount split to others.
    // Admin can override winner via optional field.
    // ═══════════════════════════════════════════════
    @PostMapping("/bidding/execute")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PayoutCycleResponse>> executeBiddingPayout(
            @Valid @RequestBody ExecutePayoutRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        PayoutCycleResponse response = payoutService.executeBiddingPayout(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "Bidding payout executed! Winner notified and discount distributed.", response));
    }

    // ═══════════════════════════════════════════════
    // ALL — View bidding status for a cycle
    // Admin: sees all bids + amounts
    // Member: sees only their own bid
    // ═══════════════════════════════════════════════
    @GetMapping("/bidding/{cycleId}/status")
    public ResponseEntity<ApiResponse<BiddingStatusResponse>> getBiddingStatus(
            @PathVariable String cycleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        BiddingStatusResponse response = payoutService.getBiddingStatus(cycleId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════════════════════════════════════
    // ALL — Full payout history for a group
    // Shows all completed + in-progress cycles
    // ═══════════════════════════════════════════════
    @GetMapping("/history/{groupId}")
    public ResponseEntity<ApiResponse<PayoutHistoryResponse>> getPayoutHistory(
            @PathVariable String groupId) {

        PayoutHistoryResponse response = payoutService.getPayoutHistory(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════════════════════════════════════
    // ALL — Get current active cycle for a group
    // ═══════════════════════════════════════════════
    @GetMapping("/current/{groupId}")
    public ResponseEntity<ApiResponse<PayoutCycleResponse>> getCurrentCycle(
            @PathVariable String groupId) {

        PayoutCycleResponse response = payoutService.getCurrentCycle(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
