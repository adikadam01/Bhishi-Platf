package com.bhishi.group;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RemoveMemberRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
    @NotBlank(message = "Reason is required")
    private String reason;
}
