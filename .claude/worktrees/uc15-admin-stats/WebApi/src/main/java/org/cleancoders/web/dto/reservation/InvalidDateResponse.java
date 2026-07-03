package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "日期格式不合法响应")
public record InvalidDateResponse(
        @Schema(description = "错误信息", example = "日期格式不合法，请使用 YYYY-MM-DD 格式")
        String error,
        @Schema(description = "传入的原始日期值", example = "2026/07/02")
        String provided
) {
}
