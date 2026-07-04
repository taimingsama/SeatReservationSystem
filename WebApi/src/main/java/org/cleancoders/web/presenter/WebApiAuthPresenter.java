package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.*;
import org.cleancoders.web.dto.auth.LoginResponse;
import org.cleancoders.web.dto.auth.MeResponse;
import org.cleancoders.web.dto.auth.RegisterResponse;
import org.cleancoders.web.dto.auth.ResetPasswordResponse;
import org.cleancoders.web.dto.auth.UsernameConflictResponse;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.common.AdminStudentResponse;
import org.cleancoders.web.dto.common.UserListResponse;
import org.cleancoders.web.dto.common.UserResponse;

import java.util.List;
import java.util.Map;

@Singleton
public class WebApiAuthPresenter extends WebApiPresenter implements
        LoginUseCase.Presenter,
        RegisterUseCase.Presenter,
        GetMeUseCase.Presenter,
        ManageUserCreditUseCase.Presenter,
        ChangePasswordUseCase.Presenter,
        ResetPasswordUseCase.Presenter,
        UpdateUserNameUseCase.Presenter,
        ListAllStudentsUseCase.Presenter,
        BanStudentUseCase.Presenter,
        DeleteStudentUseCase.Presenter,
        StudentAuthUseCase.Presenter,
        AdminAuthUseCase.Presenter,
        AuthUseCase.Presenter
{
    // --- LoginUseCase.Presenter ---

    @Override
    public void success(String token, User user)
    {
        NewCookie authCookie = new NewCookie.Builder("Authorization")
                .value(token)
                .path("/api")
                .httpOnly(true)
                .build();

        responseContext.set(Response.ok(new LoginResponse(token, toUserResponse(user)))
                .cookie(authCookie).build());
    }

    @Override
    public void invalidCredentials()
    {
        responseContext.set(Response.status(401).entity(new ErrorResponse("Invalid credentials")).build());
    }

    // --- RegisterUseCase.Presenter ---

    @Override
    public void success(User user)
    {
        responseContext.set(Response.status(201).entity(new RegisterResponse(toUserResponse(user))).build());
    }

    @Override
    public void usernameAlreadyExists(String username)
    {
        responseContext.set(Response.status(409)
                .entity(new UsernameConflictResponse("Username already exists", username))
                .build());
    }

    // --- GetMeUseCase.Presenter ---

    @Override
    public void presentUser(User user)
    {
        responseContext.set(Response.ok(new MeResponse(toUserResponse(user))).build());
    }

    private UserResponse toUserResponse(User user)
    {
        return new UserResponse(user.id(), user.username(), user.role(), user.name(), user.email(),
                user.reservationCount(), user.studyHours(), user.checkInCount(), user.creditScore(), user.banned());
    }

    // --- ManageUserCreditUseCase.Presenter ---

    @Override
    public void creditUpdated(User user)
    {
        responseContext.set(Response.ok(new UserResponse(user.id(), user.username(), user.role(),
                user.name(), user.email(), user.reservationCount(), user.studyHours(),
                user.checkInCount(), user.creditScore(), user.banned())).build());
    }

    @Override
    public void userNotFound(String userId)
    {
        responseContext.set(Response.status(404).entity(
                new ErrorResponse("用户不存在: " + userId)).build());
    }

    // --- ChangePasswordUseCase.Presenter ---

    @Override
    public void passwordChanged()
    {
        responseContext.set(Response.ok(
                new ErrorResponse("密码修改成功")).build());
    }

    @Override
    public void wrongPassword()
    {
        responseContext.set(Response.status(400).entity(
                new ErrorResponse("旧密码错误")).build());
    }

    @Override
    public void sameAsOldPassword()
    {
        responseContext.set(Response.status(400).entity(
                new ErrorResponse("新密码不能与旧密码相同")).build());
    }

    // --- ResetPasswordUseCase.Presenter ---

    @Override
    public void passwordReset(String username, String newPassword)
    {
        responseContext.set(Response.ok(
                new ResetPasswordResponse(username, newPassword)).build());
    }

    // --- UpdateUserNameUseCase.Presenter ---

    @Override
    public void nameUpdated(User user)
    {
        responseContext.set(Response.ok(
                new UserResponse(user.id(), user.username(), user.role(),
                        user.name(), user.email(), user.reservationCount(),
                        user.studyHours(), user.checkInCount(), user.creditScore(), user.banned())).build());
    }

    // --- ListAllStudentsUseCase.Presenter ---

    @Override
    public void presentStudents(List<User> students)
    {
        List<AdminStudentResponse> dtos = students.stream()
                .map(u -> new AdminStudentResponse(u.id(), u.username(), u.name(), u.email(),
                        u.role(), u.checkInCount(), u.creditScore(), u.banned()))
                .toList();
        responseContext.set(Response.ok(new UserListResponse(dtos)).build());
    }

    // --- BanStudentUseCase.Presenter ---

    @Override
    public void banUpdated(User user)
    {
        responseContext.set(Response.ok(toUserResponse(user)).build());
    }

    // --- DeleteStudentUseCase.Presenter ---

    @Override
    public void deleted(String userId)
    {
        responseContext.set(Response.ok(Map.of("message", "学生已删除", "userId", userId)).build());
    }

    // --- AuthUseCase.Presenterr ---

    @Override
    public void invalidToken()
    {
        responseContext.set(Response.status(401).entity(new ErrorResponse("Invalid or expired token")).build());
    }

    @Override
    public void userNotFound()
    {
        responseContext.set(Response.status(404).entity(new ErrorResponse("User not found")).build());
    }

    @Override
    public void banned()
    {
        responseContext.set(Response.status(403).entity(new ErrorResponse("账户已被封禁")).build());
    }

    // --- AdminAuthUseCase.Presenter ---
    // --- AuthUseCase.Presenter ---

    @Override
    public void forbidden()
    {
        responseContext.set(Response.status(403).entity(new ErrorResponse("权限不足，拒绝访问")).build());
    }
}