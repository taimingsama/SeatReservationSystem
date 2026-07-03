package org.cleancoders.common.outbound;

/**
 * Thrown when a JWT token is invalid, expired, or malformed.
 */
public class TokenValidationException extends RuntimeException
{

    public TokenValidationException(String message)
    {
        super(message);
    }

    public TokenValidationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}