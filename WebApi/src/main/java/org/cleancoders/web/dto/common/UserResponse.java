package org.cleancoders.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.userandauth.domain.UserRole;

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
        String email,
        @Schema(description = "累计预约次数", example = "15")
        int reservationCount,
        @Schema(description = "累计学习时长（秒）", example = "151200")
        int studySeconds,
        @Schema(description = "累计签到次数", example = "12")
        int checkInCount,
        @Schema(description = "信用分（满分100）", example = "95")
        int creditScore,
        @Schema(description = "是否被封禁", example = "false")
        boolean banned
)
{
}
