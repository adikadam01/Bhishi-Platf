package com.bhishi.group;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class GroupApprovalRequest {
    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Action is required")
    private GroupAction action;

    private String reason;

    public enum GroupAction { APPROVE, REJECT }
}
