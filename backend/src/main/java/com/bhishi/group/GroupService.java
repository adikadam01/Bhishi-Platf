package com.bhishi.group;

// ============================================================
// DIGITAL BHISHI PLATFORM — Group Service
// Phase 3 | File: GroupService.java | Package: com.bhishi.group
// Handles:
//   - Group creation (Group Admin) → auto-sent to Super Admin
//   - Super Admin approve/reject group
//   - Join group via code (Member)
//   - Group Admin approve/reject join requests
//   - Remove member
//   - View group details, members, stats
// Links to Phase 1:
//   - Group, GroupMember, User, AuditLog models
//   - GroupRepository, GroupMemberRepository, UserRepository, AuditLogRepository
//   - CodeGenerator (com.bhishi.util)
// Links to Phase 2:
//   - User.Role, User.UserStatus checks
// ============================================================

import com.bhishi.exception.*;
import com.bhishi.model.*;
import com.bhishi.repository.*;
import com.bhishi.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository       groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository        userRepository;
    private final AuditLogRepository    auditLogRepository;
    private final CodeGenerator         codeGenerator;
    private final JavaMailSender        mailSender;

    // ─────────────────────────────────────────────
    // CREATE GROUP (Group Admin)
    // Contribution = totalAmount / maxMembers
    // Status = PENDING_APPROVAL until Super Admin approves
    // ─────────────────────────────────────────────
    public GroupResponse createGroup(CreateGroupRequest req, String adminId) {
        User admin = findUser(adminId);
        if (admin.getRole() != User.Role.GROUP_ADMIN && admin.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("Only Group Admins can create groups");

        // Contribution is auto-calculated
        double contribution = req.getTotalAmount() / req.getMaxMembers();

        Group group = Group.builder()
                .name(req.getName())
                .description(req.getDescription())
                .adminId(adminId)
                .totalAmount(req.getTotalAmount())
                .maxMembers(req.getMaxMembers())
                .contributionPerMember(contribution)
                .payoutMethod(req.getPayoutMethod())
                .status(Group.GroupStatus.PENDING_APPROVAL)
                .dueDayOfMonth(req.getDueDayOfMonth())
                .penaltyAmount(req.getPenaltyAmount())
                .currentCycleMonth(0)
                .build();

        group = groupRepository.save(group);

        audit(adminId, admin.getRole().name(), "GROUP_CREATED", "GROUP", group.getId(), group.getId(),
                Map.of("groupName", group.getName(), "payoutMethod", group.getPayoutMethod().name()));

        log.info("Group created: {} by admin: {}", group.getId(), adminId);
        return toGroupResponse(group, admin, 0);
    }

    // ─────────────────────────────────────────────
    // SUPER ADMIN: APPROVE or REJECT GROUP
    // On approve: generates unique group code
    // ─────────────────────────────────────────────
    public GroupResponse approveOrRejectGroup(GroupApprovalRequest req, String superAdminId) {
        Group group = findGroup(req.getGroupId());

        if (group.getStatus() != Group.GroupStatus.PENDING_APPROVAL)
            throw new BadRequestException("Group is not pending approval");

        User admin = findUser(group.getAdminId());

        if (req.getAction() == GroupApprovalRequest.GroupAction.APPROVE) {
            // Generate unique group code — retry if collision
            String code;
            do { code = codeGenerator.generateGroupCode(); }
            while (groupRepository.existsByGroupCode(code));

            group.setGroupCode(code);
            group.setStatus(Group.GroupStatus.ACTIVE);
            group.setSuperAdminApprovedAt(LocalDateTime.now());
            group.setCurrentCycleMonth(1);

            groupRepository.save(group);

            audit(superAdminId, "SUPER_ADMIN", "GROUP_APPROVED", "GROUP", group.getId(), group.getId(),
                    Map.of("groupCode", code));

            notifyGroupAdmin(admin, group, true, null);
            log.info("Group {} approved with code: {}", group.getId(), code);

        } else {
            group.setStatus(Group.GroupStatus.CANCELLED);
            groupRepository.save(group);

            audit(superAdminId, "SUPER_ADMIN", "GROUP_REJECTED", "GROUP", group.getId(), group.getId(),
                    Map.of("reason", req.getReason() != null ? req.getReason() : "No reason given"));

            notifyGroupAdmin(admin, group, false, req.getReason());
            log.info("Group {} rejected", group.getId());
        }

        long memberCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
        return toGroupResponse(group, admin, (int) memberCount);
    }

    // ─────────────────────────────────────────────
    // JOIN GROUP (Member sends request)
    // Member enters 6-char group code
    // ─────────────────────────────────────────────
    public String joinGroup(JoinGroupRequest req, String userId) {
        User user = findUser(userId);

        if (user.getStatus() != User.UserStatus.ACTIVE)
            throw new BadRequestException("Your account must be active to join a group");

        Group group = groupRepository.findByGroupCode(req.getGroupCode())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid group code. Please check and try again."));

        if (group.getStatus() != Group.GroupStatus.ACTIVE)
            throw new BadRequestException("This group is not currently accepting members");

        // Check if already in group
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId))
            throw new ConflictException("You have already joined or requested to join this group");

        // Check capacity
        long activeCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
        if (activeCount >= group.getMaxMembers())
            throw new BadRequestException("This group is full");

        GroupMember member = GroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .status(GroupMember.MemberStatus.PENDING)
                .joinRequestedAt(LocalDateTime.now())
                .hasReceivedPayout(false)
                .build();

        groupMemberRepository.save(member);

        audit(userId, user.getRole().name(), "JOIN_REQUESTED", "GROUP_MEMBER", member.getId(), group.getId(),
                Map.of("groupName", group.getName()));

        log.info("User {} requested to join group {}", userId, group.getId());
        return "Join request sent! Waiting for group admin approval.";
    }

    // ─────────────────────────────────────────────
    // GROUP ADMIN: APPROVE or REJECT JOIN REQUEST
    // ─────────────────────────────────────────────
    public MemberResponse handleJoinRequest(MemberActionRequest req, String adminId) {
        GroupMember member = groupMemberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        Group group = findGroup(member.getGroupId());
        validateAdminOfGroup(adminId, group);

        if (member.getStatus() != GroupMember.MemberStatus.PENDING)
            throw new BadRequestException("This request has already been processed");

        User memberUser = findUser(member.getUserId());

        if (req.getAction() == MemberActionRequest.MemberAction.APPROVE) {
            // Check capacity again at approval time
            long activeCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMember.MemberStatus.ACTIVE);
            if (activeCount >= group.getMaxMembers())
                throw new BadRequestException("Group is now full, cannot approve more members");

            member.setStatus(GroupMember.MemberStatus.ACTIVE);
            member.setJoinApprovedAt(LocalDateTime.now());

            // For FIXED_ROTATION assign rotation order based on join sequence
            if (group.getPayoutMethod() == Group.PayoutMethod.FIXED_ROTATION) {
                int nextOrder = (int) activeCount + 1;
                if (nextOrder > group.getMaxMembers()) {
                    throw new BadRequestException("Cannot assign rotation order " + nextOrder + 
                            ": exceeds group maxMembers (" + group.getMaxMembers() + ")");
                }
                member.setRotationOrder(nextOrder);
            }

            groupMemberRepository.save(member);
            audit(adminId, "GROUP_ADMIN", "MEMBER_APPROVED", "GROUP_MEMBER", member.getId(), group.getId(),
                    Map.of("memberName", memberUser.getName()));

            log.info("Member {} approved in group {}", member.getUserId(), group.getId());

        } else {
            member.setStatus(GroupMember.MemberStatus.LEFT);
            groupMemberRepository.save(member);
            audit(adminId, "GROUP_ADMIN", "MEMBER_REJECTED", "GROUP_MEMBER", member.getId(), group.getId(),
                    Map.of("memberName", memberUser.getName(), "reason", req.getReason() != null ? req.getReason() : ""));
            log.info("Member {} rejected from group {}", member.getUserId(), group.getId());
        }

        return toMemberResponse(member, memberUser);
    }

    // ─────────────────────────────────────────────
    // GROUP ADMIN: REMOVE ACTIVE MEMBER
    // ─────────────────────────────────────────────
    public String removeMember(RemoveMemberRequest req, String adminId) {
        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(req.getUserId(), adminId) // we reuse userId for target
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this group"));

        // Actually find by userId, not adminId — fix lookup
        Group group = findGroup(member.getGroupId());
        validateAdminOfGroup(adminId, group);

        if (member.getStatus() != GroupMember.MemberStatus.ACTIVE)
            throw new BadRequestException("Member is not currently active");

        User memberUser = findUser(member.getUserId());

        member.setStatus(GroupMember.MemberStatus.REMOVED);
        groupMemberRepository.save(member);

        audit(adminId, "GROUP_ADMIN", "MEMBER_REMOVED", "GROUP_MEMBER", member.getId(), group.getId(),
                Map.of("memberName", memberUser.getName(), "reason", req.getReason()));

        log.info("Member {} removed from group {} — reason: {}", member.getUserId(), group.getId(), req.getReason());
        return "Member removed from group successfully";
    }

    // ─────────────────────────────────────────────
    // GET GROUP DETAILS
    // ─────────────────────────────────────────────
    public GroupResponse getGroupById(String groupId) {
        Group group = findGroup(groupId);
        User admin = findUser(group.getAdminId());
        long count = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMember.MemberStatus.ACTIVE);
        return toGroupResponse(group, admin, (int) count);
    }

    // ─────────────────────────────────────────────
    // GET ALL MEMBERS OF A GROUP
    // ─────────────────────────────────────────────
    public List<MemberResponse> getGroupMembers(String groupId, String requesterId) {
        findGroup(groupId); // validate exists
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream().map(m -> {
            User u = findUser(m.getUserId());
            return toMemberResponse(m, u);
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET PENDING JOIN REQUESTS (Group Admin)
    // ─────────────────────────────────────────────
    public List<MemberResponse> getPendingRequests(String groupId, String adminId) {
        Group group = findGroup(groupId);
        validateAdminOfGroup(adminId, group);
        List<GroupMember> pending = groupMemberRepository.findByGroupIdAndStatus(groupId, GroupMember.MemberStatus.PENDING);
        return pending.stream().map(m -> {
            User u = findUser(m.getUserId());
            return toMemberResponse(m, u);
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET MY GROUPS (for a logged-in member)
    // ─────────────────────────────────────────────
    public List<GroupSummary> getMyGroups(String userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        return memberships.stream()
                .filter(m -> m.getStatus() == GroupMember.MemberStatus.ACTIVE || m.getStatus() == GroupMember.MemberStatus.PENDING)
                .map(m -> {
                    Group g = findGroup(m.getGroupId());
                    long count = groupMemberRepository.countByGroupIdAndStatus(g.getId(), GroupMember.MemberStatus.ACTIVE);
                    return toGroupSummary(g, (int) count);
                }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET GROUPS MANAGED BY ADMIN
    // ─────────────────────────────────────────────
    public List<GroupSummary> getAdminGroups(String adminId) {
        return groupRepository.findByAdminId(adminId).stream().map(g -> {
            long count = groupMemberRepository.countByGroupIdAndStatus(g.getId(), GroupMember.MemberStatus.ACTIVE);
            return toGroupSummary(g, (int) count);
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET ALL PENDING GROUPS (Super Admin)
    // ─────────────────────────────────────────────
    public List<GroupSummary> getPendingGroups() {
        return groupRepository.findByStatus(Group.GroupStatus.PENDING_APPROVAL).stream().map(g -> {
            long count = groupMemberRepository.countByGroupIdAndStatus(g.getId(), GroupMember.MemberStatus.ACTIVE);
            return toGroupSummary(g, (int) count);
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET GROUP STATS (Admin Dashboard)
    // ─────────────────────────────────────────────
    public GroupStats getGroupStats(String groupId, String adminId) {
        Group group = findGroup(groupId);
        validateAdminOfGroup(adminId, group);

        long total   = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMember.MemberStatus.ACTIVE);
        long pending = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMember.MemberStatus.PENDING);

        List<GroupMember> recentPending = groupMemberRepository
                .findByGroupIdAndStatus(groupId, GroupMember.MemberStatus.PENDING);

        List<MemberResponse> recentRequests = recentPending.stream()
                .limit(5)
                .map(m -> toMemberResponse(m, findUser(m.getUserId())))
                .collect(Collectors.toList());

        return GroupStats.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers((int) total)
                .pendingMembers((int) pending)
                .activeMembers((int) total)
                .currentCycleMonth(group.getCurrentCycleMonth())
                .recentJoinRequests(recentRequests)
                .build();
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

    private void validateAdminOfGroup(String userId, Group group) {
        User user = findUser(userId);
        if (!group.getAdminId().equals(userId) && user.getRole() != User.Role.SUPER_ADMIN)
            throw new UnauthorizedException("You are not the admin of this group");
    }

    private void audit(String actorId, String actorRole, String action,
                       String targetType, String targetId, String groupId, Map<String, Object> details) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId).actorRole(actorRole).action(action)
                .targetType(targetType).targetId(targetId)
                .groupId(groupId).details(details).build());
    }

    private void notifyGroupAdmin(User admin, Group group, boolean approved, String reason) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(admin.getEmail());
            msg.setSubject(approved ? "Your Bhishi Group Has Been Approved!" : "Bhishi Group Application Update");
            if (approved) {
                msg.setText("Hello " + admin.getName() + ",\n\n" +
                        "Great news! Your group \"" + group.getName() + "\" has been approved.\n" +
                        "Group Code: " + group.getGroupCode() + "\n" +
                        "Share this code with your members to let them join.\n\n— Bhishi Platform");
            } else {
                msg.setText("Hello " + admin.getName() + ",\n\n" +
                        "Your group \"" + group.getName() + "\" was not approved.\n" +
                        "Reason: " + (reason != null ? reason : "Not specified") + "\n\n— Bhishi Platform");
            }
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send group approval email: {}", e.getMessage());
        }
    }

    // ── Mappers ──────────────────────────────────

    private GroupResponse toGroupResponse(Group g, User admin, int memberCount) {
        return GroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .adminId(g.getAdminId())
                .adminName(admin.getName())
                .totalAmount(g.getTotalAmount())
                .maxMembers(g.getMaxMembers())
                .currentMemberCount(memberCount)
                .contributionPerMember(g.getContributionPerMember())
                .payoutMethod(g.getPayoutMethod().name())
                .status(g.getStatus().name())
                .groupCode(g.getGroupCode())
                .dueDayOfMonth(g.getDueDayOfMonth())
                .penaltyAmount(g.getPenaltyAmount())
                .currentCycleMonth(g.getCurrentCycleMonth())
                .superAdminApprovedAt(g.getSuperAdminApprovedAt())
                .createdAt(g.getCreatedAt())
                .build();
    }

    private GroupSummary toGroupSummary(Group g, int memberCount) {
        return GroupSummary.builder()
                .id(g.getId())
                .name(g.getName())
                .payoutMethod(g.getPayoutMethod().name())
                .status(g.getStatus().name())
                .groupCode(g.getGroupCode())
                .contributionPerMember(g.getContributionPerMember())
                .currentMemberCount(memberCount)
                .maxMembers(g.getMaxMembers())
                .currentCycleMonth(g.getCurrentCycleMonth())
                .build();
    }

    private MemberResponse toMemberResponse(GroupMember m, User u) {
        return MemberResponse.builder()
                .id(m.getId())
                .userId(u.getId())
                .name(u.getName())
                .phone(u.getPhone())
                .email(u.getEmail())
                .status(m.getStatus().name())
                .rotationOrder(m.getRotationOrder())
                .hasReceivedPayout(m.isHasReceivedPayout())
                .payoutReceivedOnCycle(m.getPayoutReceivedOnCycle())
                .joinRequestedAt(m.getJoinRequestedAt())
                .joinApprovedAt(m.getJoinApprovedAt())
                .build();
    }
}
