package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_tokens")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OtpToken {
    @Id private String id;
    private String phone;
    private String otp;
    private OtpPurpose purpose;
    private LocalDateTime expiresAt;
    private boolean used;
    @CreatedDate private LocalDateTime createdAt;

    public enum OtpPurpose { REGISTRATION, LOGIN, RESET }
}
