package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;

/**
 * 管理员删除学生。
 */
public class DeleteStudentUseCase extends AdminAuthUseCase<DeleteStudentUseCase.Request, DeleteStudentUseCase.Output>
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

        userRepo.deleteById(req.userId());
        presenter.deleted(req.userId());
        return new Output(req.userId());
    }

    public interface Presenter
    {
        void deleted(String userId);

        void userNotFound(String userId);
    }

    public record Request(String token, String userId) implements AuthUseCase.Request {}
    public record Output(String userId) {}
}
