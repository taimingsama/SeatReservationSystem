package org.cleancoders.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "错误响应")
public record ErrorResponse(
        @Schema(description = "错误信息", example = "Invalid credentials")
        String error
)
{
}
