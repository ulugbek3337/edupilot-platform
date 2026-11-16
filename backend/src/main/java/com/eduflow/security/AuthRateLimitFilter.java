package com.eduflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count",
            Long.class
    );

    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.auth.login-max-attempts:10}")
    private int loginLimit;

    @Value("${app.rate-limit.auth.register-max-attempts:5}")
    private int registerLimit;

    @Value("${app.rate-limit.auth.refresh-max-attempts:30}")
    private int refreshLimit;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private long windowSeconds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return !("/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        int limit = path.endsWith("/login") ? loginLimit
                : path.endsWith("/register") ? registerLimit : refreshLimit;
        long effectiveWindowSeconds = Math.max(windowSeconds, 1L);
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String key = "edupilot:rate-limit:auth:"
                + path.substring(path.lastIndexOf('/') + 1) + ":" + ip;

        try {
            // INCR and EXPIRE run atomically: a process crash cannot leave a permanent counter.
            Long count = redis.execute(
                    INCREMENT_WITH_TTL,
                    List.of(key),
                    Long.toString(effectiveWindowSeconds)
            );
            if (count == null) {
                rejectTemporarily(response);
                return;
            }
            if (count > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(effectiveWindowSeconds));
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":\"rate.limit.exceeded\","
                                + "\"message\":\"Too many authentication requests. Try again later.\"}");
                return;
            }
        } catch (DataAccessException ex) {
            rejectTemporarily(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void rejectTemporarily(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"code\":\"rate.limit.unavailable\","
                        + "\"message\":\"Authentication is temporarily unavailable.\"}");
    }
}
