package com.bhishi.urgency;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RaiseUrgencyRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;

    @NotNull(message = "Voting hours is required")
    @Min(value = 1,  message = "Minimum 1 hour voting window")
    @Max(value = 72, message = "Maximum 72 hours voting window")
    private Integer votingHours;
}
