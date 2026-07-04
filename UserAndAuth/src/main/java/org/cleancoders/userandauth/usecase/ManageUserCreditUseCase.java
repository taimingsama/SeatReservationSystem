package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.UserRepository;

/**
 * 管理员管理用户信用分。
 */
public class ManageUserCreditUseCase extends AdminAuthUseCase<ManageUserCreditUseCase.Request, ManageUserCreditUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    UserRepository userRepo;

    @Override
    protected Output doExecute(User admin, Request req)
    {
        var userOpt = userRepo.findById(req.userId());
        if (userOpt.isEmpty())
        {
            presenter.userNotFound(req.userId());
            return null;
        }

        User user = userOpt.get();
        int newScore = req.creditScore();

        User updated = new User(user.id(), user.username(), user.password(), user.role(),
                user.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), newScore, user.banned());

        userRepo.save(updated);
        presenter.creditUpdated(updated);
        return new Output(updated);
    }

    public interface Presenter
    {
        void creditUpdated(User user);

        void userNotFound(String userId);
    }

    public record Request(String token, String userId, int creditScore)
            implements AuthUseCase.Request
    {
    }

    public record Output(User user)
    {
    }
}
