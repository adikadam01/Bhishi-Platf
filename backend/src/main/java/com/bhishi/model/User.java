package com.bhishi.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id private String id;
    private String name;
    @Indexed(unique = true) private String email;
    @Indexed(unique = true) private String phone;
    private String passwordHash;
    private LocalDate dob;
    private Role role;
    private UserStatus status;
    private String profilePhotoUrl;
    private String aadhaarLastFour;
    private boolean termsAccepted;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public enum Role        { SUPER_ADMIN, GROUP_ADMIN, MEMBER }
    public enum UserStatus  { PENDING_VERIFICATION, ACTIVE, SUSPENDED, REJECTED }
}
