package com.bhishi.auth;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterStep1Response {
    private String userId;
    private String message;
    private String otpSentTo;
}
