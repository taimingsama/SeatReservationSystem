package org.cleancoders.userandauth.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_test_infrastructure.StubTokenService;
import org.cleancoders.common_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link GetMeUseCase#doExecute}: the authenticated user is forwarded to
 * {@link GetMeUseCase.Presenter#presentUser} and wrapped in the returned {@link GetMeUseCase.Output}.
 * <p>
 * Token validation (invalid token / user-not-found branches) is covered by AuthUseCaseTest
 * and is deliberately not re-tested here. A valid token + existing user is set up only as
 * the precondition required to reach doExecute via the inherited execute() template method.
 */
class GetMeUseCaseTest
{

    private static final String STUDENT_ID = "u1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "u2";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";

    private GetMeUseCase useCase;
    private StubUserRepo userRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;

    @BeforeEach
    void setUp()
    {
        useCase = new GetMeUseCase();
        userRepo = new StubUserRepo();
        presenter = new StubPresenter();
        tokenService = new StubTokenService();
        tokenService.setUserId(STUDENT_ID);

        // GetMeUseCase re-declares (shadows) userRepo/tokenService/presenter, so the
        // subclass-level fields are the ones its own code reads. authenticate() in the
        // parent AuthUseCase reads the AuthUseCase-level fields — set those too so the
        // inherited template method can reach doExecute.
        useCase.userRepo = userRepo;
        useCase.tokenService = tokenService;
        useCase.presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "alice@example.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "bob@example.com"));
    }

    @Test
    void shouldPresentStudentAndReturnOutput()
    {
        var output = useCase.execute(new GetMeUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertNotNull(output.user());
        assertEquals(STUDENT_ID, output.user().id());
        assertEquals("alice", output.user().username());
        assertEquals(UserRole.STUDENT, output.user().role());

        assertNotNull(presenter.presentedUser.get());
        User presented = presenter.presentedUser.get();
        assertEquals(STUDENT_ID, presented.id());
        assertEquals("alice", presented.username());
        assertEquals("Alice", presented.name());
        assertEquals("alice@example.com", presented.email());
    }

    @Test
    void shouldPresentAdminAndReturnOutput()
    {
        tokenService.setUserId(ADMIN_ID);
        var output = useCase.execute(new GetMeUseCase.Request(ADMIN_TOKEN));

        assertNotNull(output);
        assertEquals(ADMIN_ID, output.user().id());
        assertEquals("bob", output.user().username());
        assertEquals(UserRole.ADMIN, output.user().role());

        assertEquals(ADMIN_ID, presenter.presentedUser.get().id());
        assertEquals("Bob", presenter.presentedUser.get().name());
    }

    @Test
    void shouldPresentSameUserInstanceReturnedInOutput()
    {
        var output = useCase.execute(new GetMeUseCase.Request(STUDENT_TOKEN));

        // The user passed to the presenter and the user wrapped in the output are the
        // same object received from authenticate() — no mutation, no re-lookup.
        assertSame(presenter.presentedUser.get(), output.user());
    }

    @Test
    void shouldPresentExactlyOncePerExecution()
    {
        useCase.execute(new GetMeUseCase.Request(STUDENT_TOKEN));

        assertEquals(1, presenter.presentCallCount);
    }

    // --- Stubs ---

    static class StubPresenter implements GetMeUseCase.Presenter, AuthUseCase.Presenter
    {
        final AtomicReference<User> presentedUser = new AtomicReference<>();
        int presentCallCount = 0;

        @Override
        public void presentUser(User user)
        {
            presentedUser.set(user);
            presentCallCount++;
        }

        // Inherited auth-error branches — deliberately not under test here (covered by
        // AuthUseCaseTest). Failing loudly if either is called turns an accidental slip
        // into an explicit, self-explanatory test failure rather than a silent no-op.
        @Override
        public void invalidToken()
        {
            fail("invalidToken() must not be called — token validation is not under test in GetMeUseCaseTest");
        }

        @Override
        public void userNotFound()
        {
            fail("userNotFound() must not be called — token validation is not under test in GetMeUseCaseTest");
        }
    }
}