package org.cleancoders.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void shouldEncodePassword() {
        String encoded = encoder.encode("mysecret");
        assertNotNull(encoded);
        assertNotEquals("mysecret", encoded);
    }

    @Test
    void shouldMatchSamePassword() {
        String encoded = encoder.encode("mysecret");
        assertTrue(encoder.matches("mysecret", encoded));
    }

    @Test
    void shouldNotMatchDifferentPassword() {
        String encoded = encoder.encode("mysecret");
        assertFalse(encoder.matches("wrong", encoded));
    }

    @Test
    void shouldProduceDifferentHashesForSameInput() {
        String h1 = encoder.encode("same");
        String h2 = encoder.encode("same");
        assertNotEquals(h1, h2, "BCrypt 盐值应使每次哈希结果不同");
        assertTrue(encoder.matches("same", h1));
        assertTrue(encoder.matches("same", h2));
    }
}
