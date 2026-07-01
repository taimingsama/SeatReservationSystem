package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
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

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("jwt.token.here", entity.get("token"));

        @SuppressWarnings("unchecked")
        var userMap = (java.util.Map<String, Object>) entity.get("user");
        assertEquals("u1", userMap.get("id"));
        assertEquals("alice", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
        assertEquals("Alice", userMap.get("name"));
        assertEquals("a@b.com", userMap.get("email"));
    }

    @Test
    void invalidCredentialsShouldReturn401()
    {
        presenter.invalidCredentials();

        Response response = presenter.getResponse();
        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("Invalid credentials", entity.get("error"));
    }

    @Test
    void userNotFoundShouldReturn404()
    {
        presenter.userNotFound();

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("User not found", entity.get("error"));
    }

    // --- RegisterUseCase.Presenter ---

    @Test
    void registerSuccessShouldReturn201WithUserJson()
    {
        User user = new User("u1", "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com");
        presenter.success(user);

        Response response = presenter.getResponse();
        assertEquals(201, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();

        @SuppressWarnings("unchecked")
        var userMap = (java.util.Map<String, Object>) entity.get("user");
        assertEquals("u1", userMap.get("id"));
        assertEquals("alice", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
    }

    @Test
    void usernameAlreadyExistsShouldReturn409()
    {
        presenter.usernameAlreadyExists("alice");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("Username already exists", entity.get("error"));
        assertEquals("alice", entity.get("username"));
    }
}
