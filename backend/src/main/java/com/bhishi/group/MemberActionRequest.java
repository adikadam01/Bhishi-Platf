package com.bhishi.group;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class MemberActionRequest {
    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotNull(message = "Action is required")
    private MemberAction action;

    private String reason;

    public enum MemberAction { APPROVE, REJECT }
}
