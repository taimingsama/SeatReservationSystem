package org.cleancoders.common.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.TokenPayload;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.TokenValidationException;
import org.cleancoders.common.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Template Method behavior of {@link AuthUseCase}:
 * authenticate → authorize (default allows all) → doExecute.
 * <p>
 * Uses a concrete test-only subclass since AuthUseCase is abstract.
 */
class AuthUseCaseTest
{

    private static final String STUDENT_ID = "u1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "u2";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";

    private TestableAuthUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        presenter = new StubPresenter();

        useCase = new TestableAuthUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
    }

    @Test
    void shouldExecuteAndReturnOutputWhenTokenValidAndUserExists()
    {
        var output = useCase.execute(new TestableAuthUseCase.Request(STUDENT_TOKEN));

        assertEquals("executed", output);
        assertTrue(useCase.doExecuteCalled);
        assertEquals(STUDENT_ID, useCase.receivedUser.id());
        assertFalse(presenter.invalidTokenCalled);
        assertFalse(presenter.userNotFoundCalled);
    }

    @Test
    void shouldCallInvalidTokenAndReturnNullWhenTokenMalformed()
    {
        var output = useCase.execute(new TestableAuthUseCase.Request("bad-token"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
        assertFalse(presenter.userNotFoundCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallInvalidTokenWhenTokenIsNull()
    {
        var output = useCase.execute(new TestableAuthUseCase.Request(null));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallUserNotFoundWhenUserIdNotInRepo()
    {
        // Valid token format, but the referenced user does not exist
        String orphanToken = "jwt:ghost:nobody:STUDENT";

        var output = useCase.execute(new TestableAuthUseCase.Request(orphanToken));

        assertNull(output);
        assertTrue(presenter.userNotFoundCalled);
        assertFalse(presenter.invalidTokenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void defaultAuthorizeShouldAllowBothStudentAndAdmin()
    {
        var studentOutput = useCase.execute(new TestableAuthUseCase.Request(STUDENT_TOKEN));
        var adminOutput = useCase.execute(new TestableAuthUseCase.Request(ADMIN_TOKEN));

        assertEquals("executed", studentOutput);
        assertEquals("executed", adminOutput);
        assertTrue(useCase.doExecuteCalled);
        // receivedUser holds the last invocation (admin)
        assertEquals(UserRole.ADMIN, useCase.receivedUser.role());
        assertFalse(presenter.invalidTokenCalled);
        assertFalse(presenter.userNotFoundCalled);
    }

    // --- Testable subclass ---

    static class TestableAuthUseCase extends AuthUseCase<TestableAuthUseCase.Request, String>
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

    static class StubPresenter implements AuthUseCase.Presenter
    {
        boolean invalidTokenCalled = false;
        boolean userNotFoundCalled = false;

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