package org.cleancoders.common.domain;

public record User(
    String id,
    String username,
    String password,
    UserRole role,
    String name,
    String email
) {}
