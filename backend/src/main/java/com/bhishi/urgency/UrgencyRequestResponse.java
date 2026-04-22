package com.bhishi.urgency;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UrgencyRequestResponse {
    private String  id;
    private String  groupId;
    private String  groupName;
    private String  requestedByUserId;
    private String  requestedByName;
    private String  reason;
    private String  status;

    private int     totalMembers;
    private int     votesFor;
    private int     votesAgainst;
    private int     votesAbstained;
    private int     votesRemaining;
    private boolean majorityReached;

    private boolean currentUserHasVoted;
    private String  currentUserVote;

    private List<VoteResponse> votes;

    private LocalDateTime votingDeadline;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    private String  payoutCycleId;
    private Double  payoutAmount;
}
