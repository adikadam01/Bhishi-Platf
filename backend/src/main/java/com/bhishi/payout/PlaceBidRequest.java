package com.bhishi.payout;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlaceBidRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Cycle ID is required")
    private String cycleId;

    @NotNull(message = "Bid amount is required")
    @Min(value = 1, message = "Bid amount must be positive")
    private Double bidAmount;
}
