package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.web.dto.common.UserResponse;

@Schema(description = "当前用户信息响应")
public record MeResponse(
        @Schema(description = "用户信息")
        UserResponse user
)
{
}
