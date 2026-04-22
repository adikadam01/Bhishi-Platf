package com.bhishi.urgency;

// ============================================================
// DIGITAL BHISHI PLATFORM — Urgency Controller
// Phase 6 | File: UrgencyController.java | Package: com.bhishi.urgency
// REST Endpoints:
//
// MEMBER:
//   POST /api/urgency/raise                        → Raise urgency request
//   POST /api/urgency/vote                         → Cast vote
//   GET  /api/urgency/group/{groupId}              → All requests in group
//   GET  /api/urgency/group/{groupId}/pending      → Pending (not voted yet)
//   GET  /api/urgency/{urgencyRequestId}           → Single request detail
//
// GROUP ADMIN:
//   POST /api/urgency/resolve                      → Manually approve/reject
//   GET  /api/urgency/group/{groupId}              → Same — admin sees all vote details
//
// Links to Phase 1: UrgencyRequest model + repository
// Links to Phase 2: JWT auth via @AuthenticationPrincipal
// Links to Phase 3: Group membership validation in UrgencyService
// Links to Phase 5: PayoutService triggered on approval
// ============================================================

import com.bhishi.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urgency")
@RequiredArgsConstructor
public class UrgencyController {

    private final UrgencyService urgencyService;

    // ═══════════════════════════════════════════════
    // MEMBER — Raise an urgency request
    // Sends vote notification email to all group members
    // ═══════════════════════════════════════════════
    @PostMapping("/raise")
    public ResponseEntity<ApiResponse<UrgencyRequestResponse>> raiseUrgencyRequest(
            @Valid @RequestBody RaiseUrgencyRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        UrgencyRequestResponse response = urgencyService.raiseUrgencyRequest(req, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Urgency request raised. All group members have been notified to vote.", response));
    }

    // ═══════════════════════════════════════════════
    // MEMBER — Cast a vote on an urgency request
    // Auto-resolves if majority reached immediately
    // ═══════════════════════════════════════════════
    @PostMapping("/vote")
    public ResponseEntity<ApiResponse<UrgencyRequestResponse>> castVote(
            @Valid @RequestBody CastVoteRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        UrgencyRequestResponse response = urgencyService.castVote(req, userDetails.getUsername());

        String message = switch (response.getStatus()) {
            case "APPROVED" -> "Vote cast. Majority reached — urgency request APPROVED! Early payout triggered.";
            case "REJECTED" -> "Vote cast. Majority voted against — urgency request REJECTED.";
            default         -> "Vote cast successfully.";
        };

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // ═══════════════════════════════════════════════
    // GROUP ADMIN — Manually approve or reject
    // Used when voting is stuck or deadline is near
    // ═══════════════════════════════════════════════
    @PostMapping("/resolve")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UrgencyRequestResponse>> manualResolve(
            @Valid @RequestBody ResolveUrgencyRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        UrgencyRequestResponse response = urgencyService.manualResolve(req, userDetails.getUsername());
        String message = req.getAction() == ResolveUrgencyRequest.ResolutionAction.APPROVE
                ? "Urgency request approved manually. Early payout triggered."
                : "Urgency request rejected manually.";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // ═══════════════════════════════════════════════
    // ALL — Get all urgency requests for a group
    // Admin sees full vote details
    // Member sees summary counts only
    // ═══════════════════════════════════════════════
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<UrgencyRequestSummary>>> getGroupUrgencyRequests(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<UrgencyRequestSummary> requests = urgencyService
                .getGroupUrgencyRequests(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // ═══════════════════════════════════════════════
    // MEMBER — Pending requests they haven't voted on
    // Used in member dashboard as action items
    // ═══════════════════════════════════════════════
    @GetMapping("/group/{groupId}/pending")
    public ResponseEntity<ApiResponse<List<UrgencyRequestSummary>>> getPendingRequestsForMember(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<UrgencyRequestSummary> pending = urgencyService
                .getPendingRequestsForMember(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    // ═══════════════════════════════════════════════
    // ALL — Get single urgency request full detail
    // ═══════════════════════════════════════════════
    @GetMapping("/{urgencyRequestId}")
    public ResponseEntity<ApiResponse<UrgencyRequestResponse>> getUrgencyRequestById(
            @PathVariable String urgencyRequestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UrgencyRequestResponse response = urgencyService
                .getUrgencyRequestById(urgencyRequestId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
