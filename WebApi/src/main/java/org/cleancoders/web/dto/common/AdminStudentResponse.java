package org.cleancoders.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.userandauth.domain.UserRole;

@Schema(description = "管理员学生列表项")
public record AdminStudentResponse(
        @Schema(description = "用户ID", example = "u1")
        String id,
        @Schema(description = "用户名", example = "zhangsan")
        String username,
        @Schema(description = "显示名称", example = "张三")
        String name,
        @Schema(description = "邮箱地址", example = "zhangsan@example.com")
        String email,
        @Schema(description = "角色", example = "STUDENT")
        UserRole role,
        @Schema(description = "累计签到次数", example = "12")
        int checkInCount,
        @Schema(description = "信用分", example = "95")
        int creditScore,
        @Schema(description = "是否被封禁", example = "false")
        boolean banned
)
{
}
