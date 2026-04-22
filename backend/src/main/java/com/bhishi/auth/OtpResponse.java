package com.bhishi.auth;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OtpResponse {
    private String message;
    private String maskedPhone;
    private int expiresInMinutes;
}
