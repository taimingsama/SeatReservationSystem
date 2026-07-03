package org.cleancoders.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JjwtTokenServiceTest
{

    private static final String SECRET = "this-is-a-test-secret-key-for-jwt-signing-256bit!!";
    private final JjwtTokenService service = new JjwtTokenService();
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldGenerateNonEmptyToken()
    {
        String token = service.generate("u1");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateTokenWithThreeDotSegments()
    {
        String token = service.generate("u1");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have header.payload.signature");
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers()
    {
        String t1 = service.generate("u1");
        String t2 = service.generate("u2");
        assertNotEquals(t1, t2);
    }

    @Test
    void shouldGenerateValidTokenWithCorrectClaims()
    {
        String token = service.generate("u1");

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("u1", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }
}
