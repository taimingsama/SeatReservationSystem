package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.common.outbound.UserRepository;

public class LoginUseCase
{

    @Inject
    UserRepository userRepo;
    @Inject
    PasswordEncoder passwordEncoder;
    @Inject
    TokenService tokenService;
    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        var user = userRepo.findByUsername(request.username());
        if (user.isEmpty())
        {
            presenter.userNotFound();
            return null;
        }

        var u = user.get();
        if (!passwordEncoder.matches(request.password(), u.password()))
        {
            presenter.invalidCredentials();
            return null;
        }

        String token = tokenService.generate(u.id());
        presenter.success(token, u);
        return new Output(token);
    }

    public interface Presenter
    {
        void success(String token, User user);

        void invalidCredentials();

        void userNotFound();
    }

    public record Request(String username, String password)
    {
    }

    public record Output(String token)
    {
    }
}
