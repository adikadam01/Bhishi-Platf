package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SendOtpRequest {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[0-9]{10}$", message = "Phone must be in format +91XXXXXXXXXX")
    private String phone;

    @NotNull(message = "Purpose is required")
    private OtpPurposeDto purpose;

    public enum OtpPurposeDto { REGISTRATION, LOGIN, RESET }
}
