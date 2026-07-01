package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.web.dto.LoginRequest;
import org.cleancoders.web.dto.RegisterRequest;
import org.cleancoders.web.presenter.WebApiAuthPresenter;

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
    WebApiAuthPresenter presenter;

    @POST
    @Path("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功返回 JWT token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功，返回 JWT token"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "404", description = "用户名不存在"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法")
    })
    public Response login(LoginRequest request)
    {
        loginUseCase.execute(new LoginUseCase.Request(request.username(), request.password()));
        return presenter.getResponse();
    }

    @GET
    @Path("/me")
    @Operation(summary = "获取当前用户信息", description = "通过 JWT token 获取当前登录用户的基本信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回用户信息"),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public Response me(@CookieParam("Authorization") String authCookie)
    {
        getMeUseCase.execute(new GetMeUseCase.Request(authCookie));
        return presenter.getResponse();
    }

    @POST
    @Path("/register")
    @Operation(summary = "用户注册", description = "注册新用户账户。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "409", description = "用户名已存在"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法")
    })
    public Response register(RegisterRequest request)
    {
        registerUseCase.execute(new RegisterUseCase.Request(
                request.username(), request.password(), request.name(), request.email()));
        return presenter.getResponse();
    }

}
