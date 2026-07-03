package org.cleancoders.userandauth_test_infrastructure;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.UserRepository;

import java.util.Optional;

public class StubUserRepo implements UserRepository
{
    private final java.util.Map<String, User> users = new java.util.HashMap<>();

    public void addUser(User user)
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
        String id = user.id() != null ? user.id() : "generated-id";
        User saved = new User(id, user.username(), user.password(), user.role(),
                user.name(), user.email(), user.reservationCount(), user.studyHours(),
                user.checkInCount(), user.creditScore());
        users.put(saved.username(), saved);
        return saved;
    }
}
