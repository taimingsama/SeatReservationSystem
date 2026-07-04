package org.cleancoders.userandauth.outbound;

import org.cleancoders.userandauth.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository
{
    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    User save(User user);

    default List<User> findAll() { return List.of(); }

    default void deleteById(String id) {}
}
