package com.bhishi.payout;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class InitiatePayoutRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Cycle number is required")
    @Min(value = 1, message = "Cycle number must be at least 1")
    private Integer cycleNumber;

    @NotNull(message = "Cycle month is required")
    @Min(value = 1) @Max(value = 12)
    private Integer cycleMonth;

    @NotNull(message = "Cycle year is required")
    private Integer cycleYear;
}
