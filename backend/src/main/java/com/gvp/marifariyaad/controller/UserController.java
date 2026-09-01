package com.gvp.marifariyaad.controller;

import com.gvp.marifariyaad.dto.ApiMessageResponse;
import com.gvp.marifariyaad.dto.ChangePasswordRequest;
import com.gvp.marifariyaad.dto.ProfileUpdateRequest;
import com.gvp.marifariyaad.dto.UserResponse;
import com.gvp.marifariyaad.entity.User;
import com.gvp.marifariyaad.security.UserPrincipal;
import com.gvp.marifariyaad.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @RequestBody ProfileUpdateRequest request) {
        User updated = authService.updateProfile(principal.getUser(), request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiMessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUser(), request);
        return ResponseEntity.ok(ApiMessageResponse.of(true, "Password changed successfully."));
    }
}
