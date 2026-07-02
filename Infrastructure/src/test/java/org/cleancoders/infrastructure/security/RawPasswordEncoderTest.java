package org.cleancoders.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawPasswordEncoderTest
{
    private final RawPasswordEncoder encoder = new RawPasswordEncoder();

    @Test
    void shouldReturnRawPasswordAsIs()
    {
        assertEquals("secret123", encoder.encode("secret123"));
    }

    @Test
    void shouldReturnTrueWhenPasswordsMatch()
    {
        assertTrue(encoder.matches("secret123", "secret123"));
    }

    @Test
    void shouldReturnFalseWhenPasswordsDiffer()
    {
        assertFalse(encoder.matches("secret123", "wrong"));
    }

    @Test
    void shouldReturnEmptyForEmptyInput()
    {
        assertEquals("", encoder.encode(""));
    }

    @Test
    void shouldMatchEmptyPasswords()
    {
        assertTrue(encoder.matches("", ""));
    }
}
