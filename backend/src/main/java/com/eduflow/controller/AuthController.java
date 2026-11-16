package com.eduflow.controller;

import com.eduflow.dto.AuthDtos.AuthResponse;
import com.eduflow.dto.AuthDtos.LoginRequest;
import com.eduflow.dto.AuthDtos.LanguageUpdateRequest;
import com.eduflow.dto.AuthDtos.ChangePasswordRequest;
import com.eduflow.dto.AuthDtos.RefreshRequest;
import com.eduflow.dto.AuthDtos.RegisterRequest;
import com.eduflow.dto.AuthDtos.UserSummary;
import com.eduflow.entity.User;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = authService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(toUserSummary(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/language")
    public ResponseEntity<UserSummary> updateLanguage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody LanguageUpdateRequest request) {
        User user = authService.updateLanguagePreference(principal.getUsername(), request);
        return ResponseEntity.ok(toUserSummary(user));
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getLanguagePreference().getCode()
        );
    }
}
