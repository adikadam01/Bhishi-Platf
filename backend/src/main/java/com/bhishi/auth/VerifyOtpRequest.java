package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class VerifyOtpRequest {
    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;

    @NotNull(message = "Purpose is required")
    private SendOtpRequest.OtpPurposeDto purpose;
}
