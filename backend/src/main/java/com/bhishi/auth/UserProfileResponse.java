package com.bhishi.auth;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private LocalDate dob;
    private String role;
    private String status;
    private String profilePhotoUrl;
    private LocalDateTime createdAt;
}
