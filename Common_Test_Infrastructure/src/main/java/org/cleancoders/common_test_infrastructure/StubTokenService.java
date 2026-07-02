package org.cleancoders.reservation.usecase;

import org.cleancoders.common.outbound.TokenPayload;
import org.cleancoders.common.outbound.TokenService;

import static org.junit.jupiter.api.Assertions.fail;

class StubTokenService implements TokenService
{
    private TokenPayload payload = null;

    void setUserId(String userId)
    {
        this.payload = new TokenPayload(userId);
    }

    @Override
    public String generate(String userId)
    {
        fail("generate() is not expected to be called in CheckInUseCaseTest");
        return null;
    }

    @Override
    public TokenPayload validate(String token)
    {
        return payload;
    }
}
