package com.bhishi.payment;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class WaivePenaltyRequest {
    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
