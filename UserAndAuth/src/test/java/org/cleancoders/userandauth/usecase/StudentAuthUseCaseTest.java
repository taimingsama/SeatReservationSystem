package org.cleancoders.userandauth.usecase;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link StudentAuthUseCase#authorize}: only STUDENT role is allowed;
 * all other roles trigger {@link Presenter#forbidden} and abort execution.
 * Also verifies inherited authentication behavior (invalid token → invalidToken).
 * <p>
 * Uses a concrete test-only subclass since StudentAuthUseCase is abstract.
 */
class StudentAuthUseCaseTest
{

    private static final String STUDENT_ID = "u1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "u2";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";

    private TestableStudentUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        presenter = new StubPresenter();

        useCase = new TestableStudentUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        // presenter field is declared (shadowed) at multiple levels of the hierarchy.
        // authorize() uses the StudentAuthUseCase-level field; authenticate() uses the
        // AuthUseCase-level field. Set both to the same stub, mirroring CheckInUseCaseTest.
        useCase.presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
    }

    @Test
    void shouldExecuteWhenUserIsStudent()
    {
        var output = useCase.execute(new TestableStudentUseCase.Request(STUDENT_TOKEN));

        assertEquals("executed", output);
        assertTrue(useCase.doExecuteCalled);
        assertEquals(STUDENT_ID, useCase.receivedUser.id());
        assertFalse(presenter.forbiddenCalled);
    }

    @Test
    void shouldCallForbiddenAndReturnNullWhenUserIsAdmin()
    {
        var output = useCase.execute(new TestableStudentUseCase.Request(ADMIN_TOKEN));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallInvalidTokenWhenTokenMalformed()
    {
        // Inherited auth behavior: invalid token never reaches authorize()
        var output = useCase.execute(new TestableStudentUseCase.Request("bad-token"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
        assertFalse(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    @Test
    void shouldCallUserNotFoundWhenUserIdNotInRepo()
    {
        var output = useCase.execute(new TestableStudentUseCase.Request("jwt:ghost:nobody:STUDENT"));

        assertNull(output);
        assertTrue(presenter.userNotFoundCalled);
        assertFalse(presenter.forbiddenCalled);
        assertFalse(useCase.doExecuteCalled);
    }

    // --- Testable subclass ---

    static class TestableStudentUseCase extends StudentAuthUseCase<TestableStudentUseCase.Request, String>
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

    /**
     * Implements both {@link StudentAuthUseCase.Presenter} (for the forbidden branch)
     * and {@link AuthUseCase.Presenter} (for the inherited invalidToken/userNotFound
     * branches). The two nested Presenter interfaces are independent — neither extends
     * the other — so one class must implement both.
     */
    static class StubPresenter implements StudentAuthUseCase.Presenter, AuthUseCase.Presenter
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