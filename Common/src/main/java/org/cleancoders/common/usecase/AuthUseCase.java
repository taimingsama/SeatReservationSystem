package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;
import org.cleancoders.userandauth.outbound.UserRepository;

/**
 * Abstract base class for authenticated use cases.
 * <p>
 * Implements the Template Method pattern:
 * <ol>
 *   <li>{@code authenticate(token)} — validates JWT and looks up the user</li>
 *   <li>{@code authorize(user, request)} — hook for role-based checks (subclasses override)</li>
 *   <li>{@code doExecute(user, request)} — business logic (subclasses implement)</li>
 * </ol>
 * <p>
 * If authentication or authorization fails, the corresponding presenter method
 * is called and {@code execute()} returns {@code null}.
 *
 * @param <R> the request type, must implement {@link AuthRequest}
 * @param <O> the output type returned by {@link #execute(AuthRequest)}
 */
public abstract class AuthUseCase<R extends AuthUseCase.AuthRequest, O>
{

    @Inject
    public TokenService tokenService;

    @Inject
    public UserRepository userRepo;

    /**
     * Contract for request types that carry a JWT token.
     */
    public interface AuthRequest
    {
        String token();
    }

    /**
     * Presenter interface for auth-related error branches.
     * Leaf use cases extend this with their own success/error branches.
     */
    public interface Presenter
    {
        void invalidToken();

        void userNotFound();
    }

    /**
     * Returns the presenter instance for this use case.
     * Implemented by leaf classes (which hold the injected presenter).
     */
    protected abstract Presenter getPresenter();

    /**
     * Template method: authenticate → authorize → doExecute.
     *
     * @param request the request containing the JWT token
     * @return the output from {@link #doExecute}, or {@code null} if auth fails
     */
    public O execute(R request)
    {
        User user = authenticate(request.token());
        if (user == null)
        {
            return null;
        }

        if (!authorize(user, request))
        {
            return null;
        }

        return doExecute(user, request);
    }

    /**
     * Validates the JWT token and looks up the corresponding user.
     * Calls {@link Presenter#invalidToken()} or {@link Presenter#userNotFound()} on failure.
     *
     * @return the authenticated user, or {@code null} if authentication fails
     */
    private User authenticate(String token)
    {
        TokenPayload payload;
        try
        {
            payload = tokenService.validate(token);
        } catch (TokenValidationException e)
        {
            getPresenter().invalidToken();
            return null;
        }

        var user = userRepo.findById(payload.userId());
        if (user.isEmpty())
        {
            getPresenter().userNotFound();
            return null;
        }

        return user.get();
    }

    /**
     * Hook for role-based authorization. Default implementation allows all users.
     * Override to restrict access (e.g., STUDENT only, ADMIN only).
     *
     * @param user    the authenticated user
     * @param request the original request
     * @return {@code true} if authorized, {@code false} otherwise
     */
    protected boolean authorize(User user, R request)
    {
        return true;
    }

    /**
     * Implements the core business logic. Called only after successful authentication and authorization.
     *
     * @param user    the authenticated and authorized user
     * @param request the original request
     * @return the output of the use case
     */
    protected abstract O doExecute(User user, R request);
}
