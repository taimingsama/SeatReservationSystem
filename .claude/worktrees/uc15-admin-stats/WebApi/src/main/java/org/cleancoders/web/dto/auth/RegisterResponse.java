package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.web.dto.common.UserResponse;

@Schema(description = "注册成功响应")
public record RegisterResponse(
        @Schema(description = "用户信息")
        UserResponse user
)
{
}
