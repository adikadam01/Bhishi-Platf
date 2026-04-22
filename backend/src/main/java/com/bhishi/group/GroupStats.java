package com.bhishi.group;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupStats {
    private String  groupId;
    private String  groupName;
    private int     totalMembers;
    private int     pendingMembers;
    private int     activeMembers;
    private double  totalCollectedThisCycle;
    private int     paidThisCycle;
    private int     pendingThisCycle;
    private int     lateThisCycle;
    private int     currentCycleMonth;
    private int     completedCycles;
    private List<MemberResponse> recentJoinRequests;
}
