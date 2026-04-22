package com.bhishi.urgency;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ResolveUrgencyRequest {
    @NotBlank(message = "Urgency request ID is required")
    private String urgencyRequestId;

    @NotNull(message = "Resolution is required")
    private ResolutionAction action;

    private String reason;

    public enum ResolutionAction { APPROVE, REJECT }
}
