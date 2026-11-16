package com.eduflow.service;

import com.eduflow.dto.AuthDtos.AuthResponse;
import com.eduflow.dto.AuthDtos.LoginRequest;
import com.eduflow.dto.AuthDtos.LanguageUpdateRequest;
import com.eduflow.dto.AuthDtos.ChangePasswordRequest;
import com.eduflow.dto.AuthDtos.RefreshRequest;
import com.eduflow.dto.AuthDtos.RegisterRequest;
import com.eduflow.entity.LanguagePreference;
import com.eduflow.entity.RefreshToken;
import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.RefreshTokenRepository;
import com.eduflow.repository.UserRepository;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.security.JwtService;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TenantRlsContext tenantRlsContext;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        tenantRlsContext.applyUserEmail(request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new EduFlowException(409, "auth.email.already-registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), passwordHash, request.firstName(), request.lastName());
        User saved = userRepository.save(user);

        return buildAuthResponse(saved, CurrentCenterContext.get());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        if (!authentication.isAuthenticated()) {
            throw new EduFlowException(HttpStatus.UNAUTHORIZED, "auth.invalid-credentials");
        }

        tenantRlsContext.applyUserEmail(request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EduFlowException(401, "auth.invalid-credentials"));

        return buildAuthResponse(user, CurrentCenterContext.get());
    }

    // Keep the revocation update committed when an inactive account is rejected.
    @Transactional(noRollbackFor = EduFlowException.class)
    public AuthResponse refreshToken(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isRefreshTokenValid(token)) {
            throw new EduFlowException(401, "auth.refresh.invalid");
        }

        // The verified token carries the identity required by refresh-token RLS.
        // Establish the user scope before looking up the persisted row.
        Long tokenUserId = jwtService.extractUserId(token);
        if (tokenUserId == null) {
            throw new EduFlowException(401, "auth.refresh.invalid");
        }
        tenantRlsContext.applyUser(tokenUserId);

        // Lock the persisted token row so two simultaneous refresh requests
        // cannot both rotate the same refresh token successfully.
        // New rows store SHA-256 digests. The raw-token fallback is temporary
        // compatibility for sessions created before this security upgrade.
        String tokenHash = hashRefreshToken(token);
        Optional<RefreshToken> persistedToken = refreshTokenRepository.findByTokenForUpdate(tokenHash);
        boolean legacyRawToken = persistedToken.isEmpty();
        if (legacyRawToken) {
            persistedToken = refreshTokenRepository.findByTokenForUpdate(token);
        }
        RefreshToken refreshToken = persistedToken
                .orElseThrow(() -> new EduFlowException(401, "auth.refresh.invalid"));
        if (legacyRawToken) {
            // Upgrade legacy rows in place so the raw token is not retained after use.
            refreshToken.setToken(tokenHash);
        }

        Instant now = Instant.now();
        if (refreshToken.isRevoked()
                || refreshToken.getExpiresAt() == null
                || !refreshToken.getExpiresAt().isAfter(now)) {
            throw new EduFlowException(401, "auth.refresh.invalid");
        }

        User user = refreshToken.getUser();
        String tokenSubject = jwtService.extractUsername(token);

        if (user == null
                || user.getId() == null
                || !user.getId().equals(tokenUserId)
                || !user.getEmail().equals(tokenSubject)
                || !user.isEnabled()
                || user.getStatus() != UserStatus.ACTIVE) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new EduFlowException(401, "auth.refresh.invalid");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user, CurrentCenterContext.get());
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        tenantRlsContext.applyUserEmail(email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EduFlowException(404, "user.not-found"));
    }

    @Transactional
    public User changePassword(String email, ChangePasswordRequest request) {
        User user = getCurrentUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "auth.password.current.invalid");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "auth.password.must-be-different");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        User saved = userRepository.save(user);
        tenantRlsContext.applyUser(user.getId());
        refreshTokenRepository.deleteByUserId(user.getId());
        return saved;
    }

    @Transactional
    public User updateLanguagePreference(String email, LanguageUpdateRequest request) {
        User user = getCurrentUser(email);
        if (user.isStudentAccount()) {
            throw new EduFlowException(HttpStatus.FORBIDDEN, "student.account.profile.read-only");
        }
        user.setLanguagePreference(LanguagePreference.fromCode(request.language()));
        return userRepository.save(user);
    }

    static String hashRefreshToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private AuthResponse buildAuthResponse(User user, Long centerId) {
        tenantRlsContext.applyUser(user.getId());
        String accessToken = jwtService.generateToken(user, centerId);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken(
                hashRefreshToken(refreshTokenValue),
                user,
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getLanguagePreference().getCode(),
                user.isMustChangePassword()
        );
    }
}
