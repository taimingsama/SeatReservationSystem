package org.cleancoders.userandauth.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;

import java.util.Comparator;
import java.util.List;

/**
 * 管理员查看所有学生信息。
 */
public class ListAllStudentsUseCase extends AdminAuthUseCase<ListAllStudentsUseCase.Request, ListAllStudentsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Override
    protected Output doExecute(User admin, Request req)
    {
        List<User> students = userRepo.findAll().stream()
                .filter(u -> u.role() == UserRole.STUDENT)
                .sorted(Comparator.comparing(User::username))
                .toList();

        presenter.presentStudents(students);
        return new Output(students);
    }

    public interface Presenter
    {
        void presentStudents(List<User> students);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(List<User> students) {}
}
