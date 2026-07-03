package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.web.dto.auth.LoginRequest;
import org.cleancoders.web.dto.auth.RegisterRequest;
import org.cleancoders.web.presenter.ResponseContext;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthResourceTest
{

    private AuthResource resource;
    private WebApiAuthPresenter presenter;
    private ResponseContext ctx;
    private boolean loginExecuteCalled;
    private LoginUseCase.Request lastLoginRequest;
    private LoginUseCase.Output loginOutputToReturn;
    private boolean registerExecuteCalled;
    private RegisterUseCase.Request lastRegisterRequest;
    private RegisterUseCase.Output registerOutputToReturn;

    @BeforeEach
    void setUp()
    {
        ctx = new ResponseContext();
        presenter = new WebApiAuthPresenter();
        presenter.responseContext = ctx;
        loginExecuteCalled = false;
        lastLoginRequest = null;
        registerExecuteCalled = false;
        lastRegisterRequest = null;

        resource = new AuthResource();
        resource.responseContext = ctx;
        resource.loginUseCase = new LoginUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                loginExecuteCalled = true;
                lastLoginRequest = request;
                return loginOutputToReturn;
            }
        };
        resource.registerUseCase = new RegisterUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                registerExecuteCalled = true;
                lastRegisterRequest = request;
                return registerOutputToReturn;
            }
        };
    }

    @Test
    void loginShouldDelegateToUseCase()
    {
        loginOutputToReturn = new LoginUseCase.Output("test.jwt.token");
        presenter.success("test.jwt.token",
                new User("u1", "alice", "pw", UserRole.STUDENT, "Alice", "a@b.com"));

        Response response = resource.login(new LoginRequest("alice", "secret"));

        assertTrue(loginExecuteCalled);
        assertEquals("alice", lastLoginRequest.username());
        assertEquals("secret", lastLoginRequest.password());
        assertEquals(200, response.getStatus());
    }

    @Test
    void loginShouldReturn401OnBadCredentials()
    {
        loginOutputToReturn = null;
        presenter.invalidCredentials();

        Response response = resource.login(new LoginRequest("alice", "wrong"));

        assertEquals(401, response.getStatus());
    }

    @Test
    void registerShouldDelegateToUseCase()
    {
        registerOutputToReturn = new RegisterUseCase.Output("new-id");
        presenter.success(new User("new-id", "bob", "hashed", UserRole.STUDENT, "Bob", "bob@b.com"));

        Response response = resource.register(new RegisterRequest("bob", "pass", "Bob", "bob@b.com"));

        assertTrue(registerExecuteCalled);
        assertEquals("bob", lastRegisterRequest.username());
        assertEquals("pass", lastRegisterRequest.password());
        assertEquals("Bob", lastRegisterRequest.name());
        assertEquals("bob@b.com", lastRegisterRequest.email());
        assertEquals(201, response.getStatus());
    }

    @Test
    void registerShouldReturn409OnDuplicateUsername()
    {
        registerOutputToReturn = null;
        presenter.usernameAlreadyExists("bob");

        Response response = resource.register(new RegisterRequest("bob", "pass", "Bob", "bob@b.com"));

        assertEquals(409, response.getStatus());
    }
}
