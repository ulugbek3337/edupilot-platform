package com.eduflow.security;

import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedUserTest {

    @Test
    void inactiveUserIsNotEnabledForAuthentication() {
        User user = new User("inactive@example.test", "hash", "Inactive", "User");
        user.setEnabled(false);
        user.setStatus(UserStatus.INACTIVE);

        assertFalse(AuthenticatedUser.from(user).isEnabled());
    }

    @Test
    void lockedUserIsNotAllowedToAuthenticate() {
        User user = new User("locked@example.test", "hash", "Locked", "User");
        user.setEnabled(true);
        user.setStatus(UserStatus.LOCKED);

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertFalse(authenticatedUser.isEnabled());
        assertFalse(authenticatedUser.isAccountNonLocked());
    }

    @Test
    void tenantRolesAreNotGrantedAsGlobalAuthorities() {
        User user = new User("teacher@example.test", "hash", "Tenant", "Teacher");
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertEquals(1, authenticatedUser.getAuthorities().size());
        assertTrue(authenticatedUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertFalse(authenticatedUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CENTER_ADMIN")));
        assertFalse(authenticatedUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    @Test
    void activeEnabledUserCanAuthenticate() {
        User user = new User("active@example.test", "hash", "Active", "User");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertTrue(authenticatedUser.isEnabled());
        assertTrue(authenticatedUser.isAccountNonLocked());
    }
}
