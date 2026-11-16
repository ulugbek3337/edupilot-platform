package com.eduflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenHashTest {

    @Test
    void storesOnlySha256DigestInsteadOfRawRefreshToken() {
        String rawToken = "eyJhbGciOiJIUzI1NiJ9.test.refresh-token";

        String digest = AuthService.hashRefreshToken(rawToken);

        assertNotEquals(rawToken, digest);
        assertEquals(64, digest.length());
        assertTrue(digest.matches("[0-9a-f]{64}"));
    }

    @Test
    void hashesSameRefreshTokenDeterministicallyForLookup() {
        String rawToken = "refresh-token-for-lookup";

        assertEquals(
                AuthService.hashRefreshToken(rawToken),
                AuthService.hashRefreshToken(rawToken)
        );
    }

    @Test
    void differentRefreshTokensProduceDifferentDigests() {
        assertNotEquals(
                AuthService.hashRefreshToken("refresh-token-one"),
                AuthService.hashRefreshToken("refresh-token-two")
        );
    }
}
