package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "重置密码响应（包含新生成的随机密码）")
public record ResetPasswordResponse(
        @Schema(description = "用户名", example = "zhangsan")
        String username,
        @Schema(description = "新密码（明文，请告知用户妥善保管）", example = "aB3xK9mP2q")
        String newPassword
)
{
}
