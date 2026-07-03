package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.web.dto.auth.LoginResponse;
import org.cleancoders.web.dto.auth.RegisterResponse;
import org.cleancoders.web.dto.auth.UsernameConflictResponse;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiAuthPresenterTest
{

    private WebApiAuthPresenter presenter;
    private ResponseContext responseContext;

    @BeforeEach
    void setUp()
    {
        responseContext = new ResponseContext();
        presenter = new WebApiAuthPresenter();
        presenter.responseContext = responseContext;
    }

    @Test
    void successShouldReturn200WithTokenAndUserJson()
    {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        presenter.success("jwt.token.here", user);

        Response response = responseContext.get();
        assertEquals(200, response.getStatus());

        LoginResponse entity = (LoginResponse) response.getEntity();
        assertEquals("jwt.token.here", entity.token());

        assertEquals("u1", entity.user().id());
        assertEquals("alice", entity.user().username());
        assertEquals(UserRole.STUDENT, entity.user().role());
        assertEquals("Alice", entity.user().name());
        assertEquals("a@b.com", entity.user().email());
    }

    @Test
    void invalidCredentialsShouldReturn401()
    {
        presenter.invalidCredentials();

        Response response = responseContext.get();
        assertEquals(401, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Invalid credentials", entity.error());
    }

    @Test
    void userNotFoundShouldReturn404()
    {
        presenter.userNotFound();

        Response response = responseContext.get();
        assertEquals(404, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("User not found", entity.error());
    }

    // --- RegisterUseCase.Presenter ---

    @Test
    void registerSuccessShouldReturn201WithUserJson()
    {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        presenter.success(user);

        Response response = responseContext.get();
        assertEquals(201, response.getStatus());

        RegisterResponse entity = (RegisterResponse) response.getEntity();
        assertEquals("u1", entity.user().id());
        assertEquals("alice", entity.user().username());
        assertEquals(UserRole.STUDENT, entity.user().role());
    }

    @Test
    void usernameAlreadyExistsShouldReturn409()
    {
        presenter.usernameAlreadyExists("alice");

        Response response = responseContext.get();
        assertEquals(409, response.getStatus());

        UsernameConflictResponse entity = (UsernameConflictResponse) response.getEntity();
        assertEquals("Username already exists", entity.error());
        assertEquals("alice", entity.username());
    }
}
