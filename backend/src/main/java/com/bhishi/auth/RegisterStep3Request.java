package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterStep3Request {
    @NotBlank(message = "User ID is required")
    private String userId;

    @AssertTrue(message = "You must accept the terms and conditions")
    private boolean termsAccepted;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
