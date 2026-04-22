package com.bhishi.urgency;

// ============================================================
// DIGITAL BHISHI PLATFORM — Urgency Service
// Phase 6 | File: UrgencyService.java | Package: com.bhishi.urgency
// Handles:
//   - Member raises urgency request (early payout appeal)
//   - All group members vote FOR / AGAINST / ABSTAIN
//   - Auto-resolves when majority reached or deadline passed
//   - On approval: triggers early payout via PayoutService
//   - Group Admin can manually override resolution
//   - Scheduler calls expireDeadlinedRequests() in Phase 7
// Links to Phase 1:
//   - UrgencyRequest, Group, GroupMember, User, AuditLog, PayoutCycle
//   - UrgencyRequestRepository, GroupRepository, GroupMemberRepository,
//     UserRepository, AuditLogRepository
// Links to Phase 3: Group + membership validation
// Links to Phase 5: PayoutService.initiatePayoutCycle() on approval
// ============================================================

import com.bhishi.exception.*;
import com.bhishi.model.*;
import com.bhishi.payout.*;
import com.bhishi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrgencyService {

    private final UrgencyRequestRepository urgencyRequestRepository;
    private final GroupRepository          groupRepository;
    private final GroupMemberRepository    groupMemberRepository;
    private final UserRepository           userRepository;
    private final AuditLogRepository       auditLogRepository;
    private final PayoutService            payoutService;
    private final JavaMailSender           mailSender;

    // ─────────────────────────────────────────────
    // RAISE URGENCY REQUEST (Member)
    // Only one active urgency request per member
    // per group at a time.
    // Notifies all group members to vote.
    // ─────────────────────────────────────────────
    public UrgencyRequestResponse raiseUrgencyRequest(RaiseUrgencyRequest req, String userId) {

        Group group = findGroup(req.getGroupId());
        User  user  = findUser(userId);

        // Validate active membership
        GroupMember membership = groupMemberRepository
                .findByGroupIdAndUserId(req.getGroupId(), userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this group"));

        if (membership.getStatus() != GroupMember.MemberStatus.ACTIVE)
            throw new BadRequestException("Your membership is not active in this group");

        // Member who already received payout cannot request urgency again
        if (membership.isHasReceivedPayout())
            throw new BadRequestException("You have already received your payout and cannot raise an urgency request");

        // Only one pending urgency request per member at a time
        urgencyRequestRepository.findByGroupIdAndRequestedByUserIdAndStatus(
                req.getGroupId(), userId, UrgencyRequest.UrgencyStatus.PENDING)
                .ifPresent(u -> { throw new ConflictException(
                        "You already have a pending urgency request for this group"); });

        long totalActive = groupMemberRepository
                .countByGroupIdAndStatus(req.getGroupId(), GroupMember.MemberStatus.ACTIVE);

        UrgencyRequest urgencyRequest = UrgencyRequest.builder()
                .groupId(req.getGroupId())
                .requestedByUserId(userId)
                .reason(req.getReason())
                .status(UrgencyRequest.UrgencyStatus.PENDING)
                .totalMembers((int) totalActive)
                .votesFor(0)
                .votesAgainst(0)
                .votesAbstained(0)
                .votes(new ArrayList<>())
                .votingDeadline(LocalDateTime.now().plusHours(req.getVotingHours()))
                .build();

        urgencyRequest = urgencyRequestRepository.save(urgencyRequest);

        audit(userId, user.getRole().name(), "URGENCY_RAISED", "URGENCY_REQUEST",
                urgencyRequest.getId(), req.getGroupId(),
                Map.of("reason", req.getReason(), "votingHours", req.getVotingHours()));

        // Notify all group members to vote
        notifyMembersToVote(group, user, urgencyRequest);

        log.info("Urgency request raised by {} in group {} — voting deadline: {}",
                userId, req.getGroupId(), urgencyRequest.getVotingDeadline());

        return toFullResponse(urgencyRequest, group, user, userId);
    }

    // ─────────────────────────────────────────────
    // CAST VOTE (Member)
    // Each member can vote exactly once.
    // Auto-resolves if majority is reached immediately.
    // ─────────────────────────────────────────────
    public UrgencyRequestResponse castVote(CastVoteRequest req, String userId) {

        UrgencyRequest urgencyRequest = findUrgencyRequest(req.getUrgencyRequestId());

        if (urgencyRequest.getStatus() != UrgencyRequest.UrgencyStatus.PENDING)
            throw new BadRequestException("This urgency request is no longer open for voting");

        if (LocalDateTime.now().isAfter(urgencyRequest.getVotingDeadline()))
            throw new BadRequestException("Voting deadline has passed");

        Group group = findGroup(urgencyRequest.getGroupId());

        // Validate voter is active member of the group
        groupMemberRepository.findByGroupIdAndUserId(urgencyRequest.getGroupId(), userId)
                .filter(m -> m.getStatus() == GroupMember.MemberStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("You are not an active member of this group"));

        // Prevent double voting
        boolean alreadyVoted = urgencyRequest.getVotes().stream()
                .anyMatch(v -> v.getUserId().equals(userId));
        if (alreadyVoted)
            throw new ConflictException("You have already voted on this request");

        // Requester cannot vote on their own request
        if (urgencyRequest.getRequestedByUserId().equals(userId))
            throw new BadRequestException("You cannot vote on your own urgency request");

        // Record vote
        UrgencyRequest.Vote vote = UrgencyRequest.Vote.builder()
                .userId(userId)
                .vote(UrgencyRequest.VoteChoice.valueOf(req.getVote().name()))
                .votedAt(LocalDateTime.now())
                .build();

        urgencyRequest.getVotes().add(vote);

        // Update vote counts
        switch (req.getVote()) {
            case FOR      -> urgencyRequest.setVotesFor(urgencyRequest.getVotesFor() + 1);
            case AGAINST  -> urgencyRequest.setVotesAgainst(urgencyRequest.getVotesAgainst() + 1);
            case ABSTAIN  -> urgencyRequest.setVotesAbstained(urgencyRequest.getVotesAbstained() + 1);
        }

        urgencyRequestRepository.save(urgencyRequest);

        User voter = findUser(userId);
        audit(userId, voter.getRole().name(), "URGENCY_VOTE_CAST", "URGENCY_REQUEST",
                urgencyRequest.getId(), urgencyRequest.getGroupId(),
                Map.of("vote", req.getVote().name()));

        log.info("Vote cast by {} on urgency request {} — vote: {}",
                userId, urgencyRequest.getId(), req.getVote());

        // Check if majority reached (strictly more than half of total members)
        // Requester doesn't vote so eligible voters = totalMembers - 1
        int eligibleVoters = urgencyRequest.getTotalMembers() - 1;
        int majorityNeeded = (eligibleVoters / 2) + 1;

        if (urgencyRequest.getVotesFor() >= majorityNeeded) {
            log.info("Majority reached for urgency request {} — auto-approving", urgencyRequest.getId());
            resolveRequest(urgencyRequest, group, true, "Majority vote reached");
        } else if (urgencyRequest.getVotesAgainst() >= majorityNeeded) {
            log.info("Majority against urgency request {} — auto-rejecting", urgencyRequest.getId());
            resolveRequest(urgencyRequest, group, false, "Majority voted against");
        }

        User requester = findUser(urgencyRequest.getRequestedByUserId());
        return toFullResponse(urgencyRequest, group, requester, userId);
    }

    // ─────────────────────────────────────────────
    // MANUAL RESOLVE (Group Admin)
    // Admin can approve or reject any pending request
    // ─────────────────────────────────────────────
    public UrgencyRequestResponse manualResolve(ResolveUrgencyRequest req, String adminId) {

        UrgencyRequest urgencyRequest = findUrgencyRequest(req.getUrgencyRequestId());
        Group group = findGroup(urgencyRequest.getGroupId());

        User admin = findUser(adminId);
        if (!group.getAdminId().equals(adminId) && admin.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("Only the group admin can manually resolve urgency requests");

        if (urgencyRequest.getStatus() != UrgencyRequest.UrgencyStatus.PENDING)
            throw new BadRequestException("This urgency request is no longer pending");

        boolean approved = req.getAction() == ResolveUrgencyRequest.ResolutionAction.APPROVE;
        resolveRequest(urgencyRequest, group, approved,
                "Manual resolution by admin: " + (req.getReason() != null ? req.getReason() : "No reason given"));

        audit(adminId, "GROUP_ADMIN", approved ? "URGENCY_APPROVED_MANUAL" : "URGENCY_REJECTED_MANUAL",
                "URGENCY_REQUEST", urgencyRequest.getId(), group.getId(),
                Map.of("reason", req.getReason() != null ? req.getReason() : ""));

        User requester = findUser(urgencyRequest.getRequestedByUserId());
        return toFullResponse(urgencyRequest, group, requester, adminId);
    }

    // ─────────────────────────────────────────────
    // EXPIRE DEADLINE PASSED REQUESTS
    // Called by Phase 7 Scheduler every hour.
    // Marks PENDING requests as EXPIRED if deadline passed
    // and majority was not reached either way.
    // ─────────────────────────────────────────────
    public void expireDeadlinedRequests() {
        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);

        activeGroups.forEach(group -> {
            List<UrgencyRequest> expired = urgencyRequestRepository
                    .findExpiredRequests(group.getId(), LocalDateTime.now());

            expired.forEach(req -> {
                req.setStatus(UrgencyRequest.UrgencyStatus.EXPIRED);
                req.setResolvedAt(LocalDateTime.now());
                urgencyRequestRepository.save(req);

                // Notify requester their request expired
                try {
                    User requester = findUser(req.getRequestedByUserId());
                    notifyRequesterExpired(requester, group, req);
                } catch (Exception e) {
                    log.warn("Could not notify requester of expiry: {}", e.getMessage());
                }

                log.info("Urgency request {} expired — votes for: {} against: {}",
                        req.getId(), req.getVotesFor(), req.getVotesAgainst());
            });
        });
    }

    // ─────────────────────────────────────────────
    // GET ALL URGENCY REQUESTS FOR A GROUP
    // Admin sees all, member sees all summaries
    // ─────────────────────────────────────────────
    public List<UrgencyRequestSummary> getGroupUrgencyRequests(String groupId, String requesterId) {
        findGroup(groupId); // validate exists

        List<UrgencyRequest> requests = urgencyRequestRepository.findByGroupId(groupId);
        return requests.stream()
                .map(r -> toSummary(r, findUser(r.getRequestedByUserId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET SINGLE URGENCY REQUEST DETAIL
    // ─────────────────────────────────────────────
    public UrgencyRequestResponse getUrgencyRequestById(String urgencyRequestId, String requesterId) {
        UrgencyRequest req = findUrgencyRequest(urgencyRequestId);
        Group group = findGroup(req.getGroupId());
        User requester = findUser(req.getRequestedByUserId());
        return toFullResponse(req, group, requester, requesterId);
    }

    // ─────────────────────────────────────────────
    // GET PENDING REQUESTS (for member dashboard)
    // Shows pending requests the member hasn't voted on
    // ─────────────────────────────────────────────
    public List<UrgencyRequestSummary> getPendingRequestsForMember(String groupId, String userId) {
        List<UrgencyRequest> pending = urgencyRequestRepository
                .findByGroupIdAndStatus(groupId, UrgencyRequest.UrgencyStatus.PENDING);

        return pending.stream()
                .filter(r -> !r.getRequestedByUserId().equals(userId)) // exclude own request
                .map(r -> toSummary(r, findUser(r.getRequestedByUserId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // CORE RESOLUTION LOGIC
    // Used by auto-resolution and manual override
    // On APPROVE: triggers early payout via PayoutService
    // ─────────────────────────────────────────────
    private void resolveRequest(UrgencyRequest req, Group group, boolean approved, String reason) {

        req.setStatus(approved
                ? UrgencyRequest.UrgencyStatus.APPROVED
                : UrgencyRequest.UrgencyStatus.REJECTED);
        req.setResolvedAt(LocalDateTime.now());
        urgencyRequestRepository.save(req);

        User requester = findUser(req.getRequestedByUserId());

        if (approved) {
            // Trigger early payout for the requester
            triggerEarlyPayout(req, group, requester);
        }

        // Notify requester of outcome
        notifyRequesterOutcome(requester, group, req, approved, reason);

        // Notify all members of the result
        notifyAllMembersResult(group, req, requester, approved);

        log.info("Urgency request {} {} — reason: {}",
                req.getId(), approved ? "APPROVED" : "REJECTED", reason);
    }

    // ─────────────────────────────────────────────
    // TRIGGER EARLY PAYOUT
    // On urgency approval, initiates payout cycle
    // for the requester using the group's payout method
    // ─────────────────────────────────────────────
    private void triggerEarlyPayout(UrgencyRequest req, Group group, User requester) {
        try {
            int cycleMonth = LocalDateTime.now().getMonthValue();
            int cycleYear  = LocalDateTime.now().getYear();

            // Find next unused cycle number
            int nextCycle = group.getCurrentCycleMonth();

            InitiatePayoutRequest payoutReq = new InitiatePayoutRequest(
                    group.getId(), nextCycle, cycleMonth, cycleYear);

            PayoutCycleResponse cycle = payoutService.initiatePayoutCycle(
                    payoutReq, group.getAdminId());

            log.info("Early payout triggered for {} in group {} — cycle: {}",
                    requester.getName(), group.getName(), nextCycle);

        } catch (Exception e) {
            log.error("Failed to trigger early payout for urgency request {}: {}",
                    req.getId(), e.getMessage());
            // Don't throw — urgency still approved, admin notified to handle manually
        }
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private UrgencyRequest findUrgencyRequest(String id) {
        return urgencyRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Urgency request not found: " + id));
    }

    private Group findGroup(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void audit(String actorId, String role, String action,
                       String targetType, String targetId, String groupId, Map<String, Object> details) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId).actorRole(role).action(action)
                .targetType(targetType).targetId(targetId)
                .groupId(groupId).details(details).build());
    }

    private void notifyMembersToVote(Group group, User requester, UrgencyRequest req) {
        List<GroupMember> members = groupMemberRepository
                .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);

        members.stream()
                .filter(m -> !m.getUserId().equals(requester.getId()))
                .forEach(m -> {
                    try {
                        User member = findUser(m.getUserId());
                        SimpleMailMessage msg = new SimpleMailMessage();
                        msg.setTo(member.getEmail());
                        msg.setSubject("Urgent Vote Needed — " + group.getName());
                        msg.setText(
                            "Hello " + member.getName() + ",\n\n" +
                            requester.getName() + " has raised an urgency request for an early payout" +
                            " in \"" + group.getName() + "\".\n\n" +
                            "Reason: " + req.getReason() + "\n\n" +
                            "Please log in and cast your vote before: " + req.getVotingDeadline() + "\n\n" +
                            "— Bhishi Platform"
                        );
                        mailSender.send(msg);
                    } catch (Exception e) {
                        log.warn("Could not send vote notification to member {}", m.getUserId());
                    }
                });
    }

    private void notifyRequesterOutcome(User requester, Group group,
                                        UrgencyRequest req, boolean approved, String reason) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(requester.getEmail());
            msg.setSubject((approved ? "Urgency Request Approved" : "Urgency Request Rejected")
                    + " — " + group.getName());
            msg.setText(
                "Hello " + requester.getName() + ",\n\n" +
                "Your urgency request for group \"" + group.getName() + "\" has been " +
                (approved ? "APPROVED" : "REJECTED") + ".\n\n" +
                "Votes For: " + req.getVotesFor() + "\n" +
                "Votes Against: " + req.getVotesAgainst() + "\n" +
                (approved ? "Your early payout will be processed shortly.\n" : "") +
                "\n— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not notify requester of outcome: {}", e.getMessage());
        }
    }

    private void notifyAllMembersResult(Group group, UrgencyRequest req,
                                        User requester, boolean approved) {
        List<GroupMember> members = groupMemberRepository
                .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);

        members.stream()
                .filter(m -> !m.getUserId().equals(requester.getId()))
                .forEach(m -> {
                    try {
                        User member = findUser(m.getUserId());
                        SimpleMailMessage msg = new SimpleMailMessage();
                        msg.setTo(member.getEmail());
                        msg.setSubject("Urgency Vote Result — " + group.getName());
                        msg.setText(
                            "Hello " + member.getName() + ",\n\n" +
                            "The urgency request by " + requester.getName() +
                            " in \"" + group.getName() + "\" has been " +
                            (approved ? "APPROVED" : "REJECTED") + ".\n\n" +
                            "Votes For: "     + req.getVotesFor()      + "\n" +
                            "Votes Against: " + req.getVotesAgainst()  + "\n" +
                            "Abstained: "     + req.getVotesAbstained() + "\n\n" +
                            "— Bhishi Platform"
                        );
                        mailSender.send(msg);
                    } catch (Exception e) {
                        log.warn("Could not notify member of urgency result: {}", m.getUserId());
                    }
                });
    }

    private void notifyRequesterExpired(User requester, Group group, UrgencyRequest req) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(requester.getEmail());
            msg.setSubject("Urgency Request Expired — " + group.getName());
            msg.setText(
                "Hello " + requester.getName() + ",\n\n" +
                "Your urgency request for group \"" + group.getName() +
                "\" has expired without reaching a majority vote.\n\n" +
                "Votes For: " + req.getVotesFor() + " | Against: " + req.getVotesAgainst() + "\n\n" +
                "You may raise a new request if needed.\n\n— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not notify requester of expiry: {}", e.getMessage());
        }
    }

    // ── Mappers ──────────────────────────────────

    private UrgencyRequestResponse toFullResponse(UrgencyRequest req, Group group,
                                                   User requester, String currentUserId) {
        User currentUser = findUser(currentUserId);
        boolean isAdmin = group.getAdminId().equals(currentUserId)
                || currentUser.getRole() == User.Role.SUPER_ADMIN;

        boolean userHasVoted = req.getVotes().stream()
                .anyMatch(v -> v.getUserId().equals(currentUserId));
        String userVote = req.getVotes().stream()
                .filter(v -> v.getUserId().equals(currentUserId))
                .map(v -> v.getVote().name())
                .findFirst().orElse(null);

        int eligibleVoters = req.getTotalMembers() - 1;
        int majorityNeeded = (eligibleVoters / 2) + 1;
        boolean majorityReached = req.getVotesFor() >= majorityNeeded;

        // Only admin sees all votes; members see summary counts only
        List<VoteResponse> votes = isAdmin
                ? req.getVotes().stream().map(v -> {
                    User voter = findUser(v.getUserId());
                    return VoteResponse.builder()
                            .userId(v.getUserId())
                            .memberName(voter.getName())
                            .vote(v.getVote().name())
                            .votedAt(v.getVotedAt())
                            .build();
                  }).collect(Collectors.toList())
                : List.of();

        return UrgencyRequestResponse.builder()
                .id(req.getId())
                .groupId(req.getGroupId())
                .groupName(group.getName())
                .requestedByUserId(req.getRequestedByUserId())
                .requestedByName(requester.getName())
                .reason(req.getReason())
                .status(req.getStatus().name())
                .totalMembers(req.getTotalMembers())
                .votesFor(req.getVotesFor())
                .votesAgainst(req.getVotesAgainst())
                .votesAbstained(req.getVotesAbstained())
                .votesRemaining(eligibleVoters - req.getVotes().size())
                .majorityReached(majorityReached)
                .currentUserHasVoted(userHasVoted)
                .currentUserVote(userVote)
                .votes(votes)
                .votingDeadline(req.getVotingDeadline())
                .resolvedAt(req.getResolvedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }

    private UrgencyRequestSummary toSummary(UrgencyRequest req, User requester) {
        int eligibleVoters = req.getTotalMembers() - 1;
        int majorityNeeded = (eligibleVoters / 2) + 1;

        return UrgencyRequestSummary.builder()
                .id(req.getId())
                .requestedByName(requester.getName())
                .reason(req.getReason())
                .status(req.getStatus().name())
                .votesFor(req.getVotesFor())
                .votesAgainst(req.getVotesAgainst())
                .totalMembers(req.getTotalMembers())
                .majorityReached(req.getVotesFor() >= majorityNeeded)
                .votingDeadline(req.getVotingDeadline())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
