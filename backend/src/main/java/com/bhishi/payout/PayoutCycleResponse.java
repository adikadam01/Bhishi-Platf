package com.bhishi.payout;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayoutCycleResponse {
    private String  id;
    private String  groupId;
    private String  groupName;
    private int     cycleNumber;
    private int     cycleMonth;
    private int     cycleYear;
    private String  payoutMethod;
    private double  totalCollected;
    private String  winnerId;
    private String  winnerName;
    private double  winnerAmount;
    private String  status;

    private Double  lowestBid;
    private Double  discount;
    private Double  distributedPerMember;

    private List<String> eligibleMemberIds;
    private String       selectionSeed;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
