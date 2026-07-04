package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.PasswordEncoder;

/**
 * 学生修改密码：需提供旧密码验证，成功后更新为新密码。
 */
public class ChangePasswordUseCase extends AuthUseCase<ChangePasswordUseCase.Request, ChangePasswordUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    PasswordEncoder passwordEncoder;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // 验证旧密码
        if (!passwordEncoder.matches(req.oldPassword(), user.password()))
        {
            presenter.wrongPassword();
            return null;
        }

        // 新旧密码不能相同
        if (req.oldPassword().equals(req.newPassword()))
        {
            presenter.sameAsOldPassword();
            return null;
        }

        // 更新密码
        String hashed = passwordEncoder.encode(req.newPassword());
        User updated = new User(user.id(), user.username(), hashed, user.role(),
                user.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), user.creditScore(), user.banned());
        userRepo.save(updated);

        presenter.passwordChanged();
        return new Output();
    }

    public interface Presenter
    {
        void passwordChanged();

        void wrongPassword();

        void sameAsOldPassword();
    }

    public record Request(String token, String oldPassword, String newPassword)
            implements AuthUseCase.Request
    {
    }

    public record Output()
    {
    }
}
