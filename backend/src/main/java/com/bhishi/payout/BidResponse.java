package com.bhishi.payout;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BidResponse {
    private String  id;
    private String  cycleId;
    private String  userId;
    private String  memberName;
    private double  bidAmount;
    private String  status;
    private LocalDateTime submittedAt;
}
