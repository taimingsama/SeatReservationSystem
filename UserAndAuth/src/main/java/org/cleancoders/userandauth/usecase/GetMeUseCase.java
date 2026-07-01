package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;
import org.cleancoders.userandauth.outbound.UserRepository;

/**
 * UC-03: 获取当前登录用户信息。
 * <p>
 * 通过 JWT token 识别当前用户，返回用户基本信息。
 * 属于公开用例（不继承 AuthUseCase），自行处理 token 验证。
 */
public class GetMeUseCase
{

    @Inject
    UserRepository userRepo;
    @Inject
    TokenService tokenService;
    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        // 1. 验证并解析 JWT token
        TokenPayload payload;
        try
        {
            payload = tokenService.validate(request.token());
        } catch (TokenValidationException e)
        {
            presenter.invalidToken();
            return null;
        }

        // 2. 查找用户
        var user = userRepo.findById(payload.userId());
        if (user.isEmpty())
        {
            presenter.userNotFound();
            return null;
        }

        // 3. 返回用户信息
        User u = user.get();
        presenter.presentUser(u);
        return new Output(u);
    }

    public interface Presenter
    {
        void presentUser(User user);

        void invalidToken();

        void userNotFound();
    }

    public record Request(String token)
    {
    }

    public record Output(User user)
    {
    }
}