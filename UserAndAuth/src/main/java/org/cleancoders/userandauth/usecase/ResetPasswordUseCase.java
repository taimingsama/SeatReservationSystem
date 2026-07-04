package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.PasswordEncoder;

import java.security.SecureRandom;

/**
 * 管理员为用户重置密码：生成随机密码并返回明文。
 */
public class ResetPasswordUseCase extends AdminAuthUseCase<ResetPasswordUseCase.Request, ResetPasswordUseCase.Output>
{

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    Presenter presenter;

    @Inject
    PasswordEncoder passwordEncoder;

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

        // 生成随机密码
        String newPassword = generateRandomPassword();
        String hashed = passwordEncoder.encode(newPassword);

        User updated = new User(user.id(), user.username(), hashed, user.role(),
                user.name(), user.email(), user.reservationCount(), user.studySeconds(),
                user.checkInCount(), user.creditScore(), user.banned());
        userRepo.save(updated);

        presenter.passwordReset(user.username(), newPassword);
        return new Output(user.username(), newPassword);
    }

    private static String generateRandomPassword()
    {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++)
        {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public interface Presenter
    {
        void passwordReset(String username, String newPassword);

        void userNotFound(String userId);
    }

    public record Request(String token, String userId)
            implements AuthUseCase.Request
    {
    }

    public record Output(String username, String newPassword)
    {
    }
}
