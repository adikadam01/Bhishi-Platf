package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "payout_cycles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayoutCycle {
    @Id private String id;
    private String groupId;
    private int cycleNumber;
    private int cycleMonth;
    private int cycleYear;
    private String payoutMethod;
    private double totalCollected;
    private String winnerId;
    private String winnerName;
    private double winnerAmount;
    private CycleStatus status;
    private BiddingDetails biddingDetails;
    private RandomDetails randomDetails;
    private LocalDateTime completedAt;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public enum CycleStatus { PENDING, IN_PROGRESS, COMPLETED }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BiddingDetails {
        private double lowestBid;
        private double discount;
        private double distributedPerMember;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RandomDetails {
        private List<String> eligibleMemberIds;
        private String selectionSeed;
    }
}
