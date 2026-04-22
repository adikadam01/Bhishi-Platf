package com.bhishi.payout;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BiddingStatusResponse {
    private String          cycleId;
    private String          groupId;
    private int             cycleNumber;
    private String          status;
    private int             totalBids;
    private boolean         userHasBid;
    private Double          userBidAmount;
    private List<BidResponse> bids;
}
