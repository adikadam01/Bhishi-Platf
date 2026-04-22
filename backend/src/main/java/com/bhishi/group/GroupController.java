package com.bhishi.group;

// ============================================================
// DIGITAL BHISHI PLATFORM — Group Controller
// Phase 3 | File: GroupController.java | Package: com.bhishi.group
// REST Endpoints:
//
// GROUP ADMIN:
//   POST   /api/groups                           → Create group
//   GET    /api/groups/my-groups                 → Groups managed by admin
//   GET    /api/groups/{groupId}                 → Group details
//   GET    /api/groups/{groupId}/members         → All members
//   GET    /api/groups/{groupId}/pending         → Pending join requests
//   POST   /api/groups/{groupId}/members/action  → Approve/reject join
//   DELETE /api/groups/{groupId}/members/remove  → Remove active member
//   GET    /api/groups/{groupId}/stats           → Group dashboard stats
//
// MEMBER:
//   POST   /api/groups/join                      → Join via group code
//   GET    /api/groups/joined                    → My joined groups
//
// SUPER ADMIN:
//   GET    /api/super-admin/groups/pending       → All pending groups
//   POST   /api/super-admin/groups/action        → Approve/reject group
//
// Links to Phase 1: Group, GroupMember models + repositories
// Links to Phase 2: JWT auth via SecurityConfig, @AuthenticationPrincipal
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
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ═══════════════════════════════════════════════
    // GROUP ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════

    // Create a new group → goes to Super Admin for approval
    @PostMapping("/api/groups")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        GroupResponse response = groupService.createGroup(req, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created and sent for Super Admin approval", response));
    }

    // Get all groups managed by the logged-in admin
    @GetMapping("/api/groups/my-groups")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<GroupSummary>>> getAdminGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<GroupSummary> groups = groupService.getAdminGroups(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    // Get full details of a specific group
    @GetMapping("/api/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @PathVariable String groupId) {
        GroupResponse response = groupService.getGroupById(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Get all members of a group
    @GetMapping("/api/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getGroupMembers(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MemberResponse> members = groupService.getGroupMembers(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    // Get pending join requests (Group Admin only)
    @GetMapping("/api/groups/{groupId}/pending")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getPendingRequests(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MemberResponse> pending = groupService.getPendingRequests(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    // Approve or reject a join request (Group Admin)
    @PostMapping("/api/groups/{groupId}/members/action")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> handleJoinRequest(
            @PathVariable String groupId,
            @Valid @RequestBody MemberActionRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        MemberResponse response = groupService.handleJoinRequest(req, userDetails.getUsername());
        String msg = req.getAction() == MemberActionRequest.MemberAction.APPROVE
                ? "Member approved successfully"
                : "Member request rejected";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    // Remove an active member from the group (Group Admin)
    @DeleteMapping("/api/groups/{groupId}/members/remove")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable String groupId,
            @Valid @RequestBody RemoveMemberRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String message = groupService.removeMember(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    // Get dashboard stats for a group (Group Admin)
    @GetMapping("/api/groups/{groupId}/stats")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GroupStats>> getGroupStats(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        GroupStats stats = groupService.getGroupStats(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ═══════════════════════════════════════════════
    // MEMBER ENDPOINTS
    // ═══════════════════════════════════════════════

    // Member joins a group by entering group code
    @PostMapping("/api/groups/join")
    public ResponseEntity<ApiResponse<String>> joinGroup(
            @Valid @RequestBody JoinGroupRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String message = groupService.joinGroup(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    // Member sees all their joined/pending groups
    @GetMapping("/api/groups/joined")
    public ResponseEntity<ApiResponse<List<GroupSummary>>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<GroupSummary> groups = groupService.getMyGroups(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    // ═══════════════════════════════════════════════
    // SUPER ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════

    // Get all groups pending Super Admin approval
    @GetMapping("/api/super-admin/groups/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<GroupSummary>>> getPendingGroups() {
        List<GroupSummary> groups = groupService.getPendingGroups();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    // Super Admin approves or rejects a group
    @PostMapping("/api/super-admin/groups/action")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GroupResponse>> approveOrRejectGroup(
            @Valid @RequestBody GroupApprovalRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        GroupResponse response = groupService.approveOrRejectGroup(req, userDetails.getUsername());
        String msg = req.getAction() == GroupApprovalRequest.GroupAction.APPROVE
                ? "Group approved. Group code generated and sent to admin."
                : "Group rejected.";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }
}
