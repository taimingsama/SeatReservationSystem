package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.usecase.ChangePasswordUseCase;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.userandauth.usecase.UpdateUserNameUseCase;
import org.cleancoders.web.dto.auth.ChangePasswordRequest;
import org.cleancoders.web.dto.auth.LoginRequest;
import org.cleancoders.web.dto.auth.LoginResponse;
import org.cleancoders.web.dto.auth.MeResponse;
import org.cleancoders.web.dto.auth.RegisterRequest;
import org.cleancoders.web.dto.auth.RegisterResponse;
import org.cleancoders.web.dto.auth.UpdateNameRequest;
import org.cleancoders.web.dto.auth.UsernameConflictResponse;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.common.UserResponse;
import org.cleancoders.web.presenter.ResponseContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "用户认证相关接口（登录 / 注册）")
public class AuthResource
{

    @Inject
    LoginUseCase loginUseCase;
    @Inject
    RegisterUseCase registerUseCase;
    @Inject
    GetMeUseCase getMeUseCase;
    @Inject
    ChangePasswordUseCase changePasswordUseCase;
    @Inject
    UpdateUserNameUseCase updateUserNameUseCase;
    @Inject
    ResponseContext responseContext;

    @PUT
    @Path("/name")
    @Operation(summary = "修改显示名称", description = "用户修改自己的显示名称。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateName(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            UpdateNameRequest request)
    {
        updateUserNameUseCase.execute(new UpdateUserNameUseCase.Request(
                authCookie, request.name()));
        return responseContext.get();
    }

    @PUT
    @Path("/password")
    @Operation(summary = "修改密码", description = "学生输入旧密码和新密码修改自己的密码。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "密码修改成功"),
            @ApiResponse(responseCode = "400", description = "旧密码错误或新旧密码相同",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response changePassword(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            ChangePasswordRequest request)
    {
        changePasswordUseCase.execute(new ChangePasswordUseCase.Request(
                authCookie, request.oldPassword(), request.newPassword()));
        return responseContext.get();
    }

    @POST
    @Path("/login")
    @Operation(summary = "用户登录 (UC-01)", description = "使用用户名和密码登录，成功返回 JWT token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功，返回 JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "用户名不存在",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数不合法",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response login(LoginRequest request)
    {
        loginUseCase.execute(new LoginUseCase.Request(request.username(), request.password()));
        return responseContext.get();
    }

    @GET
    @Path("/me")
    @Operation(summary = "获取当前用户信息 (UC-03)", description = "通过 JWT token 获取当前登录用户的基本信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回用户信息",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response me(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getMeUseCase.execute(new GetMeUseCase.Request(authCookie));
        return responseContext.get();
    }

    @POST
    @Path("/register")
    @Operation(summary = "用户注册 (UC-02)", description = "注册新用户账户。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "409", description = "用户名已存在",
                    content = @Content(schema = @Schema(implementation = UsernameConflictResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数不合法",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response register(RegisterRequest request)
    {
        registerUseCase.execute(new RegisterUseCase.Request(
                request.username(), request.password(), request.name(), request.email()));
        return responseContext.get();
    }

}