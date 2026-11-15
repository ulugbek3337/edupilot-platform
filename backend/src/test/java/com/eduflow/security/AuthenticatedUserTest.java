package com.eduflow.security;

import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
import org.junit.jupiter.api.Test;

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
    void activeEnabledUserCanAuthenticate() {
        User user = new User("active@example.test", "hash", "Active", "User");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertTrue(authenticatedUser.isEnabled());
        assertTrue(authenticatedUser.isAccountNonLocked());
    }
}
