package com.gvp.marifariyaad.controller;

import com.gvp.marifariyaad.dto.*;
import com.gvp.marifariyaad.entity.User;
import com.gvp.marifariyaad.security.UserPrincipal;
import com.gvp.marifariyaad.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.startRegistration(request);
        return ResponseEntity.ok(ApiMessageResponse.of(true,
                "Registration OTP sent to " + request.getEmail() + ". Please verify to complete your registration."));
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request,
                                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = authService.completeRegistration(request);
        authenticateAndCreateSession(user.getEmail(), null, user, httpRequest, httpResponse);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "Email verified successfully. Your account has been created.");
        body.put("user", UserResponse.fromEntity(user));
        body.put("redirect", user.getRole().name().equals("ADMIN") ? "/admin-dashboard.html" : "/dashboard.html");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiMessageResponse.of(false, "Your account has been disabled. Please contact the administrator."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiMessageResponse.of(false, "Invalid email or password."));
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = ((UserPrincipal) authentication.getPrincipal()).getUser();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "Login successful.");
        body.put("user", UserResponse.fromEntity(user));
        body.put("redirect", user.getRole().name().equals("ADMIN") ? "/admin-dashboard.html" : "/dashboard.html");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.startForgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiMessageResponse.of(true,
                "A password reset OTP has been sent to " + request.getEmail() + "."));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiMessageResponse> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        authService.verifyResetOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiMessageResponse.of(true, "OTP verified. You may now set a new password."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiMessageResponse.of(true,
                "Password reset successful. Please login with your new password."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiMessageResponse.of(true, "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiMessageResponse.of(false, "Not logged in."));
        }
        return ResponseEntity.ok(UserResponse.fromEntity(principal.getUser()));
    }

    /**
     * Manually builds an Authentication and stores it in the HTTP session -
     * used right after OTP-verified registration so the new user is immediately
     * logged in, without requiring a separate login call.
     */
    private void authenticateAndCreateSession(String email, String rawPasswordUnused, User user,
                                               HttpServletRequest request, HttpServletResponse response) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
