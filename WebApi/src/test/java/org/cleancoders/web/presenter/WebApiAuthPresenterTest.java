package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.web.dto.ErrorResponse;
import org.cleancoders.web.dto.LoginResponse;
import org.cleancoders.web.dto.RegisterResponse;
import org.cleancoders.web.dto.UsernameConflictResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiAuthPresenterTest
{

    private WebApiAuthPresenter presenter;

    @BeforeEach
    void setUp()
    {
        presenter = new WebApiAuthPresenter();
    }

    @Test
    void successShouldReturn200WithTokenAndUserJson()
    {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        presenter.success("jwt.token.here", user);

        Response response = presenter.getResponse();
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

        Response response = presenter.getResponse();
        assertEquals(401, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Invalid credentials", entity.error());
    }

    @Test
    void userNotFoundShouldReturn404()
    {
        presenter.userNotFound();

        Response response = presenter.getResponse();
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

        Response response = presenter.getResponse();
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

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        UsernameConflictResponse entity = (UsernameConflictResponse) response.getEntity();
        assertEquals("Username already exists", entity.error());
        assertEquals("alice", entity.username());
    }
}
