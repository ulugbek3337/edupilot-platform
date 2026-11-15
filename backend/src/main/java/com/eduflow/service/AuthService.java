package com.eduflow.service;

import com.eduflow.dto.AuthDtos.AuthResponse;
import com.eduflow.dto.AuthDtos.LoginRequest;
import com.eduflow.dto.AuthDtos.LanguageUpdateRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        // Lock the persisted token row so two simultaneous refresh requests
        // cannot both rotate the same refresh token successfully.
        RefreshToken refreshToken = refreshTokenRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new EduFlowException(401, "auth.refresh.invalid"));

        Instant now = Instant.now();
        if (refreshToken.isRevoked()
                || refreshToken.getExpiresAt() == null
                || !refreshToken.getExpiresAt().isAfter(now)) {
            throw new EduFlowException(401, "auth.refresh.invalid");
        }

        User user = refreshToken.getUser();
        Long tokenUserId = jwtService.extractUserId(token);
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

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EduFlowException(404, "user.not-found"));
    }

    @Transactional
    public User updateLanguagePreference(String email, LanguageUpdateRequest request) {
        User user = getCurrentUser(email);
        user.setLanguagePreference(LanguagePreference.fromCode(request.language()));
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, Long centerId) {
        String accessToken = jwtService.generateToken(user, centerId);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
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
                user.getLanguagePreference().getCode()
        );
    }
}
