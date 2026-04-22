package com.bhishi.group;

import com.bhishi.model.Group;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 100, message = "Group name must be 3–100 characters")
    private String name;

    @Size(max = 300, message = "Description max 300 characters")
    private String description;


    @NotNull(message = "Total amount is required")
    @Min(value = 1000, message = "Minimum total amount is ₹1000")
    private Double totalAmount;

    @NotNull(message = "Number of members is required")
    @Min(value = 2,  message = "Minimum 2 members")
    @Max(value = 50, message = "Maximum 50 members")
    private Integer maxMembers;

    @NotNull(message = "Payout method is required")
    private Group.PayoutMethod payoutMethod;

    @NotNull(message = "Due day of month is required")
    @Min(value = 1,  message = "Due day must be between 1 and 28")
    @Max(value = 28, message = "Due day must be between 1 and 28")
    private Integer dueDayOfMonth;

    @NotNull(message = "Penalty amount is required")
    @Min(value = 0, message = "Penalty cannot be negative")
    private Double penaltyAmount;
}
