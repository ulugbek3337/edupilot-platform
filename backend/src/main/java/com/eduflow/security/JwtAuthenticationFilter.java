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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Never allow a center context from a previous request to leak into this request.
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

                        if (jwtService.isTokenValid(token, userDetails)) {
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

                            Long centerId = jwtService.extractCenterId(token);
                            if (centerId != null) {
                                CurrentCenterContext.set(centerId);
                            }
                        }
                    }
                } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                    // Leave the request unauthenticated. Spring Security will reject it
                    // for protected endpoints using the configured authentication entry point.
                    SecurityContextHolder.clearContext();
                    CurrentCenterContext.clear();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // ThreadLocal values must not survive servlet-container thread reuse.
            CurrentCenterContext.clear();
        }
    }
}
