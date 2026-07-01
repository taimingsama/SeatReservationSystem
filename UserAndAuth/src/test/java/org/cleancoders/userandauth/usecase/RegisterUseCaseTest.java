package org.cleancoders.userandauth.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RegisterUseCaseTest {

    private RegisterUseCase useCase;
    private StubUserRepo userRepo;
    private StubPasswordEncoder passwordEncoder;
    private StubPresenter presenter;

    static class StubUserRepo implements UserRepository {
        private final java.util.Map<String, User> users = new java.util.HashMap<>();

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public Optional<User> findById(String id) {
            return users.values().stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public User save(User user) {
            String id = user.id() != null ? user.id() : "generated-id";
            User saved = new User(id, user.username(), user.password(), user.role(), user.name(), user.email());
            users.put(saved.username(), saved);
            return saved;
        }
    }

    static class StubPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals("hashed:" + rawPassword);
        }
    }

    static class StubPresenter implements RegisterUseCase.Presenter {
        AtomicReference<User> successUser = new AtomicReference<>();
        String usernameAlreadyExistsArg;

        @Override
        public void success(User user) {
            successUser.set(user);
        }

        @Override
        public void usernameAlreadyExists(String username) {
            usernameAlreadyExistsArg = username;
        }
    }

    @BeforeEach
    void setUp() {
        userRepo = new StubUserRepo();
        passwordEncoder = new StubPasswordEncoder();
        presenter = new StubPresenter();

        useCase = new RegisterUseCase();
        useCase.userRepo = userRepo;
        useCase.passwordEncoder = passwordEncoder;
        useCase.presenter = presenter;
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        var output = useCase.execute(new RegisterUseCase.Request("alice", "secret", "Alice", "alice@example.com"));

        assertNotNull(output);
        assertEquals("generated-id", output.userId());

        assertNotNull(presenter.successUser.get());
        assertEquals("alice", presenter.successUser.get().username());
        assertEquals("hashed:secret", presenter.successUser.get().password());
        assertEquals(UserRole.STUDENT, presenter.successUser.get().role());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        userRepo.save(new User("existing", "alice", "oldhash", UserRole.STUDENT, "Old", "old@b.com"));

        var output = useCase.execute(new RegisterUseCase.Request("alice", "secret", "Alice", "alice@example.com"));

        assertNull(output);
        assertEquals("alice", presenter.usernameAlreadyExistsArg);
        assertNull(presenter.successUser.get());
    }

    @Test
    void shouldStoreHashedPassword() {
        useCase.execute(new RegisterUseCase.Request("bob", "mypassword", "Bob", "bob@b.com"));

        assertNotNull(presenter.successUser.get());
        assertNotEquals("mypassword", presenter.successUser.get().password());
    }
}
