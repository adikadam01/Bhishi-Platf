package com.bhishi.auth;

import com.bhishi.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterStep1Request {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[0-9]{10}$", message = "Phone must be in format +91XXXXXXXXXX")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    // Optional: MEMBER (default) or GROUP_ADMIN
    // SUPER_ADMIN cannot be registered via this form
    private User.Role role;

    // Required only when role = GROUP_ADMIN
    private String organizationName;
    private String businessPhone;
}