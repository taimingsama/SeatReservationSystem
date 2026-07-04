package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;

/**
 * 用户修改自己的显示名称。
 */
public class UpdateUserNameUseCase extends AuthUseCase<UpdateUserNameUseCase.Request, UpdateUserNameUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Override
    protected Output doExecute(User user, Request req)
    {
        User updated = new User(user.id(), user.username(), user.password(), user.role(),
                req.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), user.creditScore(), user.banned());
        userRepo.save(updated);

        presenter.nameUpdated(updated);
        return new Output(updated);
    }

    public interface Presenter
    {
        void nameUpdated(User user);
    }

    public record Request(String token, String name)
            implements AuthUseCase.Request
    {
    }

    public record Output(User user)
    {
    }
}
