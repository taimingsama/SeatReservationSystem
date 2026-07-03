package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.infrastructure.persistence.testdata.TestDataUserRepo;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.infrastructure.security.RawPasswordEncoder;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class UserAndAuthBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        // === UseCases ===
        bind(LoginUseCase.class).to(LoginUseCase.class);
        bind(RegisterUseCase.class).to(RegisterUseCase.class);
        bind(GetMeUseCase.class).to(GetMeUseCase.class);
        // === Presenters ===
        bind(WebApiAuthPresenter.class)
                .to(LoginUseCase.Presenter.class)
                .to(RegisterUseCase.Presenter.class)
                .to(GetMeUseCase.Presenter.class)
                .to(StudentAuthUseCase.Presenter.class)
                .to(AdminAuthUseCase.Presenter.class)
                .to(AuthUseCase.Presenter.class)
                .in(Singleton.class);
        // === Infrastructure ===
        bind(TestDataUserRepo.class).to(UserRepository.class).in(Singleton.class);
        bind(RawPasswordEncoder.class).to(PasswordEncoder.class).in(Singleton.class);
        bind(JjwtTokenService.class).to(TokenService.class).in(Singleton.class);
    }
}
