package com.bhishi.group;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String adminId;
    private String adminName;
    private double totalAmount;
    private int    maxMembers;
    private int    currentMemberCount;
    private double contributionPerMember;
    private String payoutMethod;
    private String status;
    private String groupCode;
    private int    dueDayOfMonth;
    private double penaltyAmount;
    private int    currentCycleMonth;
    private LocalDateTime superAdminApprovedAt;
    private LocalDateTime createdAt;
}
