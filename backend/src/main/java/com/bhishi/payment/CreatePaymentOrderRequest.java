package com.bhishi.payment;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Cycle month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer cycleMonth;

    @NotNull(message = "Cycle year is required")
    private Integer cycleYear;
}
