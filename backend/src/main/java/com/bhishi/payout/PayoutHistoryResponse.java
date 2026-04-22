package com.bhishi.payout;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayoutHistoryResponse {
    private String               groupId;
    private String               groupName;
    private int                  totalCycles;
    private int                  completedCycles;
    private int                  remainingCycles;
    private List<PayoutCycleResponse> cycles;
}
