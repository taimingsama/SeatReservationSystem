package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "用户名", required = true, example = "john_doe")
        String username,
        @Schema(description = "密码", required = true, example = "secret123")
        String password
)
{
}