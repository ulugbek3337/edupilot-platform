package com.eduflow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;

@Component
@RequiredArgsConstructor
public class TenantRlsContext {
    private final JdbcTemplate jdbcTemplate;

    public void apply(Long centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Tenant center ID is required");
        }
        setLocal("app.current_center_id", centerId.toString());
    }

    /**
     * Establishes the authenticated user's transaction-local context for
     * identity-owned records such as memberships and refresh tokens.
     */
    public void applyUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user ID is required");
        }
        setLocal("app.current_user_id", userId.toString());
    }

    /**
     * Restricts pre-authentication account lookup to the email being verified.
     */
    public void applyUserEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        setLocal("app.current_user_email", email);
    }

    private void setLocal(String setting, String value) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return null;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "select set_config('" + setting + "', ?, true)")) {
                statement.setString(1, value);
                statement.execute();
            }
            return null;
        });
    }
}
