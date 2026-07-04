package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;

/**
 * 管理员封禁/解封学生。
 */
public class BanStudentUseCase extends AdminAuthUseCase<BanStudentUseCase.Request, BanStudentUseCase.Output>
{

    @Inject
    Presenter presenter;

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
        User updated = new User(user.id(), user.username(), user.password(), user.role(),
                user.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), user.creditScore(), req.ban());

        userRepo.save(updated);
        presenter.banUpdated(updated);
        return new Output(updated);
    }

    public interface Presenter
    {
        void banUpdated(User user);

        void userNotFound(String userId);
    }

    public record Request(String token, String userId, boolean ban) implements AuthUseCase.Request {}
    public record Output(User user) {}
}
