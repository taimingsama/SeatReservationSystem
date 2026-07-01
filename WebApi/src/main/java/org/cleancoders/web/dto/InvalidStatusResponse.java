package org.cleancoders.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "状态不允许操作响应")
public record InvalidStatusResponse(
        @Schema(description = "错误信息", example = "当前状态不允许签到")
        String error,
        @Schema(description = "当前预约状态", example = "CHECKED_IN")
        String currentStatus
) {
}
