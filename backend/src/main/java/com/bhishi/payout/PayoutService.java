package com.bhishi.payout;

import com.bhishi.exception.BadRequestException;
import com.bhishi.exception.ConflictException;
import com.bhishi.exception.ResourceNotFoundException;
import com.bhishi.exception.UnauthorizedException;
import com.bhishi.model.*;
import com.bhishi.payment.PaymentService;
import com.bhishi.repository.*;
import com.bhishi.util.CodeGenerator;
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
public class PayoutService {

    private final PayoutCycleRepository  payoutCycleRepository;
    private final BidRepository          bidRepository;
    private final GroupRepository        groupRepository;
    private final GroupMemberRepository  groupMemberRepository;
    private final UserRepository         userRepository;
    private final AuditLogRepository     auditLogRepository;
    private final PaymentService         paymentService;
    private final CodeGenerator          codeGenerator;
    private final JavaMailSender         mailSender;

    public PayoutCycleResponse initiatePayoutCycle(InitiatePayoutRequest req, String adminId) {

        Group group = findGroup(req.getGroupId());
        validateAdmin(adminId, group);

        if (payoutCycleRepository.findByGroupIdAndCycleNumber(req.getGroupId(), req.getCycleNumber()).isPresent())
            throw new ConflictException("Payout cycle " + req.getCycleNumber() + " already initiated for this group");

        long paidCount = paymentService.applyPenaltiesForGroup(
                req.getGroupId(), req.getCycleMonth(), req.getCycleYear());
        log.info("Penalties applied before payout: {} records updated", paidCount);

        if (group.getPayoutMethod() == Group.PayoutMethod.FIXED_ROTATION) {
            long eligibleCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
            if (eligibleCount == 0) {
                throw new BadRequestException("Cannot initiate FIXED_ROTATION payout without at least one active approved member. Approve members first.");
            }
        }

        final PayoutCycle cycle = PayoutCycle.builder()
                .groupId(req.getGroupId())
                .cycleNumber(req.getCycleNumber())
                .cycleMonth(req.getCycleMonth())
                .cycleYear(req.getCycleYear())
                .payoutMethod(group.getPayoutMethod().name())
                .totalCollected(group.getContributionPerMember() * group.getMaxMembers())
                .status(PayoutCycle.CycleStatus.IN_PROGRESS)
                .build();

        final PayoutCycle finalCycle;

        switch (group.getPayoutMethod()) {
            case FIXED_ROTATION    -> finalCycle = executeFixedRotation(cycle, group);
            case CONTROLLED_RANDOM -> finalCycle = executeControlledRandom(cycle, group);
            case BIDDING           -> {
                finalCycle = payoutCycleRepository.save(cycle);
                log.info("Bidding cycle {} opened for group {}", req.getCycleNumber(), req.getGroupId());
            }
            default -> finalCycle = cycle;
        }

        audit(adminId, "GROUP_ADMIN", "PAYOUT_CYCLE_INITIATED", "PAYOUT_CYCLE",
                finalCycle.getId(), req.getGroupId(),
                Map.of("cycleNumber", req.getCycleNumber(), "method", group.getPayoutMethod().name()));

        return toCycleResponse(finalCycle, group);
    }

    private PayoutCycle executeFixedRotation(PayoutCycle cycle, Group group) {

        List<GroupMember> members = groupMemberRepository
                .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);

        // *** FIX: extract to final local variable so lambdas can capture it ***
        final int cycleNumber = cycle.getCycleNumber();

        List<GroupMember> candidates = members.stream()
                .filter(m -> m.getRotationOrder() != null && m.getRotationOrder() == cycleNumber)
                .collect(Collectors.toList());

        GroupMember winner;
        if (candidates.isEmpty()) {
            Optional<GroupMember> fallback = members.stream()
                    .filter(m -> m.getRotationOrder() != null && m.getRotationOrder() >= cycleNumber)
                    .min(Comparator.comparing(GroupMember::getRotationOrder));
            if (fallback.isEmpty()) {
                throw new BadRequestException("No rotation order >= " + cycleNumber +
                        " available. Ensure sufficient active members with sequential orders.");
            }
            winner = fallback.get();
            log.warn("[FIXED_ROTATION] Fallback winner: order {} for cycle {}", winner.getRotationOrder(), cycleNumber);
        } else {
            winner = candidates.get(0);
        }

        User winnerUser = findUser(winner.getUserId());

        cycle.setWinnerId(winner.getUserId());
        cycle.setWinnerName(winnerUser.getName());
        cycle.setWinnerAmount(cycle.getTotalCollected());
        cycle.setStatus(PayoutCycle.CycleStatus.COMPLETED);
        cycle.setCompletedAt(LocalDateTime.now());

