package org.cleancoders.reservation.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.outbound.UserRepository;

import java.util.Optional;

class StubUserRepo implements UserRepository
{
    private final java.util.Map<String, User> users = new java.util.HashMap<>();

    void addUser(User user)
    {
        users.put(user.id(), user);
    }

    @Override
    public Optional<User> findByUsername(String username)
    {
        return users.values().stream()
                .filter(u -> u.username().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findById(String id)
    {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user)
    {
        users.put(user.id(), user);
        return user;
    }
}
