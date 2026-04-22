package com.bhishi.group;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupSummary {
    private String id;
    private String name;
    private String payoutMethod;
    private String status;
    private String groupCode;
    private double contributionPerMember;
    private int    currentMemberCount;
    private int    maxMembers;
    private int    currentCycleMonth;
}
