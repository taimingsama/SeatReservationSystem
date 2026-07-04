package org.cleancoders.userandauth.usecase;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LoginUseCaseTest
{

    private LoginUseCase useCase;
    private StubUserRepo userRepo;
    private StubPasswordEncoder passwordEncoder;
    private StubTokenService tokenService;
    private StubPresenter presenter;

    // --- Stubs ---

    @BeforeEach
    void setUp()
    {
        userRepo = new StubUserRepo();
        passwordEncoder = new StubPasswordEncoder();
        tokenService = new StubTokenService();
        presenter = new StubPresenter();

        useCase = new LoginUseCase();
        useCase.userRepo = userRepo;
        useCase.passwordEncoder = passwordEncoder;
        useCase.tokenService = tokenService;
        useCase.presenter = presenter;
    }

    @Test
    void shouldReturnTokenOnValidCredentials()
    {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", "secret"));

        assertNotNull(output);
        assertEquals("jwt:u1", output.token());
        assertEquals("jwt:u1", presenter.successToken.get());
        assertNotNull(presenter.successUser.get());
        assertEquals("alice", presenter.successUser.get().username());
    }

    @Test
    void shouldReturnTokenForAdminUser()
    {
        userRepo.addUser(new User("u2", "bob", "encoded:adminpw", UserRole.ADMIN, "Bob", "bob@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("bob", "adminpw"));

        assertNotNull(output);
        assertEquals("jwt:u2", output.token());
    }

    @Test
    void shouldCallUserNotFoundWhenUsernameMissing()
    {
        var output = useCase.execute(new LoginUseCase.Request("nobody", "any"));

        assertNull(output);
        assertTrue(presenter.userNotFoundCalled);
        assertFalse(presenter.invalidCredentialsCalled);
        assertNull(presenter.successToken.get());
    }

    @Test
    void shouldCallInvalidCredentialsWhenPasswordWrong()
    {
        userRepo.addUser(new User("u1", "alice", "encoded:correct", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", "wrong"));

        assertNull(output);
        assertTrue(presenter.invalidCredentialsCalled);
        assertFalse(presenter.userNotFoundCalled);
        assertNull(presenter.successToken.get());
    }

    @Test
    void shouldRejectEmptyPassword()
    {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com"));

        var output = useCase.execute(new LoginUseCase.Request("alice", ""));

        assertNull(output);
        assertTrue(presenter.invalidCredentialsCalled);
    }

    @Test
    void shouldRejectBannedUser()
    {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com",
                0, 0, 0, 100, true));

        var output = useCase.execute(new LoginUseCase.Request("alice", "secret"));

        assertNull(output);
        assertTrue(presenter.userBannedCalled);
        assertNull(presenter.successToken.get());
    }

    @Test
    void shouldAllowNonBannedUser()
    {
        userRepo.addUser(new User("u1", "alice", "encoded:secret", UserRole.STUDENT, "Alice", "a@b.com",
                0, 0, 0, 100, false));

        var output = useCase.execute(new LoginUseCase.Request("alice", "secret"));

        assertNotNull(output);
        assertEquals("jwt:u1", output.token());
        assertFalse(presenter.userBannedCalled);
    }

    static class StubPasswordEncoder implements PasswordEncoder
    {
        @Override
        public String encode(String rawPassword)
        {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword)
        {
            return encodedPassword.equals("encoded:" + rawPassword);
        }
    }

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
            String[] parts = token.split(":");
            if (parts.length != 4 || !"jwt".equals(parts[0]))
            {
                throw new TokenValidationException("Invalid token format");
            }
            return new TokenPayload(parts[1]);
        }
    }

    static class StubPresenter implements LoginUseCase.Presenter
    {
        AtomicReference<String> successToken = new AtomicReference<>();
        AtomicReference<User> successUser = new AtomicReference<>();
        boolean invalidCredentialsCalled = false;
        boolean userNotFoundCalled = false;
        boolean userBannedCalled = false;

        @Override
        public void success(String token, User user)
        {
            successToken.set(token);
            successUser.set(user);
        }

        @Override
        public void invalidCredentials()
        {
            invalidCredentialsCalled = true;
        }

        @Override
        public void userNotFound()
        {
            userNotFoundCalled = true;
        }

        @Override
        public void userBanned()
        {
            userBannedCalled = true;
        }
    }
}
