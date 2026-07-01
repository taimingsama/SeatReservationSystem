package org.cleancoders.infrastructure.persistence;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepoTest {

    private InMemoryUserRepo repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserRepo();
    }

    @Test
    void shouldSaveAndFindByUsername() {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        repo.save(user);

        var found = repo.findByUsername("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().username());
        assertEquals("hashed", found.get().password());
    }

    @Test
    void shouldReturnEmptyForUnknownUsername() {
        var found = repo.findByUsername("nobody");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldSaveAndFindById() {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        repo.save(user);

        var found = repo.findById("u1");
        assertTrue(found.isPresent());
    }

    @Test
    void shouldGenerateIdWhenNull() {
        User user = new User(null, "bob", "hashed", UserRole.ADMIN, "Bob", "bob@b.com");
        User saved = repo.save(user);

        assertNotNull(saved.id());
        var found = repo.findById(saved.id());
        assertTrue(found.isPresent());
    }
}
