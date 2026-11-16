package com.eduflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "{validation.required}")
            @Email(message = "{validation.email}")
            String email,
            @NotBlank(message = "{validation.required}")
            @Size(min = 8, message = "{validation.password.length}")
            String password,
            @NotBlank(message = "{validation.required}") String firstName,
            @NotBlank(message = "{validation.required}") String lastName) {
    }

    public record LoginRequest(
            @NotBlank(message = "{validation.required}")
            @Email(message = "{validation.email}")
            String email,
            @NotBlank(message = "{validation.required}") String password) {
    }

    public record RefreshRequest(@NotBlank(message = "{validation.required}") String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record LanguageUpdateRequest(
            @NotBlank(message = "{validation.required}")
            @Pattern(regexp = "uz|ru", message = "{validation.language.unsupported}")
            String language) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            Long userId,
            String email,
            String firstName,
            String lastName,
            String preferredLanguage,
            boolean mustChangePassword) {
    }

    public record UserSummary(Long id, String email, String firstName, String lastName, String preferredLanguage, boolean mustChangePassword) {
    }

    public record CenterSummary(Long id, String name, String slug, String status) {
    }
}
