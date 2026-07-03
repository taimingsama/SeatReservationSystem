package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @Schema(description = "旧密码", required = true, example = "oldSecret123")
        String oldPassword,
        @Schema(description = "新密码", required = true, example = "newSecret456")
        String newPassword
)
{
}
