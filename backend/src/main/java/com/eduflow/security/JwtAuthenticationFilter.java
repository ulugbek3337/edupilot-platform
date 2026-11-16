package com.eduflow.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CenterMembershipService centerMembershipService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CurrentCenterContext.clear();

        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                final String token = authHeader.substring(7);

                try {
                    final String username = jwtService.extractUsername(token);

                    if (username != null
                            && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                        if (jwtService.isTokenValid(token, userDetails)
                                && userDetails.isEnabled()
                                && userDetails.isAccountNonLocked()) {
                            if (userDetails instanceof AuthenticatedUser authenticated
                                    && authenticated.mustChangePassword()
                                    && !isAllowedBeforePasswordChange(request)) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "auth.password.change-required");
                                return;
                            }
                            Long centerId = resolveCenterId(request, token);

                            // A client-supplied tenant header is never trusted by itself.
                            // It must identify an active center membership for this user.
                            if (centerId != null
                                    && (!(userDetails instanceof AuthenticatedUser authenticatedUser)
                                    || !centerMembershipService.isActiveMember(authenticatedUser.id(), centerId))) {
                                SecurityContextHolder.clearContext();
                                CurrentCenterContext.clear();
                            } else {
                                UsernamePasswordAuthenticationToken authToken =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities()
                                        );
                                authToken.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request)
                                );
                                SecurityContextHolder.getContext().setAuthentication(authToken);

                                if (centerId != null) {
                                    CurrentCenterContext.set(centerId);
                                }
                            }
                        }
                    }
                } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                    SecurityContextHolder.clearContext();
                    CurrentCenterContext.clear();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentCenterContext.clear();
        }
    }

    private boolean isAllowedBeforePasswordChange(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return ("GET".equals(method) && ("/api/v1/auth/me".equals(path) || "/api/v1/centers".equals(path)
               ))
                || ("POST".equals(method) && ("/api/v1/auth/change-password".equals(path)
                || "/api/v1/auth/refresh".equals(path)));
    }

    private Long resolveCenterId(HttpServletRequest request, String token) {
        String requestedCenterId = request.getHeader("X-Center-Id");
        if (requestedCenterId != null && !requestedCenterId.isBlank()) {
            return Long.parseLong(requestedCenterId);
        }
        return jwtService.extractCenterId(token);
    }
}
