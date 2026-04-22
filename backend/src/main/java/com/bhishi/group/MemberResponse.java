package com.bhishi.group;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberResponse {
    private String id;
    private String userId;
    private String name;
    private String phone;
    private String email;
    private String status;
    private Integer rotationOrder;
    private boolean hasReceivedPayout;
    private Integer payoutReceivedOnCycle;
    private LocalDateTime joinRequestedAt;
    private LocalDateTime joinApprovedAt;
}
