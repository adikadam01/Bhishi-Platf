package com.bhishi.payout;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExecutePayoutRequest {
    @NotBlank(message = "Cycle ID is required")
    private String cycleId;

    private String overrideWinnerId;
}
