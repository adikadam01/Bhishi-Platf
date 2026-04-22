package com.bhishi.group;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class JoinGroupRequest {
    @NotBlank(message = "Group code is required")
    @Size(min = 6, max = 6, message = "Group code must be exactly 6 characters")
    private String groupCode;
}
