package org.cleancoders.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.cleancoders.common.outbound.TokenPayload;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.TokenValidationException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JjwtTokenService implements TokenService
{

    private static final String SECRET = "this-is-a-test-secret-key-for-jwt-signing-256bit!!";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Override
    public String generate(String userId)
    {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    @Override
    public TokenPayload validate(String token)
    {
        try
        {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new TokenPayload(
                    claims.getSubject()
            );
        } catch (JwtException | IllegalArgumentException e)
        {
            throw new TokenValidationException("Invalid or expired token", e);
        }
    }
}
