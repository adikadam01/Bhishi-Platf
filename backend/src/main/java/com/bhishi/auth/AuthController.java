package com.bhishi.auth;

// ============================================================
// DIGITAL BHISHI PLATFORM — Auth Controller
// Phase 2 | File: AuthController.java | Package: com.bhishi.auth
// REST Endpoints:
//   POST /api/auth/register/step1     → Basic details + send OTP
//   POST /api/auth/otp/send           → Resend OTP
//   POST /api/auth/otp/verify         → Verify OTP
//   POST /api/auth/register/step2     → Aadhaar + photo
//   POST /api/auth/register/step3     → Terms + password
//   POST /api/auth/login/otp          → OTP-based login
//   POST /api/auth/login/password     → Password-based login
//   GET  /api/auth/me                 → Get logged-in user profile
// Links to Phase 1:
//   - Uses OtpToken.OtpPurpose (com.bhishi.model.Models)
//   - Uses ApiResponse wrapper (com.bhishi.Foundation)
//   - Secured by SecurityConfig (com.bhishi.security.Security)
// ============================================================

import com.bhishi.model.OtpToken;
import com.bhishi.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────
    // STEP 1 — Basic Details
    // Creates user record + sends OTP to phone
    // ─────────────────────────────────────────────
    @PostMapping("/register/step1")
    public ResponseEntity<ApiResponse<RegisterStep1Response>> registerStep1(
            @Valid @RequestBody RegisterStep1Request req) {
        RegisterStep1Response response = authService.registerStep1(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Step 1 complete. OTP sent to your phone.", response));
    }

    // ─────────────────────────────────────────────
    // SEND OTP — resend or initial send
    // ─────────────────────────────────────────────
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest req) {
        OtpToken.OtpPurpose purpose = OtpToken.OtpPurpose.valueOf(req.getPurpose().name());
        OtpResponse response = authService.sendOtp(req.getPhone(), purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    // ─────────────────────────────────────────────
    // VERIFY OTP — used after step1 before step2
    // ─────────────────────────────────────────────
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req) {
        OtpToken.OtpPurpose purpose = OtpToken.OtpPurpose.valueOf(req.getPurpose().name());
        authService.verifyOtp(req.getPhone(), req.getOtp(), purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", "proceed_to_step2"));
    }

    // ─────────────────────────────────────────────
    // STEP 2 — Identity Verification
    // Aadhaar last 4 digits + profile photo
    // ─────────────────────────────────────────────
    @PostMapping("/register/step2")
    public ResponseEntity<ApiResponse<String>> registerStep2(
            @Valid @RequestBody RegisterStep2Request req) {
        String message = authService.registerStep2(req);
        return ResponseEntity.ok(ApiResponse.success(message, "proceed_to_step3"));
    }

    // ─────────────────────────────────────────────
    // STEP 3 — Accept Terms + Set Password
    // Completes registration. Returns JWT token.
    // ─────────────────────────────────────────────
    @PostMapping("/register/step3")
    public ResponseEntity<ApiResponse<AuthResponse>> registerStep3(
            @Valid @RequestBody RegisterStep3Request req) {
        AuthResponse response = authService.registerStep3(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration complete! Your account is pending admin approval.", response));
    }

    // ─────────────────────────────────────────────
    // LOGIN — OTP based
    // ─────────────────────────────────────────────
    @PostMapping("/login/otp")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithOtp(
            @Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.loginWithOtp(req);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ─────────────────────────────────────────────
    // LOGIN — Password based
    // ─────────────────────────────────────────────
    @PostMapping("/login/password")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest req) {
        AuthResponse response = authService.loginWithPassword(req);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ─────────────────────────────────────────────
    // GET PROFILE — logged-in user
    // Requires Bearer token in Authorization header
    // ─────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ─────────────────────────────────────────────
    // HEALTH CHECK — quick ping to confirm auth routes are up
    // ─────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is up", "pong"));
    }
}