        PayoutCycle savedCycle = payoutCycleRepository.save(cycle);

        markMemberPaidOut(winner, savedCycle.getCycleNumber());
        notifyWinner(winnerUser, group, savedCycle);

        group.setCurrentCycleMonth(group.getCurrentCycleMonth() + 1);
        groupRepository.save(group);

        log.info("[FIXED_ROTATION] Cycle {} winner: {} ({}), group cycle advanced to {}",
                savedCycle.getCycleNumber(), winnerUser.getName(), winner.getUserId(), group.getCurrentCycleMonth());

        return savedCycle;
    }

    private PayoutCycle executeControlledRandom(PayoutCycle cycle, Group group) {

        List<GroupMember> eligible = groupMemberRepository.findEligibleForPayout(group.getId());

        if (eligible.isEmpty())
            throw new BadRequestException("No eligible members for random draw — all have received payouts");

        String seed = codeGenerator.generateSeed();
        int index = Math.abs(seed.hashCode()) % eligible.size();
        GroupMember winner = eligible.get(index);
        User winnerUser = findUser(winner.getUserId());

        List<String> eligibleIds = eligible.stream()
                .map(GroupMember::getUserId).collect(Collectors.toList());

        PayoutCycle.RandomDetails randomDetails = PayoutCycle.RandomDetails.builder()
                .eligibleMemberIds(eligibleIds)
                .selectionSeed(seed)
                .build();

        cycle.setWinnerId(winner.getUserId());
        cycle.setWinnerName(winnerUser.getName());
        cycle.setWinnerAmount(cycle.getTotalCollected());
        cycle.setStatus(PayoutCycle.CycleStatus.COMPLETED);
        cycle.setCompletedAt(LocalDateTime.now());
        cycle.setRandomDetails(randomDetails);

        PayoutCycle savedCycle = payoutCycleRepository.save(cycle);

        markMemberPaidOut(winner, savedCycle.getCycleNumber());
        notifyWinner(winnerUser, group, savedCycle);

        log.info("[CONTROLLED_RANDOM] Cycle {} winner: {} (seed: {})",
                savedCycle.getCycleNumber(), winnerUser.getName(), seed);

        return savedCycle;
    }

    public BidResponse placeBid(PlaceBidRequest req, String userId) {

        Group group = findGroup(req.getGroupId());

        if (group.getPayoutMethod() != Group.PayoutMethod.BIDDING)
            throw new BadRequestException("This group does not use the bidding payout method");

        PayoutCycle cycle = payoutCycleRepository.findById(req.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Payout cycle not found"));

        if (cycle.getStatus() != PayoutCycle.CycleStatus.IN_PROGRESS)
            throw new BadRequestException("Bidding is not open for this cycle");

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(req.getGroupId(), userId)
                .orElseThrow(() -> new BadRequestException("You are not an active member of this group"));

        if (member.isHasReceivedPayout())
            throw new BadRequestException("You have already received a payout and cannot bid again");

        if (bidRepository.existsByCycleIdAndUserId(req.getCycleId(), userId))
            throw new ConflictException("You have already placed a bid for this cycle");

        if (req.getBidAmount() >= cycle.getTotalCollected())
            throw new BadRequestException("Bid amount must be less than the total pot of ₹" + cycle.getTotalCollected());

        if (req.getBidAmount() <= 0)
            throw new BadRequestException("Bid amount must be greater than zero");

        Bid bid = Bid.builder()
                .groupId(req.getGroupId())
                .cycleId(req.getCycleId())
                .userId(userId)
                .bidAmount(req.getBidAmount())
                .status(Bid.BidStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        bid = bidRepository.save(bid);

        User user = findUser(userId);
        log.info("[BIDDING] Bid placed by {} — ₹{} for cycle {}", user.getName(), req.getBidAmount(), cycle.getCycleNumber());

        return toBidResponse(bid, user);
    }

    public PayoutCycleResponse executeBiddingPayout(ExecutePayoutRequest req, String adminId) {

        PayoutCycle cycle = payoutCycleRepository.findById(req.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Payout cycle not found"));

        Group group = findGroup(cycle.getGroupId());
        validateAdmin(adminId, group);

        if (cycle.getStatus() != PayoutCycle.CycleStatus.IN_PROGRESS)
            throw new BadRequestException("This cycle is not in progress");

        if (group.getPayoutMethod() != Group.PayoutMethod.BIDDING)
            throw new BadRequestException("This group does not use the bidding payout method");

        List<Bid> allBids = bidRepository.findByCycleIdOrderByBidAmountAsc(req.getCycleId());

        if (allBids.isEmpty())
            throw new BadRequestException("No bids received for this cycle. Cannot execute payout.");

        Bid winningBid = allBids.get(0);
        if (req.getOverrideWinnerId() != null) {
            winningBid = allBids.stream()
                    .filter(b -> b.getUserId().equals(req.getOverrideWinnerId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Override winner has not placed a bid"));
        }

        final String winnerId        = winningBid.getUserId();
        final double lowestBid       = winningBid.getBidAmount();
        final double discount        = cycle.getTotalCollected() - lowestBid;
        final long   otherMemberCount = groupMemberRepository
                .countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE) - 1;
        final double distributedEach = otherMemberCount > 0 ? discount / otherMemberCount : 0;

        User winnerUser = findUser(winnerId);

        allBids.forEach(b -> {
            b.setStatus(b.getUserId().equals(winnerId) ? Bid.BidStatus.WON : Bid.BidStatus.LOST);
            bidRepository.save(b);
        });

        PayoutCycle.BiddingDetails biddingDetails = PayoutCycle.BiddingDetails.builder()
                .lowestBid(lowestBid)
                .discount(discount)
                .distributedPerMember(distributedEach)
                .build();

        cycle.setWinnerId(winnerId);
        cycle.setWinnerName(winnerUser.getName());
        cycle.setWinnerAmount(lowestBid);
        cycle.setStatus(PayoutCycle.CycleStatus.COMPLETED);
        cycle.setCompletedAt(LocalDateTime.now());
        cycle.setBiddingDetails(biddingDetails);

        final PayoutCycle savedCycle = payoutCycleRepository.save(cycle);

        groupMemberRepository.findByGroupIdAndUserId(group.getId(), winnerId).ifPresent(m -> {
            markMemberPaidOut(m, savedCycle.getCycleNumber());
        });

        notifyWinner(winnerUser, group, savedCycle);
        notifyBiddingDiscount(group, savedCycle, distributedEach);

        audit(adminId, "GROUP_ADMIN", "PAYOUT_EXECUTED_BIDDING", "PAYOUT_CYCLE",
                savedCycle.getId(), group.getId(),
                Map.of("winner", winnerUser.getName(), "winnerAmount", lowestBid,
                        "discount", discount, "distributedEach", distributedEach));

        log.info("[BIDDING] Cycle {} winner: {} ₹{} | discount ₹{} split to {} members",
                savedCycle.getCycleNumber(), winnerUser.getName(), lowestBid, discount, otherMemberCount);

        return toCycleResponse(savedCycle, group);
    }

    public PayoutHistoryResponse getPayoutHistory(String groupId) {
        Group group = findGroup(groupId);
        List<PayoutCycle> cycles = payoutCycleRepository.findByGroupIdOrderByCycleNumberDesc(groupId);

        long completed = cycles.stream().filter(c -> c.getStatus() == PayoutCycle.CycleStatus.COMPLETED).count();
        int remaining  = group.getMaxMembers() - (int) completed;

        List<PayoutCycleResponse> responses = cycles.stream()
                .map(c -> toCycleResponse(c, group))
                .collect(Collectors.toList());

        return PayoutHistoryResponse.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalCycles(group.getMaxMembers())
                .completedCycles((int) completed)
                .remainingCycles(remaining)
                .cycles(responses)
                .build();
    }

    public PayoutCycleResponse getCurrentCycle(String groupId) {
        Group group = findGroup(groupId);
        return payoutCycleRepository
                .findByGroupIdAndCycleNumber(groupId, group.getCurrentCycleMonth())
                .map(c -> toCycleResponse(c, group))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active cycle found for group. Admin must initiate payout."));
    }

    public BiddingStatusResponse getBiddingStatus(String cycleId, String requesterId) {

        PayoutCycle cycle = payoutCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));

        Group group = findGroup(cycle.getGroupId());
        User requester = findUser(requesterId);

        List<Bid> allBids = bidRepository.findByCycleIdOrderByBidAmountAsc(cycleId);

        boolean isAdmin    = group.getAdminId().equals(requesterId) || requester.getRole() == User.Role.SUPER_ADMIN;
        boolean userHasBid = bidRepository.existsByCycleIdAndUserId(cycleId, requesterId);
        final String finalRequesterId = requesterId;
        Double userBidAmt = allBids.stream()
                .filter(b -> b.getUserId().equals(finalRequesterId))
                .map(Bid::getBidAmount).findFirst().orElse(null);

        List<BidResponse> visibleBids = isAdmin
                ? allBids.stream().map(b -> toBidResponse(b, findUser(b.getUserId()))).collect(Collectors.toList())
                : allBids.stream().filter(b -> b.getUserId().equals(finalRequesterId))
                         .map(b -> toBidResponse(b, findUser(b.getUserId()))).collect(Collectors.toList());

        return BiddingStatusResponse.builder()
                .cycleId(cycleId)
                .groupId(cycle.getGroupId())
                .cycleNumber(cycle.getCycleNumber())
                .status(cycle.getStatus().name())
                .totalBids(allBids.size())
                .userHasBid(userHasBid)
                .userBidAmount(userBidAmt)
                .bids(visibleBids)
                .build();
    }

    private void markMemberPaidOut(GroupMember member, int cycleNumber) {
        member.setHasReceivedPayout(true);
        member.setPayoutReceivedOnCycle(cycleNumber);
        groupMemberRepository.save(member);
    }

    private Group findGroup(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void validateAdmin(String userId, Group group) {
        User user = findUser(userId);
        if (!group.getAdminId().equals(userId) && user.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("Only the group admin can manage payouts");
    }

    private void audit(String actorId, String role, String action,
                       String targetType, String targetId, String groupId, Map<String, Object> details) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId).actorRole(role).action(action)
                .targetType(targetType).targetId(targetId)
                .groupId(groupId).details(details).build());
    }

    private void notifyWinner(User winner, Group group, PayoutCycle cycle) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(winner.getEmail());
            msg.setSubject("🎉 You won the Bhishi payout! — " + group.getName());
            msg.setText(
                "Hello " + winner.getName() + ",\n\n" +
                "Congratulations! You have been selected to receive the payout for Cycle " +
                cycle.getCycleNumber() + " of \"" + group.getName() + "\".\n\n" +
                "Payout Amount: ₹" + cycle.getWinnerAmount() + "\n" +
                "Method: " + cycle.getPayoutMethod() + "\n\n" +
                "Your group admin will process the transfer shortly.\n\n" +
                "— Bhishi Platform"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send winner notification: {}", e.getMessage());
        }
    }

    private void notifyBiddingDiscount(Group group, PayoutCycle cycle, double distributedEach) {
        if (distributedEach <= 0) return;
        List<GroupMember> members = groupMemberRepository
                .findByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
        members.stream()
                .filter(m -> !m.getUserId().equals(cycle.getWinnerId()))
                .forEach(m -> {
                    try {
                        User u = findUser(m.getUserId());
                        SimpleMailMessage msg = new SimpleMailMessage();
                        msg.setTo(u.getEmail());
                        msg.setSubject("Bhishi Bidding Discount — " + group.getName());
                        msg.setText(
                            "Hello " + u.getName() + ",\n\n" +
                            "The bidding payout for Cycle " + cycle.getCycleNumber() +
                            " has been completed.\n\n" +
                            "Since the winning bid was lower than the full pot, " +
                            "each non-winning member receives a discount of ₹" + distributedEach +
                            " off their next contribution.\n\n" +
                            "— Bhishi Platform"
                        );
                        mailSender.send(msg);
                    } catch (Exception e) {
                        log.warn("Could not send discount notification to member {}", m.getUserId());
                    }
                });
    }

    private PayoutCycleResponse toCycleResponse(PayoutCycle c, Group group) {
        PayoutCycleResponse.PayoutCycleResponseBuilder builder = PayoutCycleResponse.builder()
                .id(c.getId())
                .groupId(c.getGroupId())
                .groupName(group.getName())
                .cycleNumber(c.getCycleNumber())
                .cycleMonth(c.getCycleMonth())
                .cycleYear(c.getCycleYear())
                .payoutMethod(c.getPayoutMethod())
                .totalCollected(c.getTotalCollected())
                .winnerId(c.getWinnerId())
                .winnerName(c.getWinnerName())
                .winnerAmount(c.getWinnerAmount())
                .status(c.getStatus().name())
                .completedAt(c.getCompletedAt())
                .createdAt(c.getCreatedAt());

        if (c.getBiddingDetails() != null) {
            builder.lowestBid(c.getBiddingDetails().getLowestBid())
                   .discount(c.getBiddingDetails().getDiscount())
                   .distributedPerMember(c.getBiddingDetails().getDistributedPerMember());
        }
        if (c.getRandomDetails() != null) {
            builder.eligibleMemberIds(c.getRandomDetails().getEligibleMemberIds())
                   .selectionSeed(c.getRandomDetails().getSelectionSeed());
        }
        return builder.build();
    }

    private BidResponse toBidResponse(Bid b, User user) {
        return BidResponse.builder()
                .id(b.getId())
                .cycleId(b.getCycleId())
                .userId(b.getUserId())
                .memberName(user.getName())
                .bidAmount(b.getBidAmount())
                .status(b.getStatus().name())
                .submittedAt(b.getSubmittedAt())
                .build();
    }
}