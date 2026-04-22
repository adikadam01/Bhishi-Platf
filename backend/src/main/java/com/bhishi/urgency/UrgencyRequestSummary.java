package com.bhishi.urgency;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UrgencyRequestSummary {
    private String  id;
    private String  requestedByName;
    private String  reason;
    private String  status;
    private int     votesFor;
    private int     votesAgainst;
    private int     totalMembers;
    private boolean majorityReached;
    private LocalDateTime votingDeadline;
    private LocalDateTime createdAt;
}
