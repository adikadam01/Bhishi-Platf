package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "groups")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Group {
    @Id private String id;
    private String name;
    private String description;
    private String adminId;
    private double totalAmount;
    private int maxMembers;
    private double contributionPerMember;
    private PayoutMethod payoutMethod;
    private GroupStatus status;
    @Indexed(unique = true, sparse = true)
    private String groupCode;
    private int dueDayOfMonth;
    private double penaltyAmount;
    private int currentCycleMonth;
    private LocalDateTime superAdminApprovedAt;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public enum PayoutMethod { FIXED_ROTATION, BIDDING, CONTROLLED_RANDOM }
    public enum GroupStatus  { PENDING_APPROVAL, ACTIVE, COMPLETED, CANCELLED }
}
