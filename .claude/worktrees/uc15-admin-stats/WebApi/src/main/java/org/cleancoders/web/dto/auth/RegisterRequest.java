package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "用户名", required = true, example = "john_doe")
        String username,
        @Schema(description = "密码", required = true, example = "secret123")
        String password,
        @Schema(description = "显示名称", required = true, example = "John Doe")
        String name,
        @Schema(description = "邮箱地址", required = true, example = "john@example.com")
        String email
)
{
}