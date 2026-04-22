package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PasswordLoginRequest {
    @NotBlank(message = "Phone or email is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
