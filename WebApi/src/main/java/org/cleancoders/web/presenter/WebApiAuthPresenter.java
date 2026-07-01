package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.web.dto.ErrorResponse;
import org.cleancoders.web.dto.LoginResponse;
import org.cleancoders.web.dto.MeResponse;
import org.cleancoders.web.dto.RegisterResponse;
import org.cleancoders.web.dto.UserResponse;
import org.cleancoders.web.dto.UsernameConflictResponse;

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

        current.set(Response.ok(new LoginResponse(token, toUserResponse(user)))
                .cookie(authCookie).build());
    }

    @Override
    public void invalidCredentials()
    {
        current.set(Response.status(401).entity(new ErrorResponse("Invalid credentials")).build());
    }

    @Override
    public void userNotFound()
    {
        current.set(Response.status(404).entity(new ErrorResponse("User not found")).build());
    }

    // --- RegisterUseCase.Presenter ---

    @Override
    public void success(User user)
    {
        current.set(Response.status(201).entity(new RegisterResponse(toUserResponse(user))).build());
    }

    @Override
    public void usernameAlreadyExists(String username)
    {
        current.set(Response.status(409)
                .entity(new UsernameConflictResponse("Username already exists", username))
                .build());
    }

    // --- GetMeUseCase.Presenter ---

    @Override
    public void presentUser(User user)
    {
        current.set(Response.ok(new MeResponse(toUserResponse(user))).build());
    }

    @Override
    public void invalidToken()
    {
        current.set(Response.status(401).entity(new ErrorResponse("Invalid or expired token")).build());
    }

    // ---

    private UserResponse toUserResponse(User user)
    {
        return new UserResponse(user.id(), user.username(), user.role(), user.name(), user.email());
    }

    public Response getResponse()
    {
        return current.get();
    }
}
