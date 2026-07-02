package org.cleancoders.userandauth.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;

/**
 * Abstract base class for student-only use cases.
 * <p>
 * Overrides {@link #authorize} to check that the authenticated user
 * has the {@link UserRole#STUDENT} role. If not, calls
 * {@link StudentPresenter#forbidden()} and returns {@code false}.
 *
 * @param <R> the request type, must implement {@link AuthRequest}
 * @param <O> the output type
 */
public abstract class StudentAuthUseCase<R extends AuthUseCase.AuthRequest, O>
        extends AuthUseCase<R, O> {

    /**
     * Extended presenter interface that adds a {@code forbidden} branch
     * for role-based access denial.
     */
    public interface StudentPresenter extends AuthUseCase.Presenter {
        void forbidden();
    }

    @Override
    protected abstract StudentPresenter getPresenter();

    @Override
    protected boolean authorize(User user, R request) {
        if (user.role() != UserRole.STUDENT) {
            getPresenter().forbidden();
            return false;
        }
        return true;
    }
}
