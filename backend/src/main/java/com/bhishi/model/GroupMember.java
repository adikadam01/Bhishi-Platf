package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "group_members")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMember {
    @Id private String id;
    private String groupId;
    private String userId;
    private MemberStatus status;
    private LocalDateTime joinRequestedAt;
    private LocalDateTime joinApprovedAt;
    private Integer rotationOrder;
    private boolean hasReceivedPayout;
    private Integer payoutReceivedOnCycle;
    @CreatedDate private LocalDateTime createdAt;

    public enum MemberStatus { PENDING, ACTIVE, REMOVED, LEFT }
}
