package com.eduflow.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    public Long resolve(HttpServletRequest request) {
        // X-Center-Id is validated by JwtAuthenticationFilter before it reaches
        // CurrentCenterContext. Never parse an untrusted header in a service.
        return CurrentCenterContext.get();
    }
}
