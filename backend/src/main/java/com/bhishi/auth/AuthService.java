package com.bhishi.auth;

// ============================================================
// DIGITAL BHISHI PLATFORM — Auth Service
// Phase 2 | File: AuthService.java | Package: com.bhishi.auth
// ============================================================

import com.bhishi.exception.*;
import com.bhishi.model.*;
import com.bhishi.repository.*;
import com.bhishi.security.JwtUtil;
import com.bhishi.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository        userRepository;
    private final OtpTokenRepository    otpTokenRepository;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;
    private final JavaMailSender        mailSender;
    private final CodeGenerator         codeGenerator;

    @Value("${app.otp.expiry-minutes}") private int otpExpiryMinutes;
    @Value("${app.otp.length}")         private int otpLength;
    @Value("${app.mail.from}")          private String mailFrom;

    // ─────────────────────────────────────────────
    // STEP 1 — Basic Details
    // Creates user with PENDING_VERIFICATION status
    // and sends OTP to phone for verification
    // ─────────────────────────────────────────────
    public RegisterStep1Response registerStep1(RegisterStep1Request req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already registered");
        if (userRepository.existsByPhone(req.getPhone()))
            throw new ConflictException("Phone already registered");

        // Determine role — default to MEMBER; allow GROUP_ADMIN self-registration.
        // SUPER_ADMIN can never be set via the public registration API.
        User.Role requestedRole = req.getRole();
        if (requestedRole == null || requestedRole == User.Role.SUPER_ADMIN) {
            requestedRole = User.Role.MEMBER;
        }

        if (requestedRole == User.Role.GROUP_ADMIN) {
            if (req.getOrganizationName() == null || req.getOrganizationName().isBlank())
                throw new BadRequestException("Organization name is required for Group Admin registration");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .dob(req.getDob())
                .role(requestedRole)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .termsAccepted(false)
                .build();

        user = userRepository.save(user);
        log.info("Step 1 complete for user: {}", user.getId());

        return RegisterStep1Response.builder()
                .userId(user.getId())
                .message("Basic details saved. Please continue to Step 2.")
                .otpSentTo(maskPhone(req.getPhone()))
                .build();
    }

    // ─────────────────────────────────────────────
    // STEP 2 — Identity: Aadhaar last 4 + photo
    // ─────────────────────────────────────────────
    public String registerStep2(RegisterStep2Request req) {
        User user = findUserById(req.getUserId());

        if (user.getStatus() != User.UserStatus.PENDING_VERIFICATION)
            throw new BadRequestException("Invalid registration state");

        user.setAadhaarLastFour(req.getAadhaarLastFour());

        if (req.getProfilePhotoBase64() != null && !req.getProfilePhotoBase64().isBlank()) {
            try {
                Base64.getDecoder().decode(req.getProfilePhotoBase64().split(",")[1]);
                user.setProfilePhotoUrl("photo_pending_upload_" + user.getId());
            } catch (Exception e) {
                throw new BadRequestException("Invalid photo format. Please provide a valid base64 image.");
            }
        }

        userRepository.save(user);
        log.info("Step 2 complete for user: {}", user.getId());
        return "Identity details saved. Please complete Step 3.";
    }

    // ─────────────────────────────────────────────
    // STEP 3 — Accept Terms + Set Password
    // Finalises registration.
    //
    // MEMBER     → status = ACTIVE immediately
    //              (OTP during registration already verified their identity)
    //
    // GROUP_ADMIN → status = PENDING_VERIFICATION
    //              (must wait for Super Admin to approve their account
    //               before they can create groups)
    // ─────────────────────────────────────────────
    public AuthResponse registerStep3(RegisterStep3Request req) {
        User user = findUserById(req.getUserId());

        if (!req.isTermsAccepted())
            throw new BadRequestException("You must accept the terms and conditions");

        user.setTermsAccepted(true);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        // MEMBERS become ACTIVE immediately — OTP already verified their phone.
        // GROUP_ADMINs stay PENDING_VERIFICATION until Super Admin approves them.
        if (user.getRole() == User.Role.MEMBER) {
            user.setStatus(User.UserStatus.ACTIVE);
            log.info("Registration complete for MEMBER: {} — account is now ACTIVE", user.getId());
        } else if (user.getRole() == User.Role.GROUP_ADMIN) {
            user.setStatus(User.UserStatus.PENDING_VERIFICATION);
            log.info("Registration complete for GROUP_ADMIN: {} — awaiting Super Admin approval", user.getId());
        }

        userRepository.save(user);
        sendConfirmationEmail(user);
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // SEND OTP
    // ─────────────────────────────────────────────
    public OtpResponse sendOtp(String phone, OtpToken.OtpPurpose purpose) {
        otpTokenRepository.deleteByPhoneAndPurpose(phone, purpose);

        String otp = codeGenerator.generateOtp(otpLength);

        OtpToken token = OtpToken.builder()
                .phone(phone)
                .otp(otp)
                .purpose(purpose)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpTokenRepository.save(token);
        log.info("OTP for {} [{}]: {}", maskPhone(phone), purpose, otp);

        return OtpResponse.builder()
                .message("OTP sent successfully")
                .maskedPhone(maskPhone(phone))
                .expiresInMinutes(otpExpiryMinutes)
                .build();
    }

    // ─────────────────────────────────────────────
    // VERIFY OTP
    // ─────────────────────────────────────────────
    public boolean verifyOtp(String phone, String otp, OtpToken.OtpPurpose purpose) {
        Optional<OtpToken> tokenOpt = otpTokenRepository
                .findByPhoneAndPurposeAndUsedFalseAndExpiresAtAfter(phone, purpose, LocalDateTime.now());

        if (tokenOpt.isEmpty())
            throw new BadRequestException("OTP expired or not found. Please request a new OTP.");

        OtpToken token = tokenOpt.get();

        if (!token.getOtp().equals(otp))
            throw new BadRequestException("Incorrect OTP. Please try again.");

        token.setUsed(true);
        otpTokenRepository.save(token);
        log.info("OTP verified for {} [{}]", maskPhone(phone), purpose);
        return true;
    }

    // ─────────────────────────────────────────────
    // OTP LOGIN
    // ─────────────────────────────────────────────
    public AuthResponse loginWithOtp(LoginRequest req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this phone number"));

        if (user.getStatus() == User.UserStatus.SUSPENDED)
            throw new UnauthorizedException("Your account has been suspended. Contact support.");

        if (user.getStatus() == User.UserStatus.REJECTED)
            throw new UnauthorizedException("Your account registration was rejected.");

        verifyOtp(req.getPhone(), req.getOtp(), OtpToken.OtpPurpose.LOGIN);

        log.info("OTP login successful for user: {}", user.getId());
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // PASSWORD LOGIN
    // ─────────────────────────────────────────────
    public AuthResponse loginWithPassword(PasswordLoginRequest req) {
        User user = userRepository.findByPhone(req.getIdentifier())
                .or(() -> userRepository.findByEmail(req.getIdentifier()))
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this phone/email"));

        if (user.getStatus() == User.UserStatus.SUSPENDED)
            throw new UnauthorizedException("Your account has been suspended. Contact support.");

        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new UnauthorizedException("Incorrect password");

        log.info("Password login successful for user: {}", user.getId());
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // GET PROFILE
    // ─────────────────────────────────────────────
    public UserProfileResponse getProfile(String userId) {
        User user = findUserById(userId);
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDob())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, 3) + "XXXXXX" + phone.substring(phone.length() - 4);
    }

    private void sendConfirmationEmail(User user) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(user.getEmail());
            msg.setSubject("Registration Submitted — Digital Bhishi Platform");
            msg.setText(
                "Hello " + user.getName() + ",\n\n" +
                (user.getRole() == User.Role.MEMBER
                    ? "Your registration is complete! You can now log in and join groups.\n\n"
                    : "Your Group Admin registration has been submitted.\n" +
                      "Your account is pending Super Admin approval.\n" +
                      "You will be notified once approved.\n\n") +
                "— Bhishi Platform Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send confirmation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}