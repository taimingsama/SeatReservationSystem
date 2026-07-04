package org.cleancoders.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改显示名称请求")
public record UpdateNameRequest(
        @Schema(description = "新的显示名称", required = true, example = "张三丰")
        String name
)
{
}
