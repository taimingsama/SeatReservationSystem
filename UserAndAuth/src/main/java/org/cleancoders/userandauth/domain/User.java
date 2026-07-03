package org.cleancoders.userandauth.domain;

public record User(
        String id,
        String username,
        String password,
        UserRole role,
        String name,
        String email
)
{
}
