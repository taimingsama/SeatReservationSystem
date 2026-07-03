package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.outbound.TokenPayload;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.TokenValidationException;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common.usecase.AuthUseCase;

/**
 * UC-03: 获取当前登录用户信息。
 * <p>
 * 通过 JWT token 识别当前用户，返回用户基本信息。
 * 属于公开用例（不继承 AuthUseCase），自行处理 token 验证。
 */
public class GetMeUseCase extends AuthUseCase<GetMeUseCase.Request, GetMeUseCase.Output>
{
    @Inject
    Presenter presenter;

    @Override
    protected Output doExecute(User user, Request request)
    {
        presenter.presentUser(user);
        return new Output(user);
    }

    public interface Presenter
    {
        void presentUser(User user);
    }

    public record Request(String token) implements AuthUseCase.Request
    {
    }

    public record Output(User user)
    {
    }
}