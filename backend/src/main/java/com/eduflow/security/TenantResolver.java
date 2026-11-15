package com.eduflow.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    public Long resolve(HttpServletRequest request) {
        String centerIdHeader = request.getHeader("X-Center-Id");
        if (centerIdHeader != null && !centerIdHeader.isBlank()) {
            try {
                return Long.parseLong(centerIdHeader);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return CurrentCenterContext.get();
    }
}
