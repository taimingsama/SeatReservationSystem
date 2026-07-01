package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;

import java.util.Map;

@Singleton
public class WebApiAuthPresenter implements LoginUseCase.Presenter, RegisterUseCase.Presenter, GetMeUseCase.Presenter
{

    private final ThreadLocal<Response> current = new ThreadLocal<>();

    // --- LoginUseCase.Presenter ---

    @Override
    public void success(String token, User user)
    {
        NewCookie authCookie = new NewCookie.Builder("Authorization")
                .value(token)
                .path("/api")
                .httpOnly(true)
                .build();

        current.set(Response.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "role", user.role().name(),
                        "name", user.name(),
                        "email", user.email()
                )
        )).cookie(authCookie).build());
    }

    @Override
    public void invalidCredentials()
    {
        current.set(Response.status(401).entity(Map.of(
                "error", "Invalid credentials"
        )).build());
    }

    @Override
    public void userNotFound()
    {
        current.set(Response.status(404).entity(Map.of(
                "error", "User not found"
        )).build());
    }

    // --- RegisterUseCase.Presenter ---

    @Override
    public void success(User user)
    {
        current.set(Response.status(201).entity(Map.of(
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "role", user.role().name(),
                        "name", user.name(),
                        "email", user.email()
                )
        )).build());
    }

    @Override
    public void usernameAlreadyExists(String username)
    {
        current.set(Response.status(409).entity(Map.of(
                "error", "Username already exists",
                "username", username
        )).build());
    }

    // --- GetMeUseCase.Presenter ---

    @Override
    public void presentUser(User user)
    {
        current.set(Response.ok(Map.of(
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "role", user.role().name(),
                        "name", user.name(),
                        "email", user.email()
                )
        )).build());
    }

    @Override
    public void invalidToken()
    {
        current.set(Response.status(401).entity(Map.of(
                "error", "Invalid or expired token"
        )).build());
    }

    // ---

    public Response getResponse()
    {
        return current.get();
    }
}
