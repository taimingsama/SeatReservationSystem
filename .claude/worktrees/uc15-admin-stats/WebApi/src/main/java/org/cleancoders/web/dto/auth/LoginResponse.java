package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.web.dto.common.UserResponse;

@Schema(description = "登录成功响应")
public record LoginResponse(
        @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "用户信息")
        UserResponse user
)
{
}
