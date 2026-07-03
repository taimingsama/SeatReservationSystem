package org.cleancoders.infrastructure.security;

import org.cleancoders.userandauth.outbound.PasswordEncoder;

/**
 * A no-op {@link PasswordEncoder} that returns the raw password as-is.
 * Intended for use in tests only — never use in production.
 */
public class RawPasswordEncoder implements PasswordEncoder
{
    @Override
    public String encode(String rawPassword)
    {
        return rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword)
    {
        return rawPassword.equals(encodedPassword);
    }
}
