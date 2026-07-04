package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.infrastructure.persistence.testdata.TestDataUserRepo;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.infrastructure.security.RawPasswordEncoder;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.userandauth.usecase.*;
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
        bind(ManageUserCreditUseCase.class).to(ManageUserCreditUseCase.class);
        bind(ChangePasswordUseCase.class).to(ChangePasswordUseCase.class);
        bind(ResetPasswordUseCase.class).to(ResetPasswordUseCase.class);
        bind(UpdateUserNameUseCase.class).to(UpdateUserNameUseCase.class);
        bind(ListAllStudentsUseCase.class).to(ListAllStudentsUseCase.class);
        bind(BanStudentUseCase.class).to(BanStudentUseCase.class);
        bind(DeleteStudentUseCase.class).to(DeleteStudentUseCase.class);
        // === Presenters ===
        bind(WebApiAuthPresenter.class)
                .to(LoginUseCase.Presenter.class)
                .to(RegisterUseCase.Presenter.class)
                .to(GetMeUseCase.Presenter.class)
                .to(ManageUserCreditUseCase.Presenter.class)
                .to(ChangePasswordUseCase.Presenter.class)
                .to(ResetPasswordUseCase.Presenter.class)
                .to(UpdateUserNameUseCase.Presenter.class)
                .to(ListAllStudentsUseCase.Presenter.class)
                .to(BanStudentUseCase.Presenter.class)
                .to(DeleteStudentUseCase.Presenter.class)
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
