package com.bhishi.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterStep2Request {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Last 4 digits of Aadhaar are required")
    @Pattern(regexp = "[0-9]{4}", message = "Must be exactly 4 digits")
    private String aadhaarLastFour;

    private String profilePhotoBase64;
}
