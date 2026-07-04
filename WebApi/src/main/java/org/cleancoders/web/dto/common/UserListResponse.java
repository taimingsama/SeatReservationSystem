package org.cleancoders.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "用户列表响应")
public record UserListResponse(
        @Schema(description = "用户列表")
        List<UserResponse> users
)
{
}
