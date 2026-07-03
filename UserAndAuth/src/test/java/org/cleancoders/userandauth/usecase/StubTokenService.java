package org.cleancoders.userandauth.usecase;

import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;

class StubTokenService implements TokenService
{
    @Override
    public String generate(String userId)
    {
        return "jwt:" + userId;
    }

    @Override
    public TokenPayload validate(String token)
    {
        if (token == null || !token.startsWith("jwt:"))
        {
            throw new TokenValidationException("Invalid token");
        }
        String[] parts = token.split(":");
        if (parts.length != 4)
        {
            throw new TokenValidationException("Invalid token format");
        }
        return new TokenPayload(parts[1]);
    }
}
