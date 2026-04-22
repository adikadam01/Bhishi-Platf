package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "bids")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Bid {
    @Id private String id;
    private String groupId;
    private String cycleId;
    private String userId;
    private double bidAmount;
    private BidStatus status;
    private LocalDateTime submittedAt;
    @CreatedDate private LocalDateTime createdAt;

    public enum BidStatus { PENDING, WON, LOST, WITHDRAWN }
}
