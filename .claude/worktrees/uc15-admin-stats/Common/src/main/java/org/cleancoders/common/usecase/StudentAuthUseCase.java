package org.cleancoders.common.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;

/**
 * Abstract base class for student-only use cases.
 * <p>
 * Overrides {@link #authorize} to check that the authenticated user
 * has the {@link UserRole#STUDENT} role. If not, calls
 * {@link Presenter#forbidden()} and returns {@code false}.
 *
 * @param <R> the request type, must implement {@link Request}
 * @param <O> the output type
 */
public abstract class StudentAuthUseCase<R extends AuthUseCase.Request, O>
        extends AuthUseCase<R, O>
{

    @Inject
    public Presenter presenter;

    @Override
    protected boolean authorize(User user, R request)
    {
        if (user.role() != UserRole.STUDENT)
        {
            presenter.forbidden();
            return false;
        }
        return true;
    }

    /**
     * Extended presenter interface that adds a {@code forbidden} branch
     * for role-based access denial.
     */
    public interface Presenter
    {
        void forbidden();
    }
}
