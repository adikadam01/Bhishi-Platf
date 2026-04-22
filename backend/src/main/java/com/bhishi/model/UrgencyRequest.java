package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "urgency_requests")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UrgencyRequest {
    @Id private String id;
    private String groupId;
    private String requestedByUserId;
    private String reason;
    private UrgencyStatus status;
    private int totalMembers;
    private int votesFor;
    private int votesAgainst;
    private int votesAbstained;
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();
    private LocalDateTime votingDeadline;
    private LocalDateTime resolvedAt;
    @CreatedDate private LocalDateTime createdAt;

    public enum UrgencyStatus { PENDING, APPROVED, REJECTED, EXPIRED }
    public enum VoteChoice    { FOR, AGAINST, ABSTAIN }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Vote {
        private String userId;
        private VoteChoice vote;
        private LocalDateTime votedAt;
    }
}
