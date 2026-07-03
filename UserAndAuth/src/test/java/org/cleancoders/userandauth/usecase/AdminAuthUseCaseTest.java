package org.cleancoders.userandauth.usecase;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link AdminAuthUseCase#authorize}: only ADMIN role is allowed;
 * all other roles trigger {@link Presenter#forbidden} and abort execution.
 * Also verifies inherited authentication behavior (invalid token → invalidToken).
 * <p>
 * Uses a concrete test-only subclass since AdminAuthUseCase is abstract.
 */
class AdminAuthUseCaseTest
{

    private static final String ADMIN_ID = "u2";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";
    private static final String STUDENT_ID = "u1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private TestableAdminUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        presenter = new StubPresenter();

        useCase = new TestableAdminUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        // presenter field is declared (shadowed) at multiple levels of the hierarchy.
        // authorize() uses the AdminAuthUseCase-level field; authenticate() uses the
        // AuthUseCase-level field. Set both to the same stub, mirroring CheckInUseCaseTest.
        useCase.presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
    }

    @Test
    void shouldExecuteWhenUserIsAdmin()
    {
        var output = useCase.execute(new TestableAdminUseCase.Request(ADMIN_TOKEN));

        assertEquals("executed", output);
        assertTrue(useCase.doExecuteCalled);
        assertEquals(ADMIN_ID, useCase.receivedUser.id());
        assertFalse(presenter.forbiddenCalled);
    }

    @Test
    void shouldCallForbiddenAndReturnNullWhenUserIsStudent()
    {
        var output = useCase.execute(new TestableAdminUseCase.Request(STUDENT_TOKEN));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallInvalidTokenWhenTokenMalformed()
    {
        // Inherited auth behavior: invalid token never reaches authorize()
        var output = useCase.execute(new TestableAdminUseCase.Request("bad-token"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
        assertFalse(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallUserNotFoundWhenUserIdNotInRepo()
    {
        var output = useCase.execute(new TestableAdminUseCase.Request("jwt:ghost:nobody:ADMIN"));

        assertNull(output);
        assertTrue(presenter.userNotFoundCalled);
        assertFalse(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    // --- Testable subclass ---

    static class TestableAdminUseCase extends AdminAuthUseCase<TestableAdminUseCase.Request, String>
    {
        boolean doExecuteCalled = false;
        User receivedUser;

        @Override
        protected String doExecute(User user, Request req)
        {
            doExecuteCalled = true;
            receivedUser = user;
            return "executed";
        }

        public record Request(String token) implements AuthUseCase.Request
        {
        }
    }

    // --- Stubs ---

    static class StubTokenService implements TokenService
    {
        @Override
        public String generate(String userId)
        {
            return "jwt:" + userId;
        }

        @Override
        public TokenPayload validate(String token)
        {
            if (token == null || !token.startsWith("jwt:"))
            {
                throw new TokenValidationException("Invalid token");
            }
            String[] parts = token.split(":");
            if (parts.length != 4)
            {
                throw new TokenValidationException("Invalid token format");
            }
            return new TokenPayload(parts[1]);
        }
    }

    static class StubUserRepo implements UserRepository
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

    /**
     * Implements both {@link AdminAuthUseCase.Presenter} (for the forbidden branch)
     * and {@link AuthUseCase.Presenter} (for the inherited invalidToken/userNotFound
     * branches). The two nested Presenter interfaces are independent — neither extends
     * the other — so one class must implement both.
     */
    static class StubPresenter implements AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean forbiddenCalled = false;
        boolean invalidTokenCalled = false;
        boolean userNotFoundCalled = false;

        @Override
        public void forbidden()
        {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken()
        {
            invalidTokenCalled = true;
        }

        @Override
        public void userNotFound()
        {
            userNotFoundCalled = true;
        }
    }
}