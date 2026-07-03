package org.cleancoders.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.common.domain.UserRole;

@Schema(description = "用户信息")
public record UserResponse(
        @Schema(description = "用户ID", example = "u1")
        String id,
        @Schema(description = "用户名", example = "john_doe")
        String username,
        @Schema(description = "角色", example = "STUDENT", allowableValues = {"STUDENT", "ADMIN"})
        UserRole role,
        @Schema(description = "显示名称", example = "John Doe")
        String name,
        @Schema(description = "邮箱地址", example = "john@example.com")
        String email
)
{
}
