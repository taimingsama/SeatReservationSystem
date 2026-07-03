package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "无效布局类型响应")
public record InvalidLayoutResponse(
        @Schema(description = "错误信息", example = "无效的布局类型")
        String error,
        @Schema(description = "输入的布局值", example = "XLARGE")
        String layout,
        @Schema(description = "有效值列表", example = "[\"SMALL\", \"MEDIUM\", \"LARGE\"]")
        String[] validValues
) {
}
