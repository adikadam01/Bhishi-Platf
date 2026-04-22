package com.bhishi.urgency;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CastVoteRequest {

    @NotBlank(message = "Urgency request ID is required")
    private String urgencyRequestId;

    @NotNull(message = "Vote is required")
    private VoteChoiceDto vote;

    public enum VoteChoiceDto { FOR, AGAINST, ABSTAIN }
}
