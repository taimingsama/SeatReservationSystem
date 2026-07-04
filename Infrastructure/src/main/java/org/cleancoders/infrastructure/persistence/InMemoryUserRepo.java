package org.cleancoders.infrastructure.persistence;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepo implements UserRepository
{

    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> byUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username)
    {
        String id = byUsername.get(username);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findById(String id)
    {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public User save(User user)
    {
        String id = user.id() != null ? user.id() : UUID.randomUUID().toString();
        User saved = new User(id, user.username(), user.password(), user.role(),
                user.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), user.creditScore(), user.banned());
        byId.put(id, saved);
        byUsername.put(saved.username(), id);
        return saved;
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public void deleteById(String id) {
        User removed = byId.remove(id);
        if (removed != null) {
            byUsername.remove(removed.username());
        }
    }
}
