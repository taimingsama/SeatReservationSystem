package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户名冲突响应")
public record UsernameConflictResponse(
        @Schema(description = "错误信息", example = "Username already exists")
        String error,
        @Schema(description = "冲突的用户名", example = "john_doe")
        String username
)
{
}
