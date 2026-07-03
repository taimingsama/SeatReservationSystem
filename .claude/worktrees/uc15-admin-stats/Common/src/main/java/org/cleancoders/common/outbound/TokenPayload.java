package org.cleancoders.common.outbound;

/**
 * Holds the claims extracted from a validated JWT token.
 */
public record TokenPayload(String userId)
{
}