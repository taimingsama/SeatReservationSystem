package org.cleancoders.common.outbound;

public interface TokenService
{
    String generate(String userId);

    /**
     * Validates the JWT token and extracts the payload claims.
     *
     * @param token the raw JWT token string
     * @return the extracted payload (userId, username, role)
     * @throws TokenValidationException if the token is invalid, expired, or malformed
     */
    TokenPayload validate(String token);
}
