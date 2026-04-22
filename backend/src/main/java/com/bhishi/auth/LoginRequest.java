package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[0-9]{10}$", message = "Phone must be in format +91XXXXXXXXXX")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}
