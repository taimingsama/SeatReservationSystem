package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.common.outbound.UserRepository;

public class RegisterUseCase
{

    @Inject
    UserRepository userRepo;
    @Inject
    PasswordEncoder passwordEncoder;
    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        if (userRepo.findByUsername(request.username()).isPresent())
        {
            presenter.usernameAlreadyExists(request.username());
            return null;
        }

        String hashedPw = passwordEncoder.encode(request.password());
        User user = new User(null, request.username(), hashedPw, UserRole.STUDENT, request.name(), request.email());
        User saved = userRepo.save(user);

        presenter.success(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(User user);

        void usernameAlreadyExists(String username);
    }

    public record Request(String username, String password, String name, String email)
    {
    }

    public record Output(String userId)
    {
    }
}
